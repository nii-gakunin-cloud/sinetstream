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

import jp.ad.sinet.stream.api.Message;
import jp.ad.sinet.stream.api.MessageReader;
import jp.ad.sinet.stream.api.valuetype.SimpleValueType;
import jp.ad.sinet.stream.utils.MessageReaderFactory;

import lombok.AllArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.FileOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@AllArgsConstructor
@ToString
class CliReader {
    private boolean noConfigFile;
    private String service;
    private String config;
    private boolean text;
    private boolean verbose;
    private boolean raw;
    private String outdir;
    private Long count;
    private Map<String, Object> configs;

    private String quote(String x) throws Exception {
        return URLEncoder.encode(x, "UTF-8");
    }

    public void run() throws Exception {
        CliUtil.debug("CliReader.run: %s\n", this.toString());
        MessageReaderFactory.MessageReaderFactoryBuilder builder = MessageReaderFactory.builder();
        builder.noConfig(noConfigFile);
        if (service != null)
            builder.service(service);
        if (config != null)
            builder.configName(config);
        if (text)
            configs.put("value_type", "text");
        Object o = configs.get("value_type");
        boolean textmode = o instanceof String && "text".equals((String)o);
        builder.valueType(textmode ? SimpleValueType.TEXT : SimpleValueType.BYTE_ARRAY);
        if (configs != null)
            builder.parameters(configs);

        String rand = outdir != null ? RandomStringUtils.randomAlphanumeric(16) : null;

        MessageReaderFactory factory = builder.build();
        try (MessageReader reader = factory.getReader()) {
            for (int n = 0; count == null || n < count; ) {
                Message m = reader.read();
                if (m == null)
                    break;
                n++;
                if (verbose || !raw) {
                    System.out.printf("[#%d] Received on \"%s\"\n", n, m.getTopic());
                }
                OutputStream os;
                if (outdir != null) {
                    String fn = String.format("%s-%s-%d", quote(m.getTopic()), rand, n);
                    CliUtil.debug(Paths.get(outdir, fn).toString());
                    os = new FileOutputStream(Paths.get(outdir, fn).toString());
                } else {
                    os = System.out;
                }
                if (textmode) {
                    String s = (String) m.getValue();
                    if (outdir == null && !s.endsWith("\n"))
                        s += "\n";
                    os.write(s.getBytes());
                } else {
                    os.write((byte[]) m.getValue());
                }
                if (outdir != null)
                    os.close();
            }
        }
    }
}
