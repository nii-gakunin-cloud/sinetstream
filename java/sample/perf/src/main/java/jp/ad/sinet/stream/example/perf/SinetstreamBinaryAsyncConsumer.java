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

import jp.ad.sinet.stream.api.AsyncMessageReader;
import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.api.valuetype.SimpleValueType;
import jp.ad.sinet.stream.utils.MessageReaderFactory;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SinetstreamBinaryAsyncConsumer {

    private final Util.ConsumerOptions opts;

    public SinetstreamBinaryAsyncConsumer(Util.ConsumerOptions opts) {
        this.opts = opts;
    }

    public void run() throws Exception {
        CountDownLatch done = new CountDownLatch(opts.nmsg);
        Consistency consistency = Consistency.valueOf(opts.qos);
        MessageReaderFactory<byte[]> factory =
                MessageReaderFactory.<byte[]>builder()
                        .service(opts.service)
                        .topic(opts.topic_src)
                        .consistency(consistency)
                        .valueType(SimpleValueType.BYTE_ARRAY)
                        .build();
        long tstart = System.currentTimeMillis();
        long[] ts = new long[opts.nmsg];
        int[] ss = new int[opts.nmsg];
        int hdrlen = 4 + 8; // int+long
        byte[][] hdr = new byte[opts.nmsg][hdrlen];
        int nn = 0;
        String error = null;
        try (AsyncMessageReader<byte[]> reader = factory.getAsyncReader()) {
            AtomicInteger n = new AtomicInteger();
            reader.addOnMessageCallback((msg -> {
                int idx = n.getAndIncrement();
                ts[idx] = System.currentTimeMillis();
                ss[idx] = msg.getValue().length;
                System.arraycopy(msg.getValue(), 0, hdr[idx], 0, hdrlen);
                if (idx == 0) {
                    System.err.println("receiving");
                }
                done.countDown();
            }));
            if (done.await(opts.timeout2, TimeUnit.MILLISECONDS)) {
                System.err.println("done");
            } else {
                error = "sub:timedout2";
                System.err.println(error);
            }
            nn = n.get();
        }
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(Paths.get(opts.logfile)))) {
            pw.println("#recv,size,seq,send");
            for (int i = 0; i < nn; i++) {
                pw.println(ts[i] + "," + ss[i] + "," + Util.extractMetadata(hdr[i]).str());
            }
            if (error != null)
                pw.println("# " + error);
        }
    }

    public static void main(String[] args) {
        try {
            SinetstreamBinaryAsyncConsumer consumer = new SinetstreamBinaryAsyncConsumer(Util.parseConsumerOptions("SinetstreamBinaryAsyncConsumer", args));
            consumer.run();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
