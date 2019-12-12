/*
 * Copyright (C) 2019 National Institute of Informatics
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.cli.*;

import java.time.Duration;
import java.util.Objects;

import jp.ad.sinet.stream.example.perf.Util;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
//import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"WeakerAccess", "CodeBlock2Expr"})
public class MqttBinaryConsumer implements MqttCallback {

    private final String brokers;
    private final String topic_src;
    private final int qos;
    private final int nmsg;
    private final int timeout;
    private final String logfile;

    private Lock lock;
    private Condition progress;
    private Long[] ts;
    private Integer[] ss;
    private int n;

    public MqttBinaryConsumer(String brokers, String topic_src, int qos, int nmsg, int timeout, String logfile) {
        this.brokers = brokers;
        this.topic_src = topic_src;
        this.qos = qos;
        this.nmsg = nmsg;
        this.timeout = timeout;
        this.logfile = logfile;
        this.lock = new ReentrantLock();
        this.progress = lock.newCondition();
        this.ts = new Long[this.nmsg];
        this.ss = new Integer[this.nmsg];
        this.n = 0;
    }

    public void run() throws Exception {
        String clientId = "perftest" + System.currentTimeMillis();
        MqttClient client = new MqttClient(this.brokers, clientId);
        MqttConnectOptions  conOpt = new MqttConnectOptions();
        conOpt.setCleanSession(true);
        //conOpt.setPassword(this.password.toCharArray());
        //conOpt.setUserName(this.userName);
        client.setCallback(this);
        client.connect(conOpt);
        
        client.subscribe(this.topic_src, this.qos);
        this.lock.lock();
        while (this.n < this.nmsg) {
            boolean ok = this.progress.await(timeout, TimeUnit.MILLISECONDS);
            if (!ok) {
                System.err.println("timedout");
                break;
            }
        }
        this.lock.unlock();
        client.disconnect();

        FileWriter file = new FileWriter(this.logfile);
        PrintWriter pw = new PrintWriter(new BufferedWriter(file));
        this.lock.lock();
        for (int i = 0; i < this.n; i++) {
            pw.println(this.ts[i] + "," + this.ss[i]);
        }
        this.lock.unlock();
        pw.close();
    }

    public void connectionLost(Throwable x) {
        System.err.println("connection lost");
        System.exit(1);
    }

    public void deliveryComplete(IMqttDeliveryToken token) {
    }

    public void messageArrived(String topic, MqttMessage msg) {
        //System.err.println("XXX: msg: n=" + this.n + " topic=" + topic + " len=" + msg.getPayload().length + " retained=" + msg.isRetained());
        if (msg.isRetained()) {
            System.err.println("discard retained msg: n=" + this.n + " topic=" + topic + " len=" + msg.getPayload().length + " retained=" + msg.isRetained());
            return;
        }
        long t = System.currentTimeMillis();
        this.lock.lock();
        if (this.n == 0) {
            System.err.println("receiving");
        }
        this.ts[this.n] = t;
        this.ss[this.n] = msg.getPayload().length;
        this.n += 1;
        if (this.nmsg <= this.n) {
            System.err.println("done");
        }
        this.progress.signal();
        this.lock.unlock();
    }

    public static void main(String[] args) {
        //String logfile = "mqttJava.consumed." + Util.getTime() + "." + Util.getHostName() + ".csv";
        Options opts = new Options();
        opts.addOption(Option.builder("b").required().hasArg().longOpt("brokers").build());
        opts.addOption(Option.builder("S").required().hasArg().longOpt("topic_src").build());
        opts.addOption(Option.builder("Q").required().hasArg().longOpt("qos").build());
        opts.addOption(Option.builder("N").hasArg().longOpt("nmsg").build());
        opts.addOption(Option.builder("T").hasArg().longOpt("timeout").build());
        opts.addOption(Option.builder("f").required().hasArg().longOpt("logfile").build());

        CommandLineParser parser = new DefaultParser();
        MqttBinaryConsumer consumer = null;
        try {
            CommandLine cmd = parser.parse(opts, args);
            consumer = new MqttBinaryConsumer(
                    cmd.getOptionValue("brokers"),
                    cmd.getOptionValue("topic_src"),
                    Integer.parseInt(cmd.getOptionValue("qos")),
                    Integer.parseInt(cmd.getOptionValue("nmsg")),
                    Integer.parseInt(cmd.getOptionValue("timeout")),
                    cmd.getOptionValue("logfile")
            );
        } catch (ParseException e) {
            System.err.println("Parsing failed: " + e.getMessage());
            new HelpFormatter().printHelp(MqttBinaryConsumer.class.getSimpleName(), opts, true);
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
