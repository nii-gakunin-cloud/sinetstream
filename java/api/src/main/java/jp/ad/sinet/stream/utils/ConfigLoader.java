/*
 * Copyright (C) 2019 National Institute of Informatics
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package jp.ad.sinet.stream.utils;

import jp.ad.sinet.stream.api.InvalidConfigurationException;
import jp.ad.sinet.stream.api.NoConfigException;
import jp.ad.sinet.stream.api.SinetStreamException;
import lombok.extern.java.Log;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

//import javax.xml.bind.DatatypeConverter;

@Log
class ConfigLoader {

    private final Path config;

    ConfigLoader(Path config) {
        this.config = config;
    }

    private Map<String, Map<String, Object>> loadConfigFromEnvUrl() {
        return Optional.ofNullable(System.getenv("SINETSTREAM_CONFIG_URL"))
                .map(this::loadConfigFileFromURL).orElse(null);
    }

    private Map<String, Map<String, Object>> loadConfigFromHome() {
        return loadConfigFileFromPath(Paths.get(System.getProperty("user.home"), ".config", "sinetstream", "config.yml"));
    }

    private Map<String, Map<String, Object>> loadConfigFromCwd() {
        return loadConfigFileFromPath(Paths.get(".sinetstream_config.yml"));
    }

    private Map<String, Map<String, Object>> loadConfigFromPath() {
        return Objects.nonNull(config) ? loadConfigFileFromPath(config) : null;
    }

    Map<String, Map<String, Object>> loadConfigFile() {
        List<Supplier<Map<String, Map<String, Object>>>> configLoaders = Arrays.asList(
                this::loadConfigFromPath,
                this::loadConfigFromEnvUrl,
                this::loadConfigFromHome,
                this::loadConfigFromCwd
        );
        return configLoaders.stream().map(Supplier::get).filter(Objects::nonNull).findFirst()
                .orElseThrow(NoConfigException::new);
    }

    private Map<String, Map<String, Object>> loadConfigFileFromURL(String url) {
        URL configUrl;
        try {
            configUrl = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
        try (InputStream is = configUrl.openStream()) {
            Yaml yaml = makeYaml();
            return yaml.load(is);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Map<String, Map<String, Object>> loadConfigFileFromPath(Path path) {
        if (!path.toFile().exists()) {
            return null;
        }
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Yaml yaml = makeYaml();
            return yaml.load(reader);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    class SINETStreamConstructor extends Constructor {
        private Base64.Decoder b64decoder;
        SINETStreamConstructor() {
            this.yamlConstructors.put(new Tag("!sinetstream/encrypted"), new ConstructEncrypted());
            this.b64decoder = Base64.getDecoder();
        }
        private class ConstructEncrypted extends AbstractConstruct {
            public Object construct(Node node) {
                String s = (String) constructScalar((ScalarNode) node);
                s = s.replace("\n", "");
                byte[] value = b64decoder.decode(s);
                return new SecretValue(value, null);
            }
        }
    }

    private Yaml makeYaml() {
        return new Yaml(new SINETStreamConstructor());
    }

    @SuppressWarnings("unchecked")
    static void replaceInlineData(Map<String, Object> params, List<File> tmpLst) {
        final String sfx = "_data";
        Map<String, Object> params2 = new HashMap<String, Object>();
        for (Map.Entry<String, Object> e : params.entrySet()) {
            Object v = e.getValue();
            if (v instanceof Map) {
                replaceInlineData((Map<String, Object>) v, tmpLst);
                continue;
            }
            String k = e.getKey();
            if (k.length() > sfx.length() && k.endsWith(sfx)) {
                String k2 = k.substring(0, k.length() - sfx.length());
                byte[] data;
                if (v instanceof String) {
                    data = ((String) v).getBytes();
                } else if (v instanceof byte[]) {
                    data = (byte[]) v;
                } else {
                    throw new InvalidConfigurationException("value of " + k + " must be string or binary");
                }
                File tmp = writeTmpFile(data, tmpLst);
                params2.put(k2, tmp.getPath());
            }
        }
        params.putAll(params2);
    }
    private static File writeTmpFile(byte[] data, List<File> tmpLst) {
        try {
            Path tmpPath = Files.createTempFile(
                    "sinetstream-",
                    ".tmp",
                    PosixFilePermissions.asFileAttribute(
                            PosixFilePermissions.fromString("rw-------")));
            File tmpFile = tmpPath.toFile();
            tmpFile.deleteOnExit();
            tmpLst.add(tmpFile);
            //log.info("XXX: k=" + k);
            //log.info("XXX: k2=" + k2);
            //log.info("XXX: tmpFile=" + tmpFile.getPath());
            //log.info("XXX: v=" + DatatypeConverter.printHexBinary((byte[])v));
            try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
                fos.write(data);
            }
            catch (IOException ex) {
                throw new SinetStreamException(ex);
            }
            return tmpFile;
        }
        catch (IOException ex) {
            throw new SinetStreamException(ex);
        }
    }

}
