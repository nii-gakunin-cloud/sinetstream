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

package jp.ad.sinet.stream.api;

import jp.ad.sinet.stream.utils.MessageReaderFactory;
import jp.ad.sinet.stream.utils.MessageWriterFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InteropTest {

    private String dataDir = "src/test/java/jp/ad/sinet/stream/interop/data/";
    private String javaDir = dataDir + "java/";
    private List<byte[]> testmsgs = new ArrayList<byte[]>();
    private List<String> services = new ArrayList<String>();

    private void write(String name, byte[] b) throws IOException {
        try (FileOutputStream os = new FileOutputStream(name)) {
            os.write(b);
            os.close();
        }
    }
    private byte[] read(String name) throws IOException {
        File file = new File(name);
        byte[] b = new byte[(int)file.length()];
        try (FileInputStream is = new FileInputStream(name)) {
            int nr = is.read(b);
            assert(nr == b.length);
            is.close();
        }
        return b;
    }

    @Test
    void test_crypto() {
        mkdir();
        for (String service: services) {
            System.err.println("TEST:service=" + service);
            try {
                test_crypto(service);
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }
    void mkdir() {
        new File(javaDir).mkdir();
    }
    void test_crypto(String service) throws IOException {
        test_encrypto(service);
        test_decrypto(service);
    }
    void test_encrypto(String service) throws IOException {
        MessageWriterFactory<byte[]> writerBuilder =
                MessageWriterFactory.<byte[]>builder().service(service)
                        .build();
        try (MessageWriter<byte[]> writer = writerBuilder.getWriter()) {
            int i = 1;
            for (byte[] testmsg: testmsgs) {
                String result;
                String comment = "";
                try {
                    writer.write(testmsg);
                    write(javaDir + service + "-" + i + ".in", testmsg);
                    write(javaDir + service + "-" + i + ".out", writer.getDebugLastMsgBytes());
                    result = "OK";
                } 
                catch (Exception e) {
                    e.printStackTrace();
                    result = "NG";
                    comment = "#" + e.getMessage();
                }
                System.err.printf("TEST:RESULT: java encode %s %d %s %s\n", service, i, result, comment);
                i++;
            }
        }
    }
    void test_decrypto(String service) throws IOException {
        MessageReaderFactory<byte[]> readerBuilder =
                MessageReaderFactory.<byte[]>builder().service(service)
                        .receiveTimeout(Duration.ofMillis(100))
                        .build();
        try (MessageReader<byte[]> reader = readerBuilder.getReader()) {
            for (String ent : new File(dataDir).list()) {
                String dir = dataDir + ent + "/";
                if (!new File(dir).isDirectory())
                    continue;
                try {
                    for (int i = 1; ; i++) {
                        byte[] bin = read(dir + service + "-" + i + ".in");
                        byte[] bout = read(dir + service + "-" + i + ".out");
                        reader.debugInjectMsgBytes(bout, "xxx-topic", null);
                        String result;
                        String comment = "";
                        try {
                            Message<byte[]> msg = reader.read();
                            if (Arrays.equals(bin, msg.getValue())) {
                                result = "OK";
                            } else {
                                result = "NG";
                                comment = "#MALFORMED";
                            }
                        }
                        catch (Throwable e) {
                            e.printStackTrace();
                            result = "NG";
                            comment = "#" + e.getMessage();
                        }
                        System.err.printf("TEST:RESULT: %s java %s %d %s %s\n", ent, service, i, result, comment);
                    }
                }
                catch (FileNotFoundException e) {
                }
            }
        }
    }

    @BeforeEach
    void setupConfig() throws IOException {
        FileReader reader = new FileReader(dataDir + "dot.sinetstream_config.yml");
        Yaml yaml = new Yaml();
        Map<String, Map<String, Object>> config = yaml.load(reader);

        for (Iterator<Map.Entry<String, Map<String, Object>>> iter = config.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry<String, Map<String, Object>> service = iter.next();
            Object test = (String)service.getValue().remove("TEST");
            if (test != null) {
                String act = (String)test;
                if (act.equals("ONLY")) {
                    config = new HashMap<String, Map<String, Object>>();
                    config.put(service.getKey(), service.getValue());
                    break;
                }
                if (act.equals("SKIP")) {
                    iter.remove();
                }
            }
        }

        FileWriter writer = new FileWriter(".sinetstream_config.yml");
        yaml.dump(config, writer);
        writer.close();

        for (String service: config.keySet()) {
            services.add(service);
        }

        testmsgs.add("abc".getBytes());
        testmsgs.add("".getBytes());
        testmsgs.add("a".getBytes());
        testmsgs.add(("abcdefghijklmnopqrstuvwxyz" +
                  "abcdefghijklmnopqrstuvwxyz" +
                  "abcdefghijklmnopqrstuvwxyz" +
                  "abcdefghijklmnopqrstuvwxyz" +
                  "abcdefghijklmnopqrstuvwxyz" +
                  "abcdefghijklmnopqrstuvwxyz" +
                  "abcdefghijklmnopqrstuvwxyz" +
                  "abcdefghijklmnopqrstuvwxyz" +
                  "abcdefghijklmnopqrstuvwxyz" +
                  "abcdefghijklmnopqrstuvwxyz").getBytes());
    }

    @AfterEach
    void cleanConfig() throws IOException {
        Files.deleteIfExists(Paths.get(".sinetstream_config.yml"));
    }
}
