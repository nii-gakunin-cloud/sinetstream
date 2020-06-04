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
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("WeakerAccess")
public class KafkaBinaryProducer {

    private final String brokers;
    private final String topic_dst;
    private final int qos;
    private final int bytes;
    private final int fps;
    private final int nmsg;
    private final String logfile;

    public KafkaBinaryProducer(String brokers, String topic_dst, int qos, int bytes, int fps, int nmsg, String logfile) {
        this.brokers = brokers;
        this.topic_dst = topic_dst;
        this.qos = qos;
        this.bytes = bytes;
        this.fps = fps;
        this.nmsg = nmsg;
        this.logfile = logfile;
    }

    public void run() throws Exception {
        int interval;
        if (this.fps > 0) {
            interval = 1000 / this.fps;
            if (interval == 0) {
                System.err.println("fps " + this.fps + " is too short");
            }
        } else {
            interval = 0;
        }

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.brokers);
        switch (this.qos) {
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

        long[] ts = new long[this.nmsg];
        byte[] buf = new byte[this.bytes];
        //Random rnd = new Random();
        //rnd.nextBytes(buf);
        ProducerRecord<String, byte[]> record = new ProducerRecord<>(this.topic_dst, buf);
        long next = System.currentTimeMillis() + interval;
        for (int i = 0; i < this.nmsg; i++) {
            //producer.send(record).get();
            producer.send(record);
            long t = System.currentTimeMillis();
            ts[i] = t;
            if (interval > 0) {
                long rest = next - System.currentTimeMillis();
                if (rest > 0) {
                    TimeUnit.MILLISECONDS.sleep(rest);
                } else {
                    System.err.println("SLOW " + rest);
                }
                next += interval;
            }
        }
        FileWriter file = new FileWriter(this.logfile);
        PrintWriter pw = new PrintWriter(new BufferedWriter(file));
        for (int i = 0; i < this.nmsg; i++) {
            pw.println(ts[i] + "," + this.bytes);
        }
        pw.close();
    }

    public static void main(String[] args) {
        //String logfile = "kafkaJava.produced." + Util.getTime() + "." + Util.getHostName() + ".csv";
        Options opts = new Options();
        opts.addOption(Option.builder("b").required().hasArg().longOpt("brokers").build());
        opts.addOption(Option.builder("d").required().hasArg().longOpt("topic_dst").build());
        opts.addOption(Option.builder("Q").required().hasArg().longOpt("qos").build());
        opts.addOption(Option.builder("B").hasArg().longOpt("bytes").build());
        opts.addOption(Option.builder("F").hasArg().longOpt("fps").build());
        opts.addOption(Option.builder("N").hasArg().longOpt("nmsg").build());
        opts.addOption(Option.builder("f").required().hasArg().longOpt("logfile").build());

        CommandLineParser parser = new DefaultParser();
        KafkaBinaryProducer producer = null;
        try {
            CommandLine cmd = parser.parse(opts, args);
            producer = new KafkaBinaryProducer(
                    cmd.getOptionValue("brokers"),
                    cmd.getOptionValue("topic_dst"),
                    Integer.parseInt(cmd.getOptionValue("qos")),
                    Integer.parseInt(cmd.getOptionValue("bytes", "40")),
                    Integer.parseInt(cmd.getOptionValue("fps", "15")),
                    Integer.parseInt(cmd.getOptionValue("nmsg", "5")),
                    cmd.getOptionValue("logfile")
            );
        } catch (ParseException e) {
            System.err.println("Parsing failed: " + e.getMessage());
            new HelpFormatter().printHelp(KafkaBinaryProducer.class.getSimpleName(), opts, true);
            System.exit(1);
        }
        try {
            producer.run();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
