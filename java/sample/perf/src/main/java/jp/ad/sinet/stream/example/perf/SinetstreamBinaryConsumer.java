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

package jp.ad.sinet.stream.example.perf;

import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.api.Message;
import jp.ad.sinet.stream.api.MessageReader;
import jp.ad.sinet.stream.api.valuetype.SimpleValueType;
import jp.ad.sinet.stream.utils.MessageReaderFactory;
import org.apache.commons.cli.*;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Objects;

@SuppressWarnings({"WeakerAccess"})
public class SinetstreamBinaryConsumer {

    private final Util.ConsumerOptions opts;

    public SinetstreamBinaryConsumer(Util.ConsumerOptions opts) {
        this.opts = opts;
    }

    public void run() throws Exception {
        Consistency consistency = Consistency.valueOf(opts.qos);
        MessageReaderFactory<byte[]> factory =
                MessageReaderFactory.<byte[]>builder()
                        .service(opts.service)
                        .topic(opts.topic_src)
                        .consistency(consistency)
                        .valueType(SimpleValueType.BYTE_ARRAY)
                        .receiveTimeout(Duration.ofSeconds(opts.timeout1 / 1000))
                        .build();
        try (MessageReader<byte[]> reader = factory.getReader()) {
            long tstart = System.currentTimeMillis();
            long[] ts = new long[opts.nmsg];
            int[] ss = new int[opts.nmsg];
            int hdrlen = 4 + 8; // int+long
            byte[][] hdr = new byte[opts.nmsg][hdrlen];
            int n = 0;
            String error = null;
            Message<byte[]> msg;
            while (Objects.nonNull(msg = reader.read())) {
                if (System.currentTimeMillis() - tstart > opts.timeout2) {
                    error = "sub:timedout2";
                    System.err.println(error);
                    break;
                }
                if (n == 0) {
                    System.err.println("receiving");
                }
                byte[] bytes = msg.getValue();
                long t = System.currentTimeMillis();
                ts[n] = t;
                ss[n] = bytes.length;
                System.arraycopy(bytes, 0, hdr[n], 0, hdrlen);
                n += 1;
                if (opts.nmsg <= n)
                    break;
            }
            if (n < opts.nmsg) {
                error = "sub:timedout1";
                System.err.println(error);
            } else {
                System.err.println("done");
            }
            try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(Paths.get(opts.logfile)))) {
                pw.println("#recv,size,seq,send");
                for (int i = 0; i < n; i++) {
                    pw.println(ts[i] + "," + ss[i] + "," + Util.extractMetadata(hdr[i]).str());
                }
                if (error != null)
                    pw.println("# " + error);
            }
        }
    }

    public static void main(String[] args) {
        try {
            SinetstreamBinaryConsumer consumer = new SinetstreamBinaryConsumer(Util.parseConsumerOptions("SinetstreamBinaryConsumer", args));
            consumer.run();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
