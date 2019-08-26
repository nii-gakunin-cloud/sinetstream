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

import jp.ad.sinet.stream.api.NoConfigException;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;

class ConfigLoader {

    private Map<String, Map<String, Object>> loadConfigFromEnvUrl() {
        return Optional.ofNullable(System.getenv("SINETSTREAM_CONFIG_URL"))
                .map(this::loadConfigFileFromURL).orElse(null);
    }

    private Map<String, Map<String, Object>> loadConfigFromHome() {
        return loadConfigFile(Paths.get(System.getProperty("user.home"), ".config", "sinetstream", "config.yml"));
    }

    private Map<String, Map<String, Object>> loadConfigFromCwd() {
        return loadConfigFile(Paths.get(".sinetstream_config.yml"));
    }

    Map<String, Map<String, Object>> loadConfigFile() {
        List<Supplier<Map<String, Map<String, Object>>>> configLoaders = Arrays.asList(
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
            Yaml yaml = new Yaml();
            return yaml.load(is);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Map<String, Map<String, Object>> loadConfigFile(Path path) {
        if (!path.toFile().exists()) {
            return null;
        }
        try (BufferedReader reader = Files.newBufferedReader(path, Charset.forName("UTF-8"))) {
            Yaml yaml = new Yaml();
            return yaml.load(reader);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
