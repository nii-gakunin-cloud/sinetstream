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
import org.apache.commons.cli.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("WeakerAccess")
public class SinetstreamBinaryProducer {

    private final String service;
    private final String topic_dst;
    private final int qos;
    private final int bytes;
    private final int fps;
    private final int nmsg;
    private final String logfile;

    public SinetstreamBinaryProducer(String service, String topic_dst, int qos, int bytes, int fps, int nmsg, String logfile) {
        this.service = service;
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
        Consistency consistency = Consistency.valueOf(this.qos);
        MessageWriterFactory<byte[]> factory =
                MessageWriterFactory.<byte[]>builder()
                        .service(service)
                        .topic(topic_dst)
                        .consistency(consistency)
                        .valueType(SimpleValueType.BYTE_ARRAY)
                        .build();
        try(MessageWriter<byte[]> writer = factory.getWriter()) {
            long[] ts = new long[this.nmsg];
            byte[] buf = new byte[this.bytes];
            Random rnd = new Random();
            long next = System.currentTimeMillis() + interval;
            for (int i = 0; i < this.nmsg; i++) {
                //rnd.nextBytes(buf);
                writer.write(buf);
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
    }

    public static void main(String[] args) {
        //String logfile = "sinetstreamJava.produced." + Util.getTime() + "." + Util.getHostName() + ".csv";
        Options opts = new Options();
        opts.addOption(Option.builder("s").required().hasArg().longOpt("service").build());
        opts.addOption(Option.builder("d").required().hasArg().longOpt("topic_dst").build());
        opts.addOption(Option.builder("Q").required().hasArg().longOpt("qos").build());
        opts.addOption(Option.builder("B").hasArg().longOpt("bytes").build());
        opts.addOption(Option.builder("F").hasArg().longOpt("fps").build());
        opts.addOption(Option.builder("N").hasArg().longOpt("nmsg").build());
        opts.addOption(Option.builder("f").required().hasArg().longOpt("logfile").build());

        CommandLineParser parser = new DefaultParser();
        SinetstreamBinaryProducer producer = null;
        try {
            CommandLine cmd = parser.parse(opts, args);
            producer = new SinetstreamBinaryProducer(
                    cmd.getOptionValue("service"),
                    cmd.getOptionValue("topic_dst"),
                    Integer.parseInt(cmd.getOptionValue("qos")),
                    Integer.parseInt(cmd.getOptionValue("bytes", "40")),
                    Integer.parseInt(cmd.getOptionValue("fps", "15")),
                    Integer.parseInt(cmd.getOptionValue("nmsg", "5")),
                    cmd.getOptionValue("logfile")
                    );
        } catch (ParseException e) {
            System.err.println("Parsing failed: " + e.getMessage());
            new HelpFormatter().printHelp(SinetstreamBinaryProducer.class.getSimpleName(), opts, true);
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
