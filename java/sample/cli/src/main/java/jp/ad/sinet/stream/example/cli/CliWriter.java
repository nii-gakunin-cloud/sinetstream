/*
 * Copyright (C) 2022 National Institute of Informatics
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

package jp.ad.sinet.stream.example.cli;

import jp.ad.sinet.stream.api.MessageWriter;
import jp.ad.sinet.stream.api.valuetype.SimpleValueType;
import jp.ad.sinet.stream.utils.MessageWriterFactory;

import lombok.AllArgsConstructor;
import lombok.ToString;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

@AllArgsConstructor
@ToString
class CliWriter {
    private String service;
    private String config;
    private boolean text;
    private String file;
    private String message;
    private boolean line;
    private Map<String, Object> configs;

    private interface Reader extends Iterator, Closeable {
    }

    private class StringReader implements Reader {
        private String message;
        private StringReader(String message) {
            this.message = message;
        }
        public boolean hasNext() { return message != null; }
        public String next() {
            String r = message;
            message = null;
            return r;
        }
        public void close() { message = null; }
    }

    private class LineReader implements Reader {
        private Scanner scanner;
        public LineReader(InputStream is) {
            CliUtil.debug("LineReader");
            scanner = new Scanner(is);
        }
        public boolean hasNext() { return scanner.hasNextLine(); }
        public String next() { return scanner.nextLine(); }
        public void close() throws IOException { scanner.close(); }
    }

    private class ByteArrayReader implements Reader {
        private InputStream is;
        private boolean eof;
        public ByteArrayReader(InputStream is) {
            CliUtil.debug("ByteArrayReader");
            this.is = is;
            this.eof = false;
        }
        public boolean hasNext() { return !eof; }
        public byte[] next() {
            if (eof)
                return null;
            eof = true;
            try {
                return is.readAllBytes();
            }
            catch (IOException e) {
                CliUtil.err("ByteArrayReader: " + e);
                return null;
            }
        }
        public void close() throws IOException {
            is.close();
        }
    }

    private class TextReader implements Reader {
        private InputStream is;
        private boolean eof;
        public TextReader(InputStream is) {
            CliUtil.debug("TextReader");
            this.is = is;
            this.eof = false;
        }
        public boolean hasNext() { return !eof; }
        public String next() {
            if (eof)
                return null;
            eof = true;
            try {
                return new String(is.readAllBytes());
            }
            catch (IOException e) {
                CliUtil.err("TextReader: " + e);
                return null;
            }
        }
        public void close() throws IOException {
            is.close();
        }
    }

    public void run() throws Exception {
        CliUtil.debug("CliWriter.run: %s\n", this.toString());
        MessageWriterFactory.MessageWriterFactoryBuilder builder = MessageWriterFactory.builder();
        if (service == null && config == null) {
            service = CliUtil.makeTempConfig();
        }
        if (service != null)
            builder.service(service);
        if (config != null)
            builder.configName(config);
        if (text || message != null || line)
            configs.put("value_type", "text");
        Object o = configs.get("value_type");
        boolean textmode = o instanceof String && "text".equals((String)o);
        builder.valueType(textmode ? SimpleValueType.TEXT : SimpleValueType.BYTE_ARRAY);
        if (configs != null)
            builder.parameters(configs);

        MessageWriterFactory factory = builder.build();
        try (MessageWriter writer = factory.getWriter();
             InputStream is = message != null ? null :
                              file != null    ? new FileInputStream(file)
                                              : System.in;
             Reader reader = message != null ? new StringReader(message) :
                             line            ? new LineReader(is) :
                             text            ? new TextReader(is)
                                             : new ByteArrayReader(is))
        {
            while (reader.hasNext()) {
                Object x = reader.next();
                writer.write(x);
            }
        }
    }
}
