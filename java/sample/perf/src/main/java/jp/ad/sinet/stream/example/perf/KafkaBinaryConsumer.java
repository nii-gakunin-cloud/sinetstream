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

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

@SuppressWarnings({"WeakerAccess"})
public class KafkaBinaryConsumer {

    private final Util.ConsumerOptions opts;

    public KafkaBinaryConsumer(Util.ConsumerOptions opts) {
        this.opts = opts;
    }

    public void run() throws Exception {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, opts.brokers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "foobar");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        switch (opts.qos) {
            case 0:
                break;
            case 1:
                break;
            case 2:
                break;
        }

        KafkaConsumer<String, byte[]> consumer =
                new KafkaConsumer<>(props, new StringDeserializer(), new ByteArrayDeserializer());
        consumer.subscribe(Collections.singletonList(opts.topic_src));
        long tstart = System.currentTimeMillis();
        long[] ts = new long[opts.nmsg];
        int[] ss = new int[opts.nmsg];
        int hdrlen = 4 + 8; // int+long
        byte[][] hdr = new byte[opts.nmsg][hdrlen];
        int n = 0;
        String error = null;
        while (n < opts.nmsg) {
            if (System.currentTimeMillis() - tstart > opts.timeout2) {
                error = "sub:timedout2";
                System.err.println(error);
                break;
            }
            ConsumerRecords<String, byte[]> recs = consumer.poll(Duration.ofMillis(opts.timeout1));
            long t = System.currentTimeMillis();
            //System.out.println("recs length=" + recs.count());
            if (recs.isEmpty()) {
                error = "sub:timedout1";
                System.err.println(error);
                break;
            }
            if (n == 0) {
                System.err.println("receiving");
            }
            for (ConsumerRecord<String, byte[]> rec : recs) {
                ts[n] = t;
                ss[n] = rec.value().length;
                System.arraycopy(rec.value(), 0, hdr[n], 0, hdrlen);
                n++;
                if (opts.nmsg <= n)
                    break;
            }
        }
        System.err.println("done");
        consumer.commitSync();
        consumer.close();
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(Paths.get(opts.logfile)))) {
            pw.println("#recv,size,seq,send");
            for (int i = 0; i < n; i++) {
                pw.println(ts[i] + "," + ss[i] + "," + Util.extractMetadata(hdr[i]).str());
            }
            if (error != null)
                pw.println("# " + error);
        }
    }

    public static void main(String[] args) {
        try {
            KafkaBinaryConsumer consumer = new KafkaBinaryConsumer(Util.parseConsumerOptions("KafkaBinaryConsumer", args));
            consumer.run();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
