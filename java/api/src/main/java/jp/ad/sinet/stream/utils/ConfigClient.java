/*
 * Copyright (C) 2021 National Institute of Informatics
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

import jp.ad.sinet.stream.crypto.SecretDecoder;

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.Key;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import jp.ad.sinet.stream.api.InvalidConfigurationException;
import jp.ad.sinet.stream.api.NoServiceException;
import lombok.extern.java.Log;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Log
public class ConfigClient {
    public static class AuthConfigServer {
        public String address;
        public String user;
        @SerializedName("secret-key")
        public String secretKey;
        @SerializedName("expiration-date")
        public String expirationDate;
    }
    public static class AuthInfo {
        @SerializedName("config-server")
        public AuthConfigServer configServer;
    }
    static AuthInfo readAuthFile(Path authFile) {
        if (authFile == null)
            authFile = Paths.get(System.getProperty("user.home"), ".config", "sinetstream", "auth.json");
        try (Reader reader = Files.newBufferedReader(authFile)) {
            Gson gson = new Gson();
            return gson.fromJson(reader, AuthInfo.class);
        }
        catch (IOException e) {
            System.out.println(e);
            return null;
        }
    }
    private static class ConfigServer implements AutoCloseable {
        private HttpTransport httpTransport;
        private HttpRequestFactory httpRequestFactory;
        private JsonFactory jsonFactory;
        private GenericUrl baseUrl;
        private String accessToken;

        public ConfigServer(String address) {
            httpTransport = new NetHttpTransport();
            httpRequestFactory = httpTransport.createRequestFactory();
            jsonFactory = GsonFactory.getDefaultInstance();
            baseUrl = new GenericUrl(address + "/api/v1");
        }

        public ConfigServer(String address, HttpTransport httpTransport) {
            this.httpTransport = httpTransport;
            httpRequestFactory = httpTransport.createRequestFactory();
            jsonFactory = GsonFactory.getDefaultInstance();
            baseUrl = new GenericUrl(address + "/api/v1");
        }

        // https://qiita.com/alt_yamamoto/items/0d72276c80589493ceb4
        // https://github.com/googleapis/google-http-java-client
        public static class AuthenticationResUser extends GenericJson {
            @Key
            public long id;
            @Key
            public String name;
        }
        public static class AuthenticationRes extends GenericJson {
            @Key
            public String accessToken;
            @Key
            public AuthenticationResUser user;
        }
        public void postAuthentication(String user, String accessKey) throws IOException {
            GenericUrl url = baseUrl.clone();
            url.appendRawPath("/authentication");

            Map<String, Object> params = new java.util.HashMap<>();
            params.put("strategy", "api-access");
            params.put("user", user);
            params.put("secret-key", accessKey);

            try {
                HttpRequest hreq = httpRequestFactory.buildPostRequest(url, new JsonHttpContent(jsonFactory, params));
                hreq.setParser(new JsonObjectParser(GsonFactory.getDefaultInstance()));
                HttpResponse hres = hreq.execute();
                try {
                    log.fine(hres.getStatusCode() + " " + hres.getStatusMessage());
                    log.fine(hres.getContentType());
                    if (hres.getStatusCode() != 201) {
                        throw new InvalidConfigurationException("server failure: " + hres.getStatusCode());
                    }
                    AuthenticationRes res = hres.parseAs(AuthenticationRes.class);
                    accessToken = res.accessToken;
                }
                finally {
                    hres.disconnect();
                }
            }
            catch (IOException e){
                System.out.println(e);
                throw e;
            }
        }
        public void close() {
            try {
                httpTransport.shutdown();
            }
            catch (IOException e) {
                System.out.println(e);
            }
        }
        public static class ConfigsRes extends GenericJson {
            public static class Config extends GenericJson {
                public static class Header extends GenericJson {
                    @Key
                    public long version;
                    @Key
                    public String fingerprint;
                }
                public static class ConfigConfig extends GenericJson {
                }

                @Key
                public Header header;
                @Key
                public ConfigConfig config;
            }
            public static class Attachment extends GenericJson {
                @Key
                public String value;
                @Key
                public String target;
            }
            public static class Secret extends GenericJson {
                public static class SId extends GenericJson {
                    @Key
                    public String id;
                    @Key
                    public long version;
                }
                @Key
                public List<SId> ids;
                @Key
                public String id;
                @Key
                public String target;
            }

            @Key
            public String name;
            @Key
            public Config config;
            @Key
            public List<Attachment> attachments;
            @Key
            public List<Secret> secrets;
        }
        public ConfigsRes getConfigs(String configName) {
            GenericUrl url = baseUrl.clone();
            url.appendRawPath("/configs/" + configName);

            try {
                HttpRequest hreq = httpRequestFactory.buildGetRequest(url);
                HttpHeaders headers = new HttpHeaders();
                headers.setAuthorization("Bearer " + accessToken);
                hreq.setHeaders(headers);
                hreq.setParser(new JsonObjectParser(GsonFactory.getDefaultInstance()));
                HttpResponse hres = hreq.execute();
                try {
                    log.fine(hres.getStatusCode() + " " + hres.getStatusMessage());
                    log.fine(hres.getContentType());
                    if (hres.getStatusCode() != 200) {
                        throw new InvalidConfigurationException("server failure: " + hres.getStatusCode());
                    }
                    ConfigsRes res = hres.parseAs(ConfigsRes.class);
                    return res;
                }
                finally {
                    hres.disconnect();
                }
            }
            catch (IOException e){
                System.out.println(e);
                throw new InvalidConfigurationException(e);
            }
        }

        public static class SecretsRes extends GenericJson {
            @Key
            public String id;
            @Key
            public String fingerprint;
            @Key
            public String target;
            @Key
            public String value;
        }
        public SecretsRes getSecret(String secId, String fingerprint) {
            GenericUrl url = baseUrl.clone();
            url.appendRawPath("/secrets/" + secId);

            try {
                HttpRequest hreq = httpRequestFactory.buildGetRequest(url);
                HttpHeaders headers = new HttpHeaders();
                headers.setAuthorization("Bearer " + accessToken);
                if (fingerprint != null) {
                    headers.set("SINETStream-config-publickey", fingerprint);
                }
                hreq.setHeaders(headers);
                hreq.setParser(new JsonObjectParser(GsonFactory.getDefaultInstance()));
                HttpResponse hres = hreq.execute();
                try {
                    log.fine(hres.getStatusCode() + " " + hres.getStatusMessage());
                    log.fine(hres.getContentType());
                    if (hres.getStatusCode() != 200) {
                        throw new InvalidConfigurationException("server failure: " + hres.getStatusCode());
                    }
                    SecretsRes res = hres.parseAs(SecretsRes.class);
                    return res;
                }
                finally {
                    hres.disconnect();
                }
            }
            catch (IOException e){
                System.out.println(e);
                throw new InvalidConfigurationException(e);
            }
        }
    }
    @SuppressWarnings("unchecked")
    private static <T> void update_value(Map<String,Object> params, String[] tpath, T value) {
        for (int i = 1; i < tpath.length - 1; i++) {
            String k = tpath[i];
            if (!params.containsKey(k))
                params.put(k, new HashMap<String,Object>());
            Object child = params.get(k);
            if (!(child instanceof Map)) {
                throw new InvalidConfigurationException("key " + k + " in the config must be Map<>");
            }
            params = (Map<String,Object>) child;
        }
        params.put(tpath[tpath.length - 1], value);
    }
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getConfig(String serviceName, String configName, boolean needAllKey, Path authFile, Path privKeyFile, Object debugHttpTransport) {
        AuthInfo authInfo = readAuthFile(authFile);
        HttpTransport httpTransport = debugHttpTransport != null ? (HttpTransport) debugHttpTransport : new NetHttpTransport();
        return getConfig(serviceName, configName, needAllKey, authInfo, privKeyFile, httpTransport);
    }
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getConfig(String serviceName, String configName, boolean needAllKey, AuthInfo authInfo, Path privKeyFile, HttpTransport httpTransport) {
        try (ConfigServer configServer = new ConfigServer(authInfo.configServer.address, httpTransport)) {
            configServer.postAuthentication(authInfo.configServer.user, authInfo.configServer.secretKey);
            ConfigServer.ConfigsRes configs = configServer.getConfigs(configName);
            log.fine("XXX:configs=" + configs);
            if (configs.name == null) {
                throw new InvalidConfigurationException("server must return name");
            }
            if (!configs.name.equals(configName)) {
                throw new InvalidConfigurationException("server must return name=" + configName + ", but " + configs.name);
            }
            Map<String,Object> params;
            if (configs.config.header != null) {
                // if the header section exists, assume version >= 2.
                if (configs.config.header.version != 2) {
                    throw new InvalidConfigurationException("config verison must be 2, but " + configs.config.header.version);
                }
                if (configs.config.config == null) {
                    throw new InvalidConfigurationException("config section does not exist");
                }
                params = (Map<String,Object>) MessageUtils.getServiceParam((Map<String,Object>) configs.config.config, serviceName);
            } else {
                // assume version 1
                params = (Map<String,Object>) MessageUtils.getServiceParam(configs.config, serviceName);
            }
            if (params == null) {
                throw new NoServiceException();
            }

            // expand attachments
            if (configs.attachments != null) {
                Base64.Decoder b64decoder = Base64.getDecoder();
                for (ConfigServer.ConfigsRes.Attachment attachment : configs.attachments) {
                    String[] tpath = attachment.target.split("[.]");
                    if (tpath.length < 2) {
                        throw new InvalidConfigurationException("target " +  attachment.target + " is too short");
                    }
                    if (tpath[0].equals("*") || tpath[0].equals(serviceName)) {
                        byte[] value = b64decoder.decode(attachment.value);
                        update_value(params, tpath, value);
                    }
                }
            }

            // expand secrets
            if (configs.secrets != null) {
                Base64.Decoder b64decoder = Base64.getDecoder();
                SecretDecoder secretDecoder = null;
                String fp = null;
                for (ConfigServer.ConfigsRes.Secret secret : configs.secrets) {
                    String[] tpath = secret.target.split("[.]");
                    if (tpath.length < 2) {
                        throw new InvalidConfigurationException("target " +  secret.target + " is too short");
                    }
                    if (tpath[0].equals("*") || tpath[0].equals(serviceName)) {
                        if (secretDecoder == null) {
                            try {
                                secretDecoder = new SecretDecoder(privKeyFile);
                                secretDecoder.setupPrivKey();
                                fp = secretDecoder.getFingerprint();
                            }
                            catch (Exception e) {
                                log.warning("config failure: " + e);
                                throw new RuntimeException(e);
                            }
                        }
                        if (pathCompare(tpath, 1, "crypto", "key")) {
                            Map<Integer, SecretValue> keys = new HashMap<>();
                            // writer: read max version on "crypto.key", set to "crypto._keys"
                            // reader: read all version on "crypto.key", set to "crypto._keys"
                            long maxVer = 0;
                            for (ConfigServer.ConfigsRes.Secret.SId sid : secret.ids) {
                                if (!needAllKey && sid.version <= maxVer)
                                    continue;
                                ConfigServer.SecretsRes sec = configServer.getSecret(sid.id, fp);
                                if (!sec.id.equals(sid.id) || (fp != null && !sec.fingerprint.equals("SHA256:" + fp)) || !sec.target.equals(secret.target)) {
                                    throw new InvalidConfigurationException("server returns malformed response: " + sec);
                                }
                                if (!needAllKey)
                                    keys.clear();
                                keys.put((int)sid.version, new SecretValue(b64decoder.decode(sec.value), sec.fingerprint, secretDecoder));
                            }
                            if (keys.isEmpty())
                                throw new InvalidConfigurationException("no valid id exists for " + secret.target);
                            String[] crypto_keys = { "<dummy>", "crypto", "_keys" };
                            update_value(params, crypto_keys, keys);
                        } else {
                            String secId = null;
                            if (secret.ids != null) {
                                long maxVer = 0;
                                for (ConfigServer.ConfigsRes.Secret.SId sid : secret.ids) {
                                    if (sid.version > maxVer && sid.id != null) {
                                        secId = sid.id;
                                        maxVer = sid.version;
                                    }
                                }
                            } else if (secret.id != null) {
                                secId = secret.id;
                            }
                            if (secId == null) {
                                throw new InvalidConfigurationException("no valid id exists for " + secret.target);
                            }
                            ConfigServer.SecretsRes sec = configServer.getSecret(secId, fp);
                            if (!sec.id.equals(secId) || (fp != null && !sec.fingerprint.equals("SHA256:" + fp)) || !sec.target.equals(secret.target)) {
                                throw new InvalidConfigurationException("server returns malformed response: " + sec);
                            }
                            update_value(params, tpath, new SecretValue(b64decoder.decode(sec.value), sec.fingerprint, secretDecoder));
                        }
                    }
                }
            }
            return  params;
        }
        catch (RuntimeException | IOException e) {
            log.warning("config failure: " + e);
            //e.printStackTrace();
            return null;
        }
    }

    static boolean pathCompare(String[] tpath, int index, String... ss) {
        int end = tpath.length;
        if (end <= index)
            return false;
        if (end - index != ss.length)
            return false;
        int k = 0;
        for (int i = index; i < end; i++, k++) {
            if (!tpath[i].equals(ss[k]))
                return false;
            }
        return true;
    }
}
