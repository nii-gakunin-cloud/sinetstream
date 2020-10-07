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
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;


import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Semaphore;

import java.net.Socket;
import javax.net.SocketFactory;
import java.net.InetAddress;
import java.io.IOException;

@SuppressWarnings("WeakerAccess")
public class MqttBinaryProducer {

    private final Util.ProducerOptions opts;

    public MqttBinaryProducer(Util.ProducerOptions opts) {
        this.opts = opts;
    }

    private int getPermitNum(MqttConnectOptions conOpt) {
        if (this.opts.qos == 0) {
            return conOpt.getMaxInflight();
        } else {
            int ret = conOpt.getMaxInflight() / 2;
            return ret > 0 ? ret : 1;
        }
    }

    public void run() throws Exception {
        int interval; // [ns]
        if (opts.fps > 0) {
            interval = 1000000000 / opts.fps;
            if (interval == 0) {
                System.err.println("fps " + opts.fps + " is too short");
            }
        } else {
            interval = 0;
        }

        boolean fullstrength_mode = false;
        boolean throttling_mode = false;
        boolean nodelay_mode = false;
        if (opts.extra != null) {
            fullstrength_mode = opts.extra.contains("fullstrength");
            throttling_mode = opts.extra.contains("throttling");
            nodelay_mode = opts.extra.contains("nodelay");
        }
        System.err.println("extra:"
            + " fullstrength=" + fullstrength_mode
            + " throttling=" + throttling_mode
            + " nodelay=" + nodelay_mode);

        String clientId = "perftest" + System.currentTimeMillis();
        MqttAsyncClient client = new MqttAsyncClient(opts.brokers, clientId);
        MqttConnectOptions conOpt = new MqttConnectOptions();
        if (nodelay_mode) {
            class NoDelaySocketFactory extends SocketFactory {
                SocketFactory base;
                public NoDelaySocketFactory(SocketFactory base) {
                    this.base = base;
                }
                private Socket setopt(Socket so) throws IOException {
                    so.setTcpNoDelay(true);
                    return so;
                }
                public Socket createSocket() throws IOException {
                    return setopt(base.createSocket());
                }
                public Socket createSocket(InetAddress host, int port) throws IOException {
                    return setopt(base.createSocket(host, port));
                }
                public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
                    return setopt(base.createSocket(address, port, localAddress, localPort));
                }
                public Socket createSocket(String host, int port) throws IOException {
                    return setopt(base.createSocket(host, port));
                }
                public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
                    return setopt(base.createSocket(host, port, localHost, localPort));
                }
            }
            conOpt.setSocketFactory(new NoDelaySocketFactory(SocketFactory.getDefault()));
        }
        conOpt.setCleanSession(true);
        //conOpt.setPassword(opts.password.toCharArray());
        //conOpt.setUserName(opts.userName);
        client.connect(conOpt).waitForCompletion();

        Semaphore sem = null;
        if (throttling_mode)
            sem = new Semaphore(getPermitNum(conOpt), true);
        CountDownLatch done = new CountDownLatch(opts.nmsg);
        AtomicInteger errcnt = new AtomicInteger();
        long[] ts = new long[opts.nmsg];
        int[] spins = new int[opts.nmsg];
        Util.DataList dataList = new Util.DataList(opts.nmsg, opts.bytes);
        long tstart = System.currentTimeMillis();
        long next = System.nanoTime() + interval;
        int n = 0;
        String error = null;
        for (int i = 0; i < opts.nmsg; i++) {
            if (System.currentTimeMillis() - tstart > opts.timeout2) {
                error = "pub:timedout2";
                System.err.println(error);
                break;
            }
            MqttMessage msg = new MqttMessage(dataList.getData(i));
            msg.setQos(opts.qos);
            if (opts.sync_mode) {
                try {
                    IMqttDeliveryToken token = client.publish(opts.topic_dst, msg);
                    token.waitForCompletion(opts.timeout1);
                }
                catch (MqttException e) {
                    error = "pub:timeout1";
                    break;
                }
                long t = System.currentTimeMillis();
                ts[n] = t;
            } else {
                class PubComp implements IMqttActionListener {
                    final private int nn;
                    private Semaphore sem;
                    PubComp(int nn, Semaphore sem) {
                        this.nn = nn;
                        this.sem = sem;
                    }
                    public void onFailure(IMqttToken asyncActionToken, java.lang.Throwable exception) {
                        long t = System.currentTimeMillis();
                        ts[nn] = -t;
                        errcnt.getAndIncrement();
                        done.countDown();
                        if (sem != null)
                            sem.release();
                    }
                    public void onSuccess(IMqttToken asyncActionToken) {
                        long t = System.currentTimeMillis();
                        ts[nn] = t;
                        done.countDown();
                        if (sem != null)
                            sem.release();
                    }
                }
                if (fullstrength_mode) {
                    long deadline = System.currentTimeMillis() + opts.timeout1;
                    int spin = 0;
                    while (true) {
                        try {
                            IMqttDeliveryToken token = client.publish(opts.topic_dst, msg, null, new PubComp(n, null));
                            break;
                        }
                        catch (MqttException ex) {
                            long now = System.currentTimeMillis();
                            if (deadline < now) {
                                error = "pub:timedout1";
                                System.err.println(error);
                                break;
                            }
                        }
                        spin++;
                    }
                    spins[n] = spin;
                    if (error != null)
                        break;
                } else {
                    if (sem != null)
                        sem.acquire();
                    try {
                        IMqttDeliveryToken token = client.publish(opts.topic_dst, msg, null, new PubComp(n, sem));
                    }
                    catch (MqttException ex) {
                        long t = System.currentTimeMillis();
                        ts[n] = -t;
                        errcnt.getAndIncrement();
                        done.countDown();
                    }
                }
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
        if (!opts.sync_mode && error == null) {
            for (int i = 0; i < opts.nmsg - n; i++)
                done.countDown();
            boolean ok = done.await(opts.timeout2, TimeUnit.MILLISECONDS);
            if (!ok) {
                error = "pub:timeout2";
                System.err.println(error);
            }
        }
        client.disconnect();

        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(Paths.get(opts.logfile)))) {
            pw.println("#comp,size,seq,send");
            for (int i = 0; i < n; i++) {
                long t = ts[i];
                int bytes = opts.bytes;
                if (t < 0) {
                    t = -t;
                    bytes = 0;
                }
                String x = "";
                if (fullstrength_mode) {
                    x = "," + spins[i];
                }
                pw.println(t + "," + bytes + "," + Util.extractMetadata(dataList.peekData(i)).str() + x);
            }
            pw.println("#errcnt " + errcnt.get());
            if (error != null)
                pw.println("# " + error);
        }
    }

    public static void main(String[] args) {
        try {
            MqttBinaryProducer producer = new MqttBinaryProducer(Util.parseProducerOptions("MqttBinaryProducer", args));
            producer.run();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
