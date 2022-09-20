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

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("WeakerAccess")
public class KafkaBinaryProducer {

    private final Util.ProducerOptions opts;

    public KafkaBinaryProducer(Util.ProducerOptions opts) {
        this.opts = opts;
    }

    public void run() throws Exception {
        AtomicInteger errcnt = new AtomicInteger();
        int interval; // [ns]
        if (opts.fps > 0) {
            interval = 1000000000 / opts.fps;
            if (interval == 0) {
                System.err.println("fps " + opts.fps + " is too short");
            }
        } else {
            interval = 0;
        }

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, opts.brokers);
        switch (opts.qos) {
            case 0:
                props.put(ProducerConfig.ACKS_CONFIG, "0");
                break;
            case 1:
                props.put(ProducerConfig.ACKS_CONFIG, "all");
                break;
            case 2:
                System.err.println("warning: qos=2 is not implemented");
                break;
        }
        KafkaProducer<String, byte[]> producer = new KafkaProducer<>(props, new StringSerializer(), new ByteArraySerializer());

        CountDownLatch done = new CountDownLatch(opts.nmsg);
        long tstart = System.currentTimeMillis();
        long[] ts = new long[opts.nmsg];
	/*
        Future<RecordMetadata>[] fus = new Future[opts.nmsg];
	*/
        Util.DataList dataList = new Util.DataList(opts.nmsg, opts.bytes);
        long next = System.nanoTime() + interval;
        int n = 0;
        String error = null;
        for (int i = 0; i < opts.nmsg; i++) {
            if (System.currentTimeMillis() - tstart > opts.timeout2) {
                error = "pub:timedout2";
                System.err.println(error);
                break;
            }
            ProducerRecord<String, byte[]> record = new ProducerRecord<>(opts.topic_dst, dataList.getData(i));
            if (opts.sync_mode) {
                Future<RecordMetadata> fu = producer.send(record);
                fu.get(opts.timeout1, TimeUnit.MILLISECONDS);
                long t = System.currentTimeMillis();
                ts[n] = t;
            } else {
                final int nn = n;
                Callback cb = new Callback() {
                    public void onCompletion(RecordMetadata metadata, Exception e) {
                        long t = System.currentTimeMillis();
                        if(e != null) {
                            errcnt.getAndIncrement();
                            // e.printStackTrace();
                            ts[nn] = -t;
                        } else {
                            ts[nn] = t;
                        }
                        done.countDown();
                    }
                };
                producer.send(record, cb);
                /*
                Future<RecordMetadata> fu = producer.send(record);
                fus[i] = fu;
                */
            }
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
        if (!opts.sync_mode) {
            for (int i = 0; i < opts.nmsg - n; i++)
                done.countDown();
            boolean ok = done.await(opts.timeout2, TimeUnit.MILLISECONDS);
            if (!ok) {
                error = "pub:timeout2";
                System.err.println(error);
            }
            /*
            for (int i = 0; i < n; i++) {
                fus[i].get(opts.timeout1, TimeUnit.MILLISECONDS);
                long t = System.currentTimeMillis();
                ts[i] = t;
            }
            */
        }
        Map<MetricName,? extends Metric> metrics = producer.metrics();

        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(Paths.get(opts.logfile)))) {
            pw.println("#comp,size,seq,send");
            for (int i = 0; i < n; i++) {
                long t = ts[i];
                int bytes = opts.bytes;
                if (t < 0) {
                    t = -t;
                    bytes = 0;
                }
                pw.println(t + "," + bytes + "," + Util.extractMetadata(dataList.peekData(i)).str());
            }
            pw.println("#errcnt " + errcnt.get());
            if (error != null)
                pw.println("# " + error);
            pw.println("#record-retry-total " + Util.get_kafka_metric(metrics, "record-retry-total", "producer-metrics"));
        }
    }

    public static void main(String[] args) {
        try {
            KafkaBinaryProducer producer = new KafkaBinaryProducer(Util.parseProducerOptions("KafkaBinaryProducer", args));
            producer.run();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
