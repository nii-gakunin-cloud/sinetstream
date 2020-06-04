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
import org.apache.commons.cli.*;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SinetstreamBinaryAsyncConsumer {

    private final String service;
    private final String topic_src;
    private final int qos;
    private final int nmsg;
    private final int timeout;
    private final String logfile;

    public SinetstreamBinaryAsyncConsumer(String service, String topic_src, int qos, int nmsg, int timeout, String logfile) {
        this.service = service;
        this.topic_src = topic_src;
        this.qos = qos;
        this.nmsg = nmsg;
        this.timeout = timeout;
        this.logfile = logfile;
    }

    public void run() throws Exception {
        CountDownLatch done = new CountDownLatch(this.nmsg);
        Consistency consistency = Consistency.valueOf(this.qos);
        MessageReaderFactory<byte[]> factory =
                MessageReaderFactory.<byte[]>builder()
                        .service(service)
                        .topic(topic_src)
                        .consistency(consistency)
                        .valueType(SimpleValueType.BYTE_ARRAY)
                        .build();
        long[] ts = new long[this.nmsg];
        int[] ss = new int[this.nmsg];
        try (AsyncMessageReader<byte[]> reader = factory.getAsyncReader()) {
            AtomicInteger n = new AtomicInteger();
            reader.addOnMessageCallback((msg -> {
                int idx = n.getAndIncrement();
                ts[idx] = System.currentTimeMillis();
                ss[idx] = msg.getValue().length;
                if (idx == 0) {
                    System.err.println("receiving");
                }
                done.countDown();
            }));
            if (!done.await(this.timeout, TimeUnit.MILLISECONDS)) {
                System.err.println("timedout");
            }
        }
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(Paths.get(this.logfile)))) {
            for (int i = 0, size = ts.length; i < size; i++) {
                pw.printf("%d,%d\n", ts[i], ss[i]);
            }
        }
    }

    public static void main(String[] args) {
        //String logfile = "sinetstreamJava.consumed." + Util.getTime() + "." + Util.getHostName() + ".csv";
        Options opts = new Options();
        opts.addOption(Option.builder("s").required().hasArg().longOpt("service").build());
        opts.addOption(Option.builder("S").required().hasArg().longOpt("topic_src").build());
        opts.addOption(Option.builder("Q").required().hasArg().longOpt("qos").build());
        opts.addOption(Option.builder("N").hasArg().longOpt("nmsg").build());
        opts.addOption(Option.builder("T").hasArg().longOpt("timeout").build());
        opts.addOption(Option.builder("f").required().hasArg().longOpt("logfile").build());

        CommandLineParser parser = new DefaultParser();
        SinetstreamBinaryAsyncConsumer consumer = null;
        try {
            CommandLine cmd = parser.parse(opts, args);
            consumer = new SinetstreamBinaryAsyncConsumer(
                    cmd.getOptionValue("service"),
                    cmd.getOptionValue("topic_src"),
                    Integer.parseInt(cmd.getOptionValue("qos")),
                    Integer.parseInt(cmd.getOptionValue("nmsg", "10")),
                    Integer.parseInt(cmd.getOptionValue("timeout", "600000")),
                    cmd.getOptionValue("logfile")
            );
        } catch (ParseException e) {
            System.err.println("Parsing failed: " + e.getMessage());
            new HelpFormatter().printHelp(SinetstreamBinaryAsyncConsumer.class.getSimpleName(), opts, true);
            System.exit(1);
        }
        try {
            consumer.run();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
