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

package jp.ad.sinet.stream.plugins.mqtt;

import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.api.SinetStreamIOException;
import jp.ad.sinet.stream.spi.PluginAsyncMessageWriter;
import jp.ad.sinet.stream.spi.WriterParameters;
import jp.ad.sinet.stream.utils.MessageUtils;
import lombok.Getter;
import lombok.extern.java.Log;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.jdeferred2.DeferredCallable;
import org.jdeferred2.DeferredManager;
import org.jdeferred2.Promise;
import org.jdeferred2.impl.DefaultDeferredManager;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

@Log
public class MqttAsyncMessageWriter extends MqttAsyncBaseIO implements PluginAsyncMessageWriter {

    @Getter
    private final String topic;
    private final DefaultDeferredManager manager;
    private final Semaphore sem;

    MqttAsyncMessageWriter(WriterParameters parameters) {
        super(parameters.getService(), parameters.getConsistency(), parameters.getClientId(), parameters.getConfig(),
                parameters.getValueType(), parameters.isDataEncryption());
        this.topic = parameters.getTopic();
        connect();
        this.manager = createDeferredManager();
        this.sem = new Semaphore(getPermitNum(), true);
    }

    private int getPermitNum() {
        if (Consistency.AT_MOST_ONCE.equals(consistency)) {
            return connectOptions.getMaxInflight();
        } else {
            int ret = connectOptions.getMaxInflight() / 2;
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
    public Promise<Void, Throwable, Void> write(byte[] message) {
        try {
            sem.acquire();
            log.finer(() -> "MQTT publish: " + getClientId() + ": " + Arrays.toString(message));
            IMqttDeliveryToken token = publish(message);
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

    private IMqttDeliveryToken publish(byte[] message) {
        try {
            synchronized (this) {
                return client.publish(topic, message, consistency.getQos(), retain);
            }
        } catch (MqttException e) {
            throw new SinetStreamIOException(e);
        }
    }
}