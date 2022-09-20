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

import org.eclipse.paho.client.mqttv3.*;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings({"WeakerAccess"})
public class MqttBinaryConsumer implements MqttCallback {

    private final Util.ConsumerOptions opts;

    private Lock lock;
    private Condition progress;
    private Long[] ts;
    private Integer[] ss;
    private int hdrlen;
    private byte[][] hdr;
    private int n;

    public MqttBinaryConsumer(Util.ConsumerOptions opts) {
        this.opts = opts;

        this.lock = new ReentrantLock();
        this.progress = lock.newCondition();
        this.ts = new Long[opts.nmsg];
        this.ss = new Integer[opts.nmsg];
        this.hdrlen = 4 + 8; // int+long
        this.hdr = new byte[opts.nmsg][hdrlen];
        this.n = 0;
    }

    public void run() throws Exception {
        String clientId = "perftest" + System.currentTimeMillis();
        MqttClient client = new MqttClient(opts.brokers, clientId);
        MqttConnectOptions conOpt = new MqttConnectOptions();
        conOpt.setCleanSession(true);
        //conOpt.setPassword(opts.password.toCharArray());
        //conOpt.setUserName(opts.userName);
        client.setCallback(this);
        client.connect(conOpt);

        client.subscribe(opts.topic_src, opts.qos);
        String error = null;
        long tstart = System.currentTimeMillis();
        this.lock.lock();
        while (this.n < opts.nmsg) {
            if (System.currentTimeMillis() - tstart > opts.timeout2) {
                error = "sub:timedout2";
                System.err.println(error);
                break;
            }
            boolean ok = this.progress.await(opts.timeout1, TimeUnit.MILLISECONDS);
            if (!ok) {
                error = "sub:timedout1";
                System.err.println(error);
                break;
            }
        }
        this.lock.unlock();
        client.disconnect();

        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(Paths.get(opts.logfile)))) {
            pw.println("#recv,size,seq,send");
            this.lock.lock();
            for (int i = 0; i < this.n; i++) {
                pw.println(this.ts[i] + "," + this.ss[i] + "," + Util.extractMetadata(this.hdr[i]).str());
            }
            this.lock.unlock();
            if (error != null)
                pw.println("# " + error);
        }
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
        System.arraycopy(msg.getPayload(), 0, this.hdr[n], 0, this.hdrlen);
        this.n++;
        if (opts.nmsg <= this.n) {
            System.err.println("done");
        }
        this.progress.signal();
        this.lock.unlock();
    }

    public static void main(String[] args) {
        try {
            MqttBinaryConsumer consumer = new MqttBinaryConsumer(Util.parseConsumerOptions("MqttBinaryConsumer", args));
            consumer.run();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
