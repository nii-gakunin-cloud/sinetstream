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

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import com.google.api.client.json.Json;
import com.google.api.client.testing.http.HttpTesting;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import jp.ad.sinet.stream.api.MessageReader;
import jp.ad.sinet.stream.api.MessageWriter;
import jp.ad.sinet.stream.api.NoServiceException;
import jp.ad.sinet.stream.crypto.SecretDecoder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigClientTest {
    // http://googleapis.github.io/google-http-java-client/unit-testing.html
    static String serviceName = "service-kafka-001";
    static String configName = "stream009";

    static Path copyInPrivKey() {
        Path privKeyFile = Paths.get("private_key.pem");
        try (InputStream in = ConfigClientTest.class.getResourceAsStream("/private_key.pem")) {
            Files.deleteIfExists(privKeyFile);
            Files.copy(in, privKeyFile);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        return privKeyFile;
    }

    private ConfigClient.AuthInfo makeAuthInfo() {
        ConfigClient.AuthInfo authInfo = new ConfigClient.AuthInfo();
        authInfo.configServer = new ConfigClient.AuthConfigServer();
        //authInfo.configServer.address = new String("https://exmaple.jp");
        authInfo.configServer.address = HttpTesting.SIMPLE_URL;
        authInfo.configServer.user = "testuser";
        authInfo.configServer.secretKey = "testsecretkey";
        return authInfo;
    }

    @Test
    void test_read_auth_json() throws IOException {
        try (InputStream in = ConfigClientTest.class.getResourceAsStream("/auth.json")) {
            Files.deleteIfExists(Paths.get("auth.json"));
            Files.copy(in, Paths.get("auth.json"));
            ConfigClient.AuthInfo authInfo = ConfigClient.readAuthFile(Paths.get("auth.json"));
            assertNotNull(authInfo);
            assertNotNull(authInfo.configServer);
            assertEquals(authInfo.configServer.user, "user123");
        }
        finally {
            Files.deleteIfExists(Paths.get("auth.json"));
        }
    }

    @Test
    void test_auth_401() {
        ConfigClient.AuthInfo authInfo = makeAuthInfo();
        HttpTransport transport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                System.err.println("XXX:buildRequest: method=" + method + " url=" + url);
                if (method.equals("POST") && url.endsWith("/api/v1/authentication")) {
                    return new MockLowLevelHttpRequest() {
                        @Override
                        public LowLevelHttpResponse execute() throws IOException {
                            MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                            response.setStatusCode(401);
                            response.setContentType("text/plain");
                            response.setContent("fail by mock");
                            return response;
                        }
                    };
                }
                System.err.println("XXX:NEED_TO_FIX_TEST");
                return null; // Oops
            }
        };
        Map<String, Object> params = ConfigClient.getConfig(serviceName, configName, authInfo, null, transport);
        assertNull(params);
    }

    private LowLevelHttpRequest make_authentication_201() {
        return new MockLowLevelHttpRequest() {
            @Override
            public LowLevelHttpResponse execute() throws IOException {
                MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                response.setStatusCode(201);
                response.setContentType(Json.MEDIA_TYPE);
                response.setContent("{ \"accessToken\": \"dummy token\", \"gomi\": \"gomi must be ignored\" }");
                return response;
            }
        };
    }

    @Test
    void test_configs_4XX() {
        ConfigClient.AuthInfo authInfo = makeAuthInfo();
        HttpTransport transport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                System.err.println("XXX:buildRequest: method=" + method + " url=" + url);
                if (method.equals("POST") && url.endsWith("/api/v1/authentication")) {
                    return make_authentication_201();
                }
                if (method.equals("GET") && url.endsWith("/api/v1/configs/stream009")) {
                    return new MockLowLevelHttpRequest() {
                        @Override
                        public LowLevelHttpResponse execute() throws IOException {
                            MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                            response.setStatusCode(404);
                            response.setContentType("text/plain");
                            response.setContent("fail by mock");
                            return response;
                        }
                    };
                }
                System.err.println("XXX:NEED_TO_FIX_TEST");
                return null; // Oops
            }
        };
        Map<String, Object> params = ConfigClient.getConfig(serviceName, configName, authInfo, null, transport);
        assertNull(params);
    }

    @Test
    void test_configs_simple() {
        ConfigClient.AuthInfo authInfo = makeAuthInfo();
        HttpTransport transport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                System.err.println("XXX:buildRequest: method=" + method + " url=" + url);
                if (method.equals("POST") && url.endsWith("/api/v1/authentication")) {
                    return make_authentication_201();
                }
                if (method.equals("GET") && url.endsWith("/api/v1/configs/stream009")) {
                    return new MockLowLevelHttpRequest() {
                        @Override
                        public LowLevelHttpResponse execute() throws IOException {
                            MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                            response.setStatusCode(200);
                            response.setContentType(Json.MEDIA_TYPE);
                            response.setContent(
                                "{\"name\": \"stream009\"," +
                                " \"config\": {" +
                                "     \"header\": {" +
                                "         \"version\": 2," +
                                "         \"fingerprint\": \"uso800\"" +
                                "     }," +
                                "     \"config\": {" +
                                "         \"service-kafka-001\": {" +
                                "             \"uso\": 800" +
                                "         }" +
                                "     }" +
                                " }," +
                                " \"attachments\": []," +
                                " \"secrets\": []" +
                                "}");
                            System.err.println("resp="+response.getContent());
                            return response;
                        }
                    };
                }
                System.err.println("XXX:NEED_TO_FIX_TEST");
                return null; // Oops
            }
        };
        Map<String, Object> params = ConfigClient.getConfig(serviceName, configName, authInfo, null, transport);
        assertNotNull(params);
        assertEquals(params.get("uso"), new java.math.BigDecimal(800));

        // if a service name is not specified...
        params = ConfigClient.getConfig(null, configName, authInfo, null, transport);
        assertNotNull(params);
        assertEquals(params.get("uso"), new java.math.BigDecimal(800));
    }

    @Test
    void test_configs_configs_attach() {
        ConfigClient.AuthInfo authInfo = makeAuthInfo();
        HttpTransport transport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                System.err.println("XXX:buildRequest: method=" + method + " url=" + url);
                if (method.equals("POST") && url.endsWith("/api/v1/authentication")) {
                    return make_authentication_201();
                }
                if (method.equals("GET") && url.endsWith("/api/v1/configs/stream009")) {
                    return new MockLowLevelHttpRequest() {
                        @Override
                        public LowLevelHttpResponse execute() throws IOException {
                            MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                            response.setStatusCode(200);
                            response.setContentType(Json.MEDIA_TYPE);
                            response.setContent(
                                "{\"name\": \"stream009\"," +
                                " \"config\": {" +
                                "     \"header\": {" +
                                "         \"version\": 2," +
                                "         \"fingerprint\": \"uso800\"" +
                                "     }," +
                                "     \"config\": {" +
                                "         \"service-kafka-001\": {" +
                                "             \"aaa\": \"AAA\"," +
                                "             \"bbb\": \"BBB\"" +
                                "         }" +
                                "     }" +
                                " }," +
                                " \"attachments\": [" +
                                "     {\"target\": \"*.aaa\",                 \"value\": \"QUFBLW92ZXJ3cml0ZQ==\"}," + // AAA-overwrite
                                "     {\"target\": \"service-kafka-001.bbb\", \"value\": \"QkJCLW92ZXJ3cml0ZQ==\"}," + // BBB-overwrite
                                "     {\"target\": \"service-kafka-999.bbb\", \"value\": \"QkJCLWlnbm9yZQ==\"}," + // BBB-ignore
                                "     {\"target\": \"*.ccc\",                 \"value\": \"Q0NDLWluc2VydA==\"}," + // CCC-insert
                                "     {\"target\": \"service-kafka-001.ddd\", \"value\": \"RERELWluc2VydA==\"}" + // DDD-insert
                                " ]" +
                                "}");
                            System.err.println("resp="+response.getContent());
                            return response;
                        }
                    };
                }
                System.err.println("XXX:NEED_TO_FIX_TEST");
                return null; // Oops
            }
        };
        Path privKeyFile = copyInPrivKey();
        Map<String, Object> params = ConfigClient.getConfig(serviceName, configName, authInfo, privKeyFile, transport);
        assertNotNull(params);
        assertArrayEquals((byte[])params.get("aaa"), "AAA-overwrite".getBytes());
        assertArrayEquals((byte[])params.get("bbb"), "BBB-overwrite".getBytes());
        assertArrayEquals((byte[])params.get("ccc"), "CCC-insert".getBytes());
        assertArrayEquals((byte[])params.get("ddd"), "DDD-insert".getBytes());

        // if a service name is not specified...
        params = ConfigClient.getConfig(serviceName, configName, authInfo, privKeyFile, transport);
        assertNotNull(params);
        assertArrayEquals((byte[])params.get("aaa"), "AAA-overwrite".getBytes());
        assertArrayEquals((byte[])params.get("bbb"), "BBB-overwrite".getBytes());
        assertArrayEquals((byte[])params.get("ccc"), "CCC-insert".getBytes());
        assertArrayEquals((byte[])params.get("ddd"), "DDD-insert".getBytes());
    }

    @Test
    void test_configs_secret_4XX() {
        ConfigClient.AuthInfo authInfo = makeAuthInfo();
        HttpTransport transport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                System.err.println("XXX:buildRequest: method=" + method + " url=" + url);
                if (method.equals("POST") && url.endsWith("/api/v1/authentication")) {
                    return make_authentication_201();
                }
                if (method.equals("GET") && url.endsWith("/api/v1/configs/stream009")) {
                    return new MockLowLevelHttpRequest() {
                        @Override
                        public LowLevelHttpResponse execute() throws IOException {
                            MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                            response.setStatusCode(200);
                            response.setContentType(Json.MEDIA_TYPE);
                            response.setContent(
                                "{\"name\": \"stream009\"," +
                                " \"config\": {" +
                                "     \"header\": {" +
                                "         \"version\": 2," +
                                "         \"fingerprint\": \"uso800\"" +
                                "     }," +
                                "     \"config\": {" +
                                "         \"service-kafka-001\": {" +
                                "         }" +
                                "     }" +
                                " }," +
                                " \"secrets\": [" +
                                "     {\"id\": \"secret_id_1\", \"target\": \"*.himitsu.value\"}," +
                                "     {\"ids\": [" +
                                "         {\"id\": \"secret_id_1\", \"version\": 1}," +
                                "         {\"id\": \"secret_id_2\", \"version\": 2}" +
                                "      ]," +
                                "      \"target\": \"*.himitsu2.value\"" +
                                "     }" +
                                " ]" +
                                "}");
                            System.err.println("resp="+response.getContent());
                            return response;
                        }
                    };
                }
                if (method.equals("GET") && url.endsWith("/api/v1/secrets/secret_id_1")) {
                    return new MockLowLevelHttpRequest() {
                        @Override
                        public LowLevelHttpResponse execute() throws IOException {
                            MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                            response.setStatusCode(404);
                            response.setContentType("text/plain");
                            response.setContent("fail by mock");
                            return response;
                        }
                    };
                }
                System.err.println("XXX:NEED_TO_FIX_TEST");
                return null; // Oops
            }
        };
        Map<String, Object> params = ConfigClient.getConfig(serviceName, configName, authInfo, null, transport);
        assertNull(params);
    }

    @Test
    @SuppressWarnings("unchecked")
    void test_configs_secret() throws Exception {
        ConfigClient.AuthInfo authInfo = makeAuthInfo();
        HttpTransport transport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                System.err.println("XXX:buildRequest: method=" + method + " url=" + url);
                if (method.equals("POST") && url.endsWith("/api/v1/authentication")) {
                    return make_authentication_201();
                }
                if (method.equals("GET") && url.endsWith("/api/v1/configs/stream009")) {
                    return new MockLowLevelHttpRequest() {
                        @Override
                        public LowLevelHttpResponse execute() throws IOException {
                            MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                            response.setStatusCode(200);
                            response.setContentType(Json.MEDIA_TYPE);
                            response.setContent(
                                "{\"name\": \"stream009\"," +
                                " \"config\": {" +
                                "     \"header\": {" +
                                "         \"version\": 2," +
                                "         \"fingerprint\": \"uso800\"" +
                                "     }," +
                                "     \"config\": {" +
                                "         \"service-kafka-001\": {" +
                                "         }" +
                                "     }" +
                                " }," +
                                " \"secrets\": [" +
                                "     {\"id\": \"secret_id_1\", \"target\": \"*.himitsu.value\"}," +
                                "     {\"ids\": [" +
                                "         {\"id\": \"secret_id_1\", \"version\": 1}," +
                                "         {\"id\": \"secret_id_2\", \"version\": 2}" +
                                "      ]," +
                                "      \"target\": \"*.himitsu2.value\"" +
                                "     }" +
                                " ]" +
                                "}");
                            System.err.println("resp="+response.getContent());
                            return response;
                        }
                    };
                }
                if (method.equals("GET") && url.endsWith("/api/v1/secrets/secret_id_1")) {
                    return new MockLowLevelHttpRequest() {
                        @Override
                        public LowLevelHttpResponse execute() throws IOException {
                            MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                            response.setStatusCode(200);
                            response.setContentType("text/plain");
                            response.setContentType(Json.MEDIA_TYPE);
                            // "abcdefghijklmn"
                            response.setContent(
                                    "{\"id\": \"secret_id_1\"," +
                                    " \"fingerprint\": \"SHA256:ZwpyervXIsTQXB1q7vJsSQA40m0Re3/Y4CSqZbGcdM4\"," +
                                    " \"target\": \"*.himitsu.value\"," +
                                    " \"value\": \"AAEBAWduqK8fiFsWoGDBenYQ3/rOtz5H+yK4Bk0bxEniT7Oz9Vu9pUc8sRVGPv7gaupsxpHK1YnH2N1vc3TWfLL07YlQ0cwdMuBsjwNe+nD/8sYdBxGu5KqtjH2v11OW8gdM6wN3Na1b+c4I1jf1axpTiinKD9nVD2oaubNYPrlAxwyNWUH8kWYx4GNFxmqEegDLmNEBrKvJLUKH8JAbnjF4zjqL3rgC/T3uKoFiCHH7wJbpM7mgiNZQj5Ti2pkBA0vctyAjkb2ByO45Qyd7mnpTfuvuLyBRh46bve4DJM4spnYnMpKWddKVVaYBuciASyxN/O+0egktO5A5LuejDqXcuyhZqiyeI9dHR1TKHQA5wqV3ALfM7/hQ3RD5UndJrTGo6JdjBSjMHUp9FlhPWLrIOyk4uaQ53sJ154tVGB1SCgH/MFX/KV6cq4XOsJPgYSwewOP1k5E5SGLtmGrnXZ2KJFpXh7CLtZHLcQxyKukxBzQPqyCtDLHD3hFtza3OqYziDQAAAAAAAAAAAAAByDGbqD7H9pWsFkkDCoon3MFAWVUTQ9Ri2p0BHfrHww==\"" +
                                    "}");
                            return response;

                        }
                    };
                }
                if (method.equals("GET") && url.endsWith("/api/v1/secrets/secret_id_2")) {
                    return new MockLowLevelHttpRequest() {
                        @Override
                        public LowLevelHttpResponse execute() throws IOException {
                            MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                            response.setStatusCode(200);
                            response.setContentType("text/plain");
                            response.setContentType(Json.MEDIA_TYPE);
                            // "opqrstuvwxyz"
                            response.setContent(
                                    "{\"id\": \"secret_id_2\"," +
                                    " \"fingerprint\": \"SHA256:ZwpyervXIsTQXB1q7vJsSQA40m0Re3/Y4CSqZbGcdM4\"," +
                                    " \"target\": \"*.himitsu2.value\"," +
                                    " \"value\": \"AAEBARWzeaWBP0iivNiJ6hAedKJkALLhpN4tjuIVZOhbj9DC1pfxMOJByUkVWxWR+AywJdDsAGuM9Rwv/9VUAcvhkwtv38SINDpbwACtjgNnnIJxMLRnvN8a7epL28kM0xuff27NN8QlARaYWqCcZME569tzgByp/vjfK4k1jAymQMUgsKDitMzkZAwR9tgx2t06jA4Cx7xWnKfoYGf0sWNZ0dvMedJ9zZOqLId3M9Ist1Wf43c8ERrusXhaXpufwhyPUo1DYYHFRrME6Fi9+xS5Zn+r5rZJfQqCvgbIddqeKjNTfXAt5SkrBMuNH8vpp87QZdHPROMDF8iEYuEqoUPt4Qn31LOsA3zkuYyAHhQyykA5wps4KkK3OKgOZaN/jDYm9wq8PuPU+7hZxA6Zx1ys8XWaOU01eAGf89psmguU0/ZYlUm67x9kbNpu6rU8PqezAADy/yGCiR3veQBaicFSVwhDaMm9LZqG2BtGAHhSoV2L/fVCvPUdDTaro8Z3pnccswAAAAAAAAAAAAAByD+JuijR5IeyCFsRHNnuy8BNdZ/aHBu4XWAxQLM=\"" +
                                    "}");
                            return response;
                        }
                    };
                }
                System.err.println("XXX:NEED_TO_FIX_TEST");
                return null; // Oops
            }
        };

        Path privKeyFile = copyInPrivKey();
        Map<String, Object> params = ConfigClient.getConfig(serviceName, configName, authInfo, privKeyFile, transport);
        assertNotNull(params);

        try (InputStream in = ConfigClientTest.class.getResourceAsStream("/private_key.pem")) {
            Files.deleteIfExists(privKeyFile);
            Files.copy(in, privKeyFile);

            SecretDecoder decoder = new SecretDecoder(privKeyFile);
            decoder.setupPrivKey();
            MessageUtils utils = new MessageUtils();
            Map<String, Object> params2 = utils.debugDecryptoParameters(params, decoder);
            Map<String, Object> x = (Map<String, Object>) params2.get("himitsu");
            assertNotNull(x);
            assertArrayEquals((byte[])x.get("value"), "abcdefghijklmn".getBytes());
            Map<String, Object> x2 = (Map<String, Object>) params2.get("himitsu2");
            assertNotNull(x2);
            assertArrayEquals((byte[])x2.get("value"), "opqrstuvwxyz".getBytes());
        }
        finally {
            Files.deleteIfExists(privKeyFile);
        }
    }

    @Test
    void test_configName1() throws IOException{
        try (InputStream in = ConfigClientTest.class.getResourceAsStream("/auth.json")) {
            Path authFile = Paths.get("auth.json");
            Files.deleteIfExists(authFile);
            Files.copy(in, authFile);

            HttpTransport transport = new MockHttpTransport() {
                @Override
                public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                    System.err.println("XXX:buildRequest: method=" + method + " url=" + url);
                    if (method.equals("POST") && url.endsWith("/api/v1/authentication")) {
                        return make_authentication_201();
                    }
                    if (method.equals("GET") && url.endsWith("/api/v1/configs/stream009")) {
                        return new MockLowLevelHttpRequest() {
                            @Override
                            public LowLevelHttpResponse execute() throws IOException {
                                MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                                response.setStatusCode(200);
                                response.setContentType(Json.MEDIA_TYPE);
                                response.setContent(
                                    "{\"name\": \"stream009\"," +
                                    " \"config\": {" +
                                    "     \"header\": {" +
                                    "         \"version\": 2," +
                                    "         \"fingerprint\": \"uso800\"" +
                                    "     }," +
                                    "     \"config\": {" +
                                    "         \"service-kafka-001\": {" +
                                    "             \"type\": \"dummy\"," +
                                    "             \"topic\": \"test-topic\"," +
                                    "             \"uso\": 800" +
                                    "         }" +
                                    "     }" +
                                    " }," +
                                    " \"attachments\": []," +
                                    " \"secrets\": []" +
                                    "}");
                                System.err.println("resp="+response.getContent());
                                return response;
                            }
                        };
                    }
                    System.err.println("XXX:NEED_TO_FIX_TEST");
                    return null; // Oops
                }
            };

            MessageWriterFactory<String> writerBuilder = MessageWriterFactory.<String>builder()
                    .service(serviceName)
                    .configName(configName)
                    .authFile(authFile)
                    .debugHttpTransport(transport).build();
            try (MessageWriter<String> writer = writerBuilder.getWriter()) {
                assertEquals(writer.getConfig().get("uso"), new java.math.BigDecimal(800));
            }

            MessageReaderFactory<String> readerBuilder = MessageReaderFactory.<String>builder()
                    .service(serviceName)
                    .configName(configName)
                    .authFile(authFile)
                    .debugHttpTransport(transport).build();
            try (MessageReader<String> reader = readerBuilder.getReader()) {
                assertEquals(reader.getConfig().get("uso"), new java.math.BigDecimal(800));
            }
        }
    }

    @Test
    void test_configName2() throws IOException{
        try (InputStream in = ConfigClientTest.class.getResourceAsStream("/auth.json")) {
            Path authFile = Paths.get("auth.json");
            Files.deleteIfExists(authFile);
            Files.copy(in, authFile);

            HttpTransport transport = new MockHttpTransport() {
                @Override
                public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                    System.err.println("XXX:buildRequest: method=" + method + " url=" + url);
                    if (method.equals("POST") && url.endsWith("/api/v1/authentication")) {
                        return make_authentication_201();
                    }
                    if (method.equals("GET") && url.endsWith("/api/v1/configs/stream009")) {
                        return new MockLowLevelHttpRequest() {
                            @Override
                            public LowLevelHttpResponse execute() throws IOException {
                                MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                                response.setStatusCode(404);
                                response.setContentType("text/plain");
                                response.setContent("fail by mock");
                                return response;
                            }
                        };
                    }
                    System.err.println("XXX:NEED_TO_FIX_TEST");
                    return null; // Oops
                }
            };

            MessageWriterFactory<String> writerBuilder = MessageWriterFactory.<String>builder()
                    .service(serviceName)
                    .configName(configName)
                    .authFile(authFile)
                    .debugHttpTransport(transport).build();
            boolean caughtNoServiceExceptionW = false;
            try (MessageWriter<String> writer = writerBuilder.getWriter()) {
                // NOP
            }
            catch (NoServiceException e) {
                caughtNoServiceExceptionW = true;
            }
            assertEquals(caughtNoServiceExceptionW, true);

            MessageReaderFactory<String> readerBuilder = MessageReaderFactory.<String>builder()
                    .service(serviceName)
                    .configName(configName)
                    .authFile(authFile)
                    .debugHttpTransport(transport).build();
            boolean caughtNoServiceExceptionR = false;
            try (MessageReader<String> reader = readerBuilder.getReader()) {
                // NOP
            }
            catch (NoServiceException e) {
                caughtNoServiceExceptionR = true;
            }
            assertEquals(caughtNoServiceExceptionR, true);
        }
    }
}
