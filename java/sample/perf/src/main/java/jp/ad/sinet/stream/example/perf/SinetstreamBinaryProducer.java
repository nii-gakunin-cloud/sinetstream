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
import jp.ad.sinet.stream.api.MessageWriter;
import jp.ad.sinet.stream.api.valuetype.SimpleValueType;
import jp.ad.sinet.stream.utils.MessageWriterFactory;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("WeakerAccess")
public class SinetstreamBinaryProducer {

    private final Util.ProducerOptions opts;

    public SinetstreamBinaryProducer(Util.ProducerOptions opts) {
        this.opts = opts;
    }

    public void run() throws Exception {
        int interval; // [ns]
        if (opts.fps > 0) {
            interval = 1000000000 / opts.fps;
            if (interval == 0) {
                System.err.println("fps " + opts.fps + " is too short");
            }
        } else {
            interval = 0;
        }
        Consistency consistency = Consistency.valueOf(opts.qos);
        MessageWriterFactory<byte[]> factory =
                MessageWriterFactory.<byte[]>builder()
                        .service(opts.service)
                        .topic(opts.topic_dst)
                        .consistency(consistency)
                        .valueType(SimpleValueType.BYTE_ARRAY)
                        .build();
        try (MessageWriter<byte[]> writer = factory.getWriter()) {
            long tstart = System.currentTimeMillis();
            long[] ts = new long[opts.nmsg];
            Util.DataList dataList = new Util.DataList(opts.nmsg, opts.bytes);
            long next = System.nanoTime() + interval;
            int n = 0;
            String error = null;
            for (int i = 0; i < opts.nmsg; i++) {
                if (System.currentTimeMillis() - tstart > opts.timeout2) {
                    error ="pub:timedout2";
                    System.err.println(error);
                    break;
                }
                writer.write(dataList.getData(i));
                long t = System.currentTimeMillis();
                ts[n] = t;
                n++;
                if (interval > 0) {
                    long rest = next - System.nanoTime();
                    if (rest > 0) {
                        TimeUnit.NANOSECONDS.sleep(rest);
                    } else {
                        //System.err.println("SLOW " + rest);
                    }
                    next += interval;
                }
            }
            try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(Paths.get(opts.logfile)))) {
                pw.println("#comp,size,seq,send");
                for (int i = 0; i < n; i++) {
                    pw.println(ts[i] + "," + opts.bytes + "," + Util.extractMetadata(dataList.peekData(i)).str());
                }
                if (error != null)
                    pw.println("# " + error);
            }
        }
    }

    public static void main(String[] args) {
        try {
            SinetstreamBinaryProducer producer = new SinetstreamBinaryProducer(Util.parseProducerOptions("SinetstreamBinaryProducer", args));
            producer.run();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
