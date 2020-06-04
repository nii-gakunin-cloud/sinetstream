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

import org.apache.commons.cli.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

@SuppressWarnings({"WeakerAccess"})
public class KafkaBinaryConsumer {

    private final String brokers;
    private final String topic_src;
    private final int qos;
    private final int nmsg;
    private final int timeout;
    private final String logfile;

    public KafkaBinaryConsumer(String brokers, String topic_src, int qos, int nmsg, int timeout, String logfile) {
        this.brokers = brokers;
        this.topic_src = topic_src;
        this.qos = qos;
        this.nmsg = nmsg;
        this.timeout = timeout;
        this.logfile = logfile;
    }

    public void run() throws Exception {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.brokers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "foobar");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        switch (this.qos) {
            case 0:
                break;
            case 1:
                break;
            case 2:
                break;
        }

        KafkaConsumer<String, byte[]> consumer =
                new KafkaConsumer<>(props, new StringDeserializer(), new ByteArrayDeserializer());
        consumer.subscribe(Collections.singletonList(this.topic_src));
        long[] ts = new long[this.nmsg];
        int[] ss = new int[this.nmsg];
        int n = 0;
        while (n < this.nmsg) {
            ConsumerRecords<String, byte[]> recs = consumer.poll(Duration.ofMillis(this.timeout));
            if (n == 0) {
                System.err.println("receiving");
            }
            long t = System.currentTimeMillis();
            //System.out.println("recs length=" + recs.count());
            if (recs.isEmpty()) {
                System.err.println("timedout");
                break;
            }
            for (ConsumerRecord<String, byte[]> rec : recs) {
                ts[n] = t;
                ss[n] = rec.value().length;
                n += 1;
                if (this.nmsg <= n)
                    break;
            }
        }
        consumer.commitSync();
        consumer.close();
        FileWriter file = new FileWriter(this.logfile);
        PrintWriter pw = new PrintWriter(new BufferedWriter(file));
        for (int i = 0; i < n; i++) {
            pw.println(ts[i] + "," + ss[i]);
        }
        pw.close();
    }

    public static void main(String[] args) {
        //String logfile = "kafkaJava.consumed." + Util.getTime() + "." + Util.getHostName() + ".csv";
        Options opts = new Options();
        opts.addOption(Option.builder("b").required().hasArg().longOpt("brokers").build());
        opts.addOption(Option.builder("S").required().hasArg().longOpt("topic_src").build());
        opts.addOption(Option.builder("Q").required().hasArg().longOpt("qos").build());
        opts.addOption(Option.builder("N").hasArg().longOpt("nmsg").build());
        opts.addOption(Option.builder("T").hasArg().longOpt("timeout").build());
        opts.addOption(Option.builder("f").required().hasArg().longOpt("logfile").build());

        CommandLineParser parser = new DefaultParser();
        KafkaBinaryConsumer consumer = null;
        try {
            CommandLine cmd = parser.parse(opts, args);
            consumer = new KafkaBinaryConsumer(
                    cmd.getOptionValue("brokers"),
                    cmd.getOptionValue("topic_src"),
                    Integer.parseInt(cmd.getOptionValue("qos")),
                    Integer.parseInt(cmd.getOptionValue("nmsg")),
                    Integer.parseInt(cmd.getOptionValue("timeout")),
                    cmd.getOptionValue("logfile")
            );
        } catch (ParseException e) {
            System.err.println("Parsing failed: " + e.getMessage());
            new HelpFormatter().printHelp(KafkaBinaryConsumer.class.getSimpleName(), opts, true);
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
