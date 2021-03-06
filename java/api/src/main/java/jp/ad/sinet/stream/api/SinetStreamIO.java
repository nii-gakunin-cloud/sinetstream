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

package jp.ad.sinet.stream.api;

import jp.ad.sinet.stream.spi.PluginMessageIO;
import jp.ad.sinet.stream.spi.SinetStreamParameters;
import lombok.Getter;
import lombok.extern.java.Log;
import org.apache.commons.beanutils.PropertyUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Log
public class SinetStreamIO<T extends PluginMessageIO> {

    @Getter
    private final boolean isDataEncryption;

    @Getter
    private final ValueType valueType;

    @Getter
    private final String service;

    @Getter
    private final Map<String, Object> config;

    protected final T target;

    private final AtomicBoolean closed = new AtomicBoolean(false);

    protected class IOMetrics {
        protected long startTimeMillis;
        protected long endTimeMillis;       // XXX This is most likely unnecessary.
        protected long msgCountTotal;
        protected long msgBytesTotal;
        protected long msgSizeMin;
        protected long msgSizeMax;
        protected long errorCountTotal;
        public synchronized String toString() {
            return "IOMetrics"
                + "{startTimeMillis=" + startTimeMillis
                + ",endTimeMillis="   + endTimeMillis
                + ",msgCountTotal="   + msgCountTotal
                + ",msgBytesTotal="   + msgBytesTotal
                + ",msgSizeMin="      + msgSizeMin
                + ",msgSizeMax="      + msgSizeMax
                + ",errorCountTotal=" + errorCountTotal
                + "}";
        }
        public IOMetrics() {
            resetMetrics();
        }
        public synchronized void updateMetrics(int len) {
            this.endTimeMillis = System.currentTimeMillis();
            this.msgCountTotal++;
            this.msgBytesTotal += len;
            this.msgSizeMin = Math.min(this.msgSizeMin, len);
            this.msgSizeMax = Math.max(this.msgSizeMax, len);
        }
        public synchronized void updateMetricsErr() {
            this.errorCountTotal++;
        }
        public synchronized void resetMetrics() {
            this.startTimeMillis = this.endTimeMillis = System.currentTimeMillis();
            this.msgCountTotal = 0;
            this.msgBytesTotal = 0;
            this.msgSizeMin = Long.MAX_VALUE;
            this.msgSizeMax = -1;
            this.errorCountTotal = 0;
        }
    }
    IOMetrics ioMetrics;
    protected void updateMetrics(int len) { ioMetrics.updateMetrics(len); }
    protected void updateMetricsErr() { ioMetrics.updateMetricsErr(); }
    public Metrics getMetrics() {
        Metrics metrics = new Metrics();
        metrics.setStartTimeMillis(this.ioMetrics.startTimeMillis);
        metrics.setEndTimeMillis(this.ioMetrics.endTimeMillis);
        metrics.setMsgCountTotal(this.ioMetrics.msgCountTotal);
        metrics.setMsgBytesTotal(this.ioMetrics.msgBytesTotal);
        metrics.setMsgSizeMin(this.ioMetrics.msgSizeMin != Long.MAX_VALUE ? this.ioMetrics.msgSizeMin : -1);
        metrics.setMsgSizeMax(this.ioMetrics.msgSizeMax);
        metrics.setErrorCountTotal(this.ioMetrics.errorCountTotal);
        metrics.setRaw(closed.get() ? null : target.getMetrics());
        return metrics;
    }
    public void resetMetrics(boolean reset_raw) {
        this.ioMetrics.resetMetrics();
        if (reset_raw && !closed.get())
            target.resetMetrics();
    }

    public SinetStreamIO(SinetStreamParameters parameters, T target) {
        this.target = target;
        this.config = new PluginWrapperMap(parameters.getConfig());
        this.service = parameters.getService();
        this.isDataEncryption = parameters.isDataEncryption();
        this.valueType = parameters.getValueType();
        this.ioMetrics = new IOMetrics();
    }

    public String getClientId() {
        return target.getClientId();
    }

    public Consistency getConsistency() {
        return target.getConsistency();
    }

    public void close() {
        if (!closed.getAndSet(true)) {
            target.close();
        }
    }

    @SuppressWarnings("NullableProblems")
    private class PluginWrapperMap implements Map<String, Object> {
        private final Map<String, Object> m;
        private Set<String> propertyKeys;
        private transient Set<String> keySet;

        PluginWrapperMap(Map<String, Object> m) {
            if (Objects.isNull(m)) {
                throw new NullPointerException();
            }
            this.m = m;
            this.propertyKeys = findPropertyKeys();
        }

        private Set<String> findPropertyKeys() {
            try {
                Map<String, Object> props = PropertyUtils.describe(target);
                return props.keySet();
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                return Collections.emptySet();
            }
        }

        @Override
        public int size() {
            return m.size();
        }

        @Override
        public boolean isEmpty() {
            return m.isEmpty();
        }

        @SuppressWarnings("SuspiciousMethodCalls")
        @Override
        public boolean containsKey(Object key) {
            return this.m.containsKey(key) || this.propertyKeys.contains(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return m.containsValue(value);
        }

        @Override
        public Object get(Object key) {
            if (Objects.isNull(key)) {
                throw new NullPointerException();
            }
            //noinspection SuspiciousMethodCalls
            if (this.propertyKeys.contains(key)) {
                try {
                    return PropertyUtils.getSimpleProperty(target, (String) key);
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    log.fine(e::getMessage);
                }
            }
            return m.get(key);
        }

        @Override
        public Object put(String key, Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object remove(Object key) {
            throw new UnsupportedOperationException();
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public void putAll(Map<? extends String, ?> m) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public Set<String> keySet() {
            if (Objects.isNull(this.keySet)) {
                Set<String> keys = new HashSet<>(this.m.keySet());
                keys.addAll(this.propertyKeys);
                this.keySet = Collections.unmodifiableSet(keys);
            }
            return this.keySet;
        }

        @Override
        public Collection<Object> values() {
            Collection<Object> values = new ArrayList<>(this.m.values());
            try {
                Map<String, Object> props = PropertyUtils.describe(target);
                values.addAll(props.values());
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                log.fine(e::getMessage);
            }
            return values;
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public Set<Entry<String, Object>> entrySet() {
            Set<Entry<String, Object>> entrySet = new HashSet<>(this.m.entrySet());
            try {
                Map<String, Object> props = PropertyUtils.describe(target);
                entrySet.addAll(props.entrySet());
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                log.fine(e::getMessage);
            }
            return entrySet;
        }
    }
}
