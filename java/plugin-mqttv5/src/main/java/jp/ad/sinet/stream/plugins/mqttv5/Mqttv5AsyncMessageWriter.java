/*
 * Copyright (C) 2023 National Institute of Informatics
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

package jp.ad.sinet.stream.plugins.mqttv5;

import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.api.SinetStreamIOException;
import jp.ad.sinet.stream.spi.PluginAsyncMessageWriter;
import jp.ad.sinet.stream.spi.WriterParameters;
import jp.ad.sinet.stream.utils.MessageUtils;
import jp.ad.sinet.stream.utils.Timestamped;

import lombok.Getter;
import lombok.extern.java.Log;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.jdeferred2.DeferredCallable;
import org.jdeferred2.DeferredManager;
import org.jdeferred2.Promise;
import org.jdeferred2.impl.DefaultDeferredManager;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

@Log
public class Mqttv5AsyncMessageWriter extends Mqttv5AsyncBaseIO implements PluginAsyncMessageWriter {

    @Getter
    private final String topic;
    private final DefaultDeferredManager manager;
    private final Semaphore sem;

    Mqttv5AsyncMessageWriter(WriterParameters parameters) {
        super(parameters.getService(), parameters.getConsistency(), parameters.getClientId(), parameters.getConfig(),
                parameters.getValueType(), parameters.isDataEncryption());
        this.topic = parameters.getTopic();
        connect();
        this.manager = createDeferredManager();
        this.sem = new Semaphore(getPermitNum(), true);
    }

    private int getPermitNum() {
        Integer receiveMaximum = connectionOptions.getReceiveMaximum();
        if (receiveMaximum == null)
            receiveMaximum = 65535;  // XXX defined in class MqttConnectionState in org.eclipse.paho.mqttv5.client/src/main/java/org/eclipse/paho/mqttv5/client/internal/MqttConnectionState.java
        if (Consistency.AT_MOST_ONCE.equals(consistency)) {
            return receiveMaximum;
        } else {
            int ret = receiveMaximum / 2;
            return ret > 0 ? ret : 1;
        }
    }

    private DefaultDeferredManager createDeferredManager() {
        int nthreads = MessageUtils.toInteger(getConfig().getOrDefault("thread_pool_num", 4));
        return new DefaultDeferredManager(Executors.newFixedThreadPool(nthreads));
    }

    @Override
    protected void doClose() {
        super.doClose();
        manager.shutdown();
    }

    @Override
    public Promise<Void, Throwable, Void> write(Timestamped<byte[]> message) {
        try {
            sem.acquire();
            log.finer(() -> "MQTT publish: " + getClientId() + ": " + Arrays.toString(message.getValue()));
            IMqttToken token = publish(message.getValue());
            return manager.when(new DeferredCallable<Void, Void>(DeferredManager.StartPolicy.AUTO) {
                @Override
                public Void call() {
                    try {
                        token.waitForCompletion();
                        return null;
                    } catch (Throwable e) {
                        throw new SinetStreamIOException(e);
                    } finally {
                        sem.release();
                    }
                }
            });
        } catch (Throwable e) {
            sem.release();
            return manager.reject(e);
        }
    }

    private IMqttToken publish(byte[] message) {
        try {
            synchronized (this) {
                return client.publish(topic, message, consistency.getQos(), retain);
            }
        } catch (MqttException e) {
            throw new SinetStreamIOException(e);
        }
    }
}
