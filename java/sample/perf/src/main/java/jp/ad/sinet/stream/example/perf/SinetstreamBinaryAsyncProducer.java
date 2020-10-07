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

import jp.ad.sinet.stream.api.AsyncMessageWriter;
import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.api.valuetype.SimpleValueType;
import jp.ad.sinet.stream.utils.MessageWriterFactory;
import org.apache.commons.cli.*;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.Collections;
import java.util.stream.Collectors;

public class SinetstreamBinaryAsyncProducer {

    private final Util.ProducerOptions opts;

    public SinetstreamBinaryAsyncProducer(Util.ProducerOptions opts) {
        this.opts = opts;
    }

    public void run() throws Exception {
        CountDownLatch done = new CountDownLatch(opts.nmsg);
        AtomicInteger errcnt = new AtomicInteger();
        List<Throwable> errlst = Collections.synchronizedList(new LinkedList<Throwable>());
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
        long[] ts = new long[opts.nmsg];
        Util.DataList dataList = new Util.DataList(opts.nmsg, opts.bytes);
        int n = 0;
        String error = null;
        Object metrics = null;
        try (AsyncMessageWriter<byte[]> writer = factory.getAsyncWriter()) {
            long next = System.nanoTime() + interval;
            for (int i = 0; i < opts.nmsg; i++) {
                final int nn = n;
                writer.write(dataList.getData(i))
                        .fail((ex) -> { long t = System.currentTimeMillis();
                                        ts[nn] = -t;
                                        errcnt.getAndIncrement();
                                        synchronized (errlst) { errlst.add(ex); } })
                        .done((r) -> { long t = System.currentTimeMillis();
                                       ts[nn] = t; })
                        .always((s, r, e) -> { done.countDown(); });
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
            boolean ok = done.await(opts.timeout2, TimeUnit.MILLISECONDS);
            if (!ok) {
                error = "pub:timedout2";
                System.err.println(error);
            }
            try {
                metrics = writer.getMetrics().getRaw();
            }
            catch (Exception e) {
            }
        }
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(Paths.get(opts.logfile)))) {
            pw.println("#comp,size,seq,send");
            for (int i = 0; i < n; i++) {
                /*
                if (i >= opts.nmsg) {
                        pw.println(ts[i] + "," + opts.bytes);
                        continue;
                }
                */
                long t = ts[i];
                int bytes = opts.bytes;
                if (t < 0) {
                    t = -t;
                    bytes = 0;
                }
                pw.println(t + "," + bytes + "," + Util.extractMetadata(dataList.peekData(i)).str());
            }
            pw.println("#errcnt " + errcnt.get());
            synchronized (errlst) {
                for (Throwable ex : errlst) {
                    pw.println("#error " + ex);
                }
            }
            if (error != null)
                pw.println("# " + error);
            if (metrics != null) {
                pw.println("#record-retry-total " + Util.get_kafka_metric(metrics, "record-retry-total", "producer-metrics"));
            }
        }
    }

    public static void main(String[] args) {
        try {
            SinetstreamBinaryAsyncProducer producer = new SinetstreamBinaryAsyncProducer(Util.parseProducerOptions("SinetstreamBinaryAsyncProducer", args));
            producer.run();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
