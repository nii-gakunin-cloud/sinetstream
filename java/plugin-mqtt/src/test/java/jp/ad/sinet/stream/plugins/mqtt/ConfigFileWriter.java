/*
 * Copyright (C) 2020 National Institute of Informatics
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.	See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package jp.ad.sinet.stream.plugins.mqtt;

import jp.ad.sinet.stream.api.Message;
import jp.ad.sinet.stream.api.MessageReader;
import jp.ad.sinet.stream.api.MessageWriter;
import jp.ad.sinet.stream.api.SinetStreamIOException;
import jp.ad.sinet.stream.api.valuetype.SimpleValueType;
import jp.ad.sinet.stream.utils.MessageReaderFactory;
import jp.ad.sinet.stream.utils.MessageWriterFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

import static org.apache.commons.lang3.reflect.TypeUtils.isInstance;
import static org.junit.jupiter.api.Assertions.*;

// 追加試験のコールバック用のメソッド
interface AdditionalTest {
    void callback(MessageWriter<?> writer, MessageReader<?> reader);

    void callback(MessageWriter<?> writer);

    void callback(MessageReader<?> reader);
}

interface TestMethod {
    void exec(String service, AdditionalTest additional) throws IOException;
}

public abstract class ConfigFileWriter {

    @TempDir
    protected static Path tmpDir;

    static final String serviceType = "mqtt";
    static final String configName = ".sinetstream_config.yml";

    static private String method = "";
    static private int testNo = 0;

    private static final List<String> certs = Arrays.asList(
            "cacert.pem", "client0.crt", "client0.key", "client0-enc.key",
            "bad-client.crt", "bad-client.key", "ca.p12", "client0.p12"
    );

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
        for (String name : certs) {
            Path path = TlsTest.makeTempCert(name);
            vars.put(name, path.toAbsolutePath().normalize().toString());
        }

        // コンフィグに MAP の情報を埋め込む
        System.out.println("--------------------------------------------------------");
        System.out.println("■ " + module + "." + method + "() : Test No." + testNo);
        try (BufferedWriter writer = Files.newBufferedWriter(this.getFilePath(configName))) {
            lines.stream().map(line -> {
                StringSubstitutor ss = new StringSubstitutor(vars);
                return ss.replace(line);
            }).forEach(line -> {
                try {
                    System.out.println(line);
                    writer.write(line);
                    writer.newLine();
                } catch (IOException e) {
                    System.out.println(e.toString());
                    throw new SinetStreamIOException(e);
                }
            });
        }
    }

    /**
     * write read 実行
     */
    private void execWriteRead(String service, AdditionalTest additional) {

        // コンフィグパス取得
        Path config = this.getFilePath(configName);

        // ファクトリ初期化
        MessageWriterFactory<String> writerBuilder =
                MessageWriterFactory.<String>builder()
                        .config(config)
                        .service(service)
                        .valueType(SimpleValueType.TEXT)
                        .build();

        MessageReaderFactory<String> readerBuilder =
                MessageReaderFactory.<String>builder()
                        .config(config)
                        .service(service)
                        .receiveTimeout(Duration.ofSeconds(5))
                        .valueType(SimpleValueType.TEXT)
                        .build();

        // 正常に通信ができればＯＫ
        MessageReader<String> reader = readerBuilder.getReader();
        MessageWriter<String> writer = writerBuilder.getWriter();
        reader.read();
        final String data = RandomStringUtils.randomAlphabetic(10);
        writer.write(data);
        Message<String> result = reader.read();
        assertEquals(data, result.getValue());

        // 追加の試験を実施する
        if (!Objects.isNull(additional)) {
            additional.callback(writer, reader);
        }
    }

    /**
     * write 実行
     */
    private void execWrite(String service, AdditionalTest additional) {

        // コンフィグパス取得
        Path config = this.getFilePath(configName);

        // ファクトリ初期化
        MessageWriterFactory<String> writerBuilder =
                MessageWriterFactory.<String>builder()
                        .config(config)
                        .service(service)
                        .valueType(SimpleValueType.TEXT)
                        .build();

        // 正常に write できればＯＫ
        MessageWriter<String> writer = writerBuilder.getWriter();
        final String data = RandomStringUtils.randomAlphabetic(10);
        writer.write(data);

        // 追加の試験を実施する
        if (!Objects.isNull(additional)) {
            additional.callback(writer);
        }
    }

    /**
     * read 実行
     */
    private void execRead(String service, AdditionalTest additional) {

        // コンフィグパス取得
        Path config = this.getFilePath(configName);

        // ファクトリ初期化
        MessageReaderFactory<String> readerBuilder =
                MessageReaderFactory.<String>builder()
                        .config(config)
                        .service(service)
                        .receiveTimeout(Duration.ofSeconds(5))
                        .valueType(SimpleValueType.TEXT)
                        .build();

        // 正常に read できればＯＫ
        MessageReader<String> reader = readerBuilder.getReader();
        Message<String> result = reader.read();

        // 追加の試験を実施する
        if (!Objects.isNull(additional)) {
            additional.callback(reader);
        }
    }

    /**
     * 書き込み読み込み試験
     */
    protected void testWriteRead(String service, Class<Throwable> expected) {
        this.testWriteRead(service, expected, null);
    }

    protected void testWriteRead(String service, Class<Throwable> expected, AdditionalTest additional) {
        System.out.println("write read:");
        this.test(service, expected, additional, this::execWriteRead);
    }

    /**
     * 書き込み試験
     */
    protected void testWrite(String service, Class<Throwable> expected) {
        this.testWrite(service, expected, null);
    }

    protected void testWrite(String service, Class<Throwable> expected, AdditionalTest additional) {
        System.out.println("write:");
        this.test(service, expected, additional, this::execWrite);
    }

    /**
     * 読み込み試験
     */
    protected void testRead(String service, Class<Throwable> expected) {
        this.testRead(service, expected, null);
    }

    protected void testRead(String service, Class<Throwable> expected, AdditionalTest additional) {
        System.out.println("read:");
        this.test(service, expected, additional, this::execRead);
    }

    /**
     * 試験実行
     */
    private void test(String service, Class<Throwable> expected, AdditionalTest additional, TestMethod method) {

        // 試験を実行する
        if (Objects.isNull(expected)) {
            //
            // 正常終了が予想される時
            //
            assertDoesNotThrow(() -> {
                try {
                    // 実行
                    method.exec(service, additional);

                    // 正常終了した場合は OK
                    System.out.println("expected: Normal End");
                    System.out.println("actual:   Normal End");
                    System.out.println("Test No." + testNo + ": OK");
                } catch (Throwable e) {
                    // 異常終了した場合は NG
                    System.out.println("expected: Normal End");
                    System.out.println("actual:   " + e.toString());
                    System.out.println("Test No." + testNo + ": NG");
                    throw e;
                }
            });
        } else {
            //
            // 異常終了が予想される時
            //
            assertThrows(expected, () -> {
                try {
                    // 実行
                    method.exec(service, additional);

                    // 正常終了した場合は NG
                    System.out.println("expected: " + expected.toString().replace("class ", ""));
                    System.out.println("actual:   Normal End");
                    System.out.println("Test No." + testNo + ": NG");
                } catch (Throwable e) {
                    // 異常終了して Exception が一致した場合は OK
                    System.out.println("expected: " + expected.toString().replace("class ", ""));
                    System.out.println("actual:   " + e.toString());
                    System.out.println("Test No." + testNo + ": " + ((isInstance(e, expected)) ? "OK" : "NG"));
                    throw e;
                }
            });
        }
    }

    /**
     * 指定されたフィアルのパスを取得する
     */
    protected Path getFilePath(String filename) {
        return tmpDir.resolve(filename);
    }

    /**
     * トピックを作成する (ラベル指定あり)
     */
    protected String getTopic(String label) {
        // ラベルを付与したトピック名を生成する
        return "mss-test/"
                + (label == null ? "" : (label + "-"))
                + RandomStringUtils.randomNumeric(5);
    }
}
