/*
 * Copyright (C) 2020 National Institute of Informatics
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

package jp.ad.sinet.stream.plugins.kafka;

import jp.ad.sinet.stream.api.Message;
import jp.ad.sinet.stream.api.MessageReader;
import jp.ad.sinet.stream.api.MessageWriter;
import jp.ad.sinet.stream.api.SinetStreamIOException;
import jp.ad.sinet.stream.utils.MessageReaderFactory;
import jp.ad.sinet.stream.utils.MessageWriterFactory;
import lombok.extern.java.Log;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.*;
import java.util.logging.Level;

import static org.apache.commons.lang3.reflect.TypeUtils.isInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Log
public abstract class ConfigFileWriter {

    @TempDir
    protected static Path tmpDir;

    static final String SERVICE_TYPE = "kafka";
    private static final String CONFIG_NAME = ".sinetstream_config.yml";

    private static String method = "";
    private static int testNo = 0;

    protected Map<String, URL> certUrl = Collections.emptyMap();

    /**
     * コンフィグをテンポラリに書き込む
     */
    protected void writeConfigFile(List<String> lines) throws IOException {

        // 試験番号を設定する
        String c = Thread.currentThread().getStackTrace()[2].getClassName();
        String module = c.substring(c.lastIndexOf('.') + 1);

        String m = Thread.currentThread().getStackTrace()[2].getMethodName();

        if (!method.equals(m)) {
            method = m;
            testNo = 0;
        }
        testNo++;

        // キーファイル等の MAP<ファイル名, 絶対パス> を作成する
        Map<String, String> vars = new HashMap<>();
        for (String name : certUrl.keySet()) {
            Path path = name.endsWith(".p12") ? makeTempKeyStore(name) : makeTempPemFile(name);
            vars.put(name, path.toAbsolutePath().normalize().toString());
        }

        // コンフィグに MAP の情報を埋め込む
        System.out.println("--------------------------------------------------------");
        System.out.println("■ " + module + "." + method + "() : Test No." + testNo);
        try (BufferedWriter writer = Files.newBufferedWriter(this.getFilePath(CONFIG_NAME))) {
            lines.stream().map(line -> {
                StringSubstitutor ss = new StringSubstitutor(vars);
                return ss.replace(line);
            }).forEach(line -> {
                try {
                    System.out.println(line);
                    writer.write(line);
                    writer.newLine();
                } catch (IOException e) {
                    log.log(Level.FINE, "write config file", e);
                    throw new SinetStreamIOException(e);
                }
            });
        }
    }

    private Path makeTempKeyStore(String filename) throws IOException {
        Path path = Files.createTempFile(null, ".p12");
        try (InputStream in = certUrl.get(filename).openStream()) {
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        }
        path.toFile().deleteOnExit();
        return path;
    }

    private Path makeTempPemFile(String filename) throws IOException {
        Path path = Files.createTempFile(null, ".pem");
        try (InputStream in = certUrl.get(filename).openStream()) {
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        }
        path.toFile().deleteOnExit();
        return path;
    }

    /**
     * 試験用の通信メソッド
     */
    protected void execWriteRead(String service) {

        // コンフィグパス取得
        Path config = this.getFilePath(CONFIG_NAME);

        // ファクトリ初期化
        MessageWriterFactory<String> writerBuilder =
                MessageWriterFactory.<String>builder()
                        .config(config)
                        .service(service)
                        .build();

        MessageReaderFactory<String> readerBuilder =
                MessageReaderFactory.<String>builder()
                        .config(config)
                        .service(service)
                        .receiveTimeout(Duration.ofSeconds(1))
                        .build();

        // 正常に通信ができればＯＫ
        try (MessageWriter<String> writer = writerBuilder.getWriter();
             MessageReader<String> reader = readerBuilder.getReader()) {
            reader.read();
            final String data = RandomStringUtils.randomAlphabetic(10);
            writer.write(data);
            Message<String> result = reader.read();
            assertEquals(data, result.getValue());
        }
    }

    /**
     * 試験実行
     */
    protected void execTest(String service, Class<Throwable> expected) {

        // 試験を実行する
        if (Objects.isNull(expected)) {
            // 正常終了が予想される時
            try {
                this.execWriteRead(service);

                System.out.println("expected Normal End");
                System.out.println("result   Normal End");
                System.out.println("Test No." + testNo + ": OK");
            } catch (Throwable e) {
                System.out.println("expected Normal End");
                System.out.println("result  " + e.toString());
                System.out.println("Test No." + testNo + ": NG");
                throw e;
            }
        } else {
            // 異常終了が予想される時
            assertThrows(expected, () -> {
                try {
                    this.execWriteRead(service);

                    System.out.println("expected " + expected.toString().replace("class ", ""));
                    System.out.println("result   Normal End");
                    System.out.println("Test No." + testNo + ": NG");
                } catch (Throwable e) {
                    System.out.println("expected " + expected.toString().replace("class ", ""));
                    System.out.println("result   " + e.toString());
                    System.out.println("Test No." + testNo + ": " + ((isInstance(e, expected)) ? "OK" : "NG"));
                    throw e;
                }
            });
        }
    }

    /**
     * 指定されたフィアルのパスを取得する
     */
    private Path getFilePath(String filename) {
        return tmpDir.resolve(filename);
    }

    /**
     * トピックを作成する (ラベル指定あり)
     */
    protected String getTopic(String label) {
        // ラベルを付与したトピック名を生成する
        return "topic-"
                + SERVICE_TYPE + "-"
                + (label == null ? "" : (label + "-"))
                + RandomStringUtils.randomNumeric(5);
    }
}
