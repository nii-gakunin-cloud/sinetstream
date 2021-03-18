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

import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.Metric;
import java.util.Map;

import lombok.Value;

public class Util {
    public static String getHostName() {
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
            hostname = hostname.split("[.]", 2)[0];
        }
        catch (Exception e) {
            e.printStackTrace();
            hostname = "UNKNOWN";
        }
        return hostname;
    }

    public static String getTime() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
        return sdf.format(c.getTime());
    }

    final static String DEFAULT_QOS = "1";
    final static String DEFAULT_BYTES = "40";
    final static String DEFAULT_FPS = "0";
    final static String DEFAULT_NMSG = "10";
    final static String DEFAULT_TIMEOUT1 = "60000";
    final static String DEFAULT_TIMEOUT2 = "3600000";

    @Value
    static public class ProducerOptions {
        public String service;
        public String brokers;
        public String topic_dst;
        public int qos;
        public int bytes;
        public int fps;
        public int nmsg;
        public int timeout1;
        public int timeout2;
        public boolean sync_mode;
        public String logfile;
        public String extra;
    }

    public static ProducerOptions parseProducerOptions(String cmdLineSyntax, String[] args) {
        Options opts = new Options();
        try {
            opts.addOption(Option.builder().hasArg().longOpt("service").build());
            opts.addOption(Option.builder().hasArg().longOpt("brokers").build());
            opts.addOption(Option.builder().required().hasArg().longOpt("topic_dst").build());
            opts.addOption(Option.builder().hasArg().longOpt("qos").build());
            opts.addOption(Option.builder().hasArg().longOpt("bytes").build());
            opts.addOption(Option.builder().hasArg().longOpt("fps").build());
            opts.addOption(Option.builder().hasArg().longOpt("nmsg").build());
            opts.addOption(Option.builder().hasArg().longOpt("timeout1").build());
            opts.addOption(Option.builder().hasArg().longOpt("timeout2").build());
            opts.addOption(Option.builder().hasArg().longOpt("sync").build());
            opts.addOption(Option.builder().required().hasArg().longOpt("logfile").build());
            opts.addOption(Option.builder().hasArg().longOpt("extra").build());

            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(opts, args);

            return new ProducerOptions(
                    cmd.getOptionValue("service"),
                    cmd.getOptionValue("brokers"),
                    cmd.getOptionValue("topic_dst"),
                    Integer.parseInt(cmd.getOptionValue("qos", DEFAULT_QOS)),
                    Integer.parseInt(cmd.getOptionValue("bytes", DEFAULT_BYTES)),
                    Integer.parseInt(cmd.getOptionValue("fps", DEFAULT_FPS)),
                    Integer.parseInt(cmd.getOptionValue("nmsg", DEFAULT_NMSG)),
                    Integer.parseInt(cmd.getOptionValue("timeout1", DEFAULT_TIMEOUT1)),
                    Integer.parseInt(cmd.getOptionValue("timeout2", DEFAULT_TIMEOUT2)),
                    Integer.parseInt(cmd.getOptionValue("sync", "0")) > 0,
                    cmd.getOptionValue("logfile"),
                    cmd.getOptionValue("extra"));
        } catch (ParseException e) {
            System.err.println("Parsing failed: " + e.getMessage());
            new HelpFormatter().printHelp(cmdLineSyntax, opts, true);
            System.exit(1);
        }
        return null;
    }

    @Value
    static public class ConsumerOptions {
        public String service;
        public String brokers;
        public String topic_src;
        public int qos;
        public int nmsg;
        public int timeout1;
        public int timeout2;
        public boolean sync_mode;
        public String logfile;
        public String extra;
    }

    public static ConsumerOptions parseConsumerOptions(String cmdLineSyntax, String[] args) {
        Options opts = new Options();
        try {
            opts.addOption(Option.builder().hasArg().longOpt("service").build());
            opts.addOption(Option.builder().hasArg().longOpt("brokers").build());
            opts.addOption(Option.builder().required().hasArg().longOpt("topic_src").build());
            opts.addOption(Option.builder().hasArg().longOpt("qos").build());
            opts.addOption(Option.builder().hasArg().longOpt("nmsg").build());
            opts.addOption(Option.builder().hasArg().longOpt("timeout1").build());
            opts.addOption(Option.builder().hasArg().longOpt("timeout2").build());
            opts.addOption(Option.builder().hasArg().longOpt("sync").build());
            opts.addOption(Option.builder().required().hasArg().longOpt("logfile").build());
            opts.addOption(Option.builder().hasArg().longOpt("extra").build());

            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(opts, args);

            return new ConsumerOptions(
                    cmd.getOptionValue("service"),
                    cmd.getOptionValue("brokers"),
                    cmd.getOptionValue("topic_src"),
                    Integer.parseInt(cmd.getOptionValue("qos", DEFAULT_QOS)),
                    Integer.parseInt(cmd.getOptionValue("nmsg", DEFAULT_NMSG)),
                    Integer.parseInt(cmd.getOptionValue("timeout1", DEFAULT_TIMEOUT1)),
                    Integer.parseInt(cmd.getOptionValue("timeout2", DEFAULT_TIMEOUT2)),
                    Integer.parseInt(cmd.getOptionValue("sync", "0")) > 0,
                    cmd.getOptionValue("logfile"),
                    cmd.getOptionValue("extra"));
        } catch (ParseException e) {
            System.err.println("Parsing failed: " + e.getMessage());
            new HelpFormatter().printHelp(cmdLineSyntax, opts, true);
            System.exit(1);
        }
        return null;
    }

    static class DataList {
        private byte[][] datas;

        public DataList(int nmsg, int bytes) {
            this.datas = new byte[nmsg][bytes];
            Random rnd = new Random();
            for (int i = 0; i < nmsg; i++) {
                rnd.nextBytes(this.datas[i]);
            }
        }
        public byte[] peekData(int i) {
            return datas[i];
        }
        public byte[] getData(int i) {
            byte[] data = datas[i];
            long t = System.currentTimeMillis();
            putInt(data, 0, i);
            putLong(data, 4, t);
            return data;
        }
    }
    static class Metadata {
        private int i;
        private long t;

        public Metadata(byte[] buf) {
            this.i = getInt(buf, 0);
            this.t = getLong(buf, 4);
        }
        public String str() {
            return i + "," + t;
        }
    }
    static public Metadata extractMetadata(byte[] buf) {
        return new Metadata(buf);
    }

    static public void putInt(byte[] buf, int pos, int x) {
        for (int i = 0; i < 4; i++) {
            buf[pos + i] = (byte)(x & 0xff);
            x >>= 8;
        }
    }
    static public int getInt(byte[] buf, int pos) {
        int x = 0;
        for (int i = 0; i < 4; i++) {
            x |= (long)((byte)buf[pos + i] & 0xff) << (i * 8);
        }
        return x;
    }
    static public void putLong(byte[] buf, int pos, long x) {
        for (int i = 0; i < 8; i++) {
            buf[pos + i] = (byte)(x & 0xff);
            x >>= 8;
        }
    }
    static public long getLong(byte[] buf, int pos) {
        long x = 0;
        for (int i = 0; i < 8; i++) {
            x |= (long)((byte)buf[pos + i] & 0xff) << (i * 8);
        }
        return x;
    }

    @SuppressWarnings("unchecked")
    static public Object get_kafka_metric(Object metrics, String name, String group) {
        return get_kafka_metric((Map<MetricName,? extends Metric>)metrics, name, group);
    }
    static public Object get_kafka_metric(Map<MetricName,? extends Metric> metrics, String name, String group) {
        for (Map.Entry<MetricName,? extends Metric> entry : metrics.entrySet()) {
            MetricName metricName = entry.getKey();
            if ((name == null || metricName.name().equals(name)) &&
                (group == null || metricName.group().equals(group)))
                return entry.getValue().metricValue();
        }
        return null;
    }
}
