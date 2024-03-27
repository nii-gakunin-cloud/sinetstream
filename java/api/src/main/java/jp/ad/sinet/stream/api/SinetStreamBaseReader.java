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

import jp.ad.sinet.stream.api.compression.CompressionFactory;
import jp.ad.sinet.stream.crypto.CryptoDeserializerWrapper;
import jp.ad.sinet.stream.marshal.Unmarshaller;
import jp.ad.sinet.stream.packet.Packet;
import jp.ad.sinet.stream.spi.PluginMessageIO;
import jp.ad.sinet.stream.spi.PluginMessageWrapper;
import jp.ad.sinet.stream.spi.ReaderParameters;
import jp.ad.sinet.stream.utils.Pair;
import jp.ad.sinet.stream.utils.Timestamped;

import lombok.Getter;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class SinetStreamBaseReader<T, U extends PluginMessageIO> extends SinetStreamIO<U> {

    @Getter
    private final List<String> topics;

    @Getter
    private final Duration receiveTimeout;

    @Getter
    private final Deserializer<T> deserializer;

    @Getter
    private final Decompressor decompressor;

    @Getter
    private final Deserializer<Timestamped<T>> compositeDeserializer;

    static private class CompressionMetrics {
        public int compLen;
        public int uncompLen;
        void set(int compLen, int uncompLen) {
            this.compLen = compLen;
            this.uncompLen = uncompLen;
        }
    }
    private static ThreadLocal<CompressionMetrics> compressionMetrics = new ThreadLocal<CompressionMetrics>() {
        @Override
        protected CompressionMetrics initialValue() {
            return new CompressionMetrics();
        }
    };
    // XXX we assume no nested SINETStream (aka broker is sinetstream)

    public SinetStreamBaseReader(U pluginReader, ReaderParameters parameters, Deserializer<T> deserializer) {
        super(parameters, pluginReader);
        this.topics = Collections.unmodifiableList(parameters.getTopics());
        this.receiveTimeout = parameters.getReceiveTimeout();
        this.deserializer = setupDeserializer(parameters, deserializer);
        this.decompressor = setupDecompressor(parameters);
        this.compositeDeserializer = generateDeserializer(parameters);
    }

    @SuppressWarnings("unchecked")
    private Deserializer<T> setupDeserializer(ReaderParameters parameters, Deserializer<T> deserializer) {
        if (Objects.isNull(deserializer)) {
            return parameters.getValueType().getDeserializer();
        }
        return deserializer;
    }

    class UserDataOnlyDeserializer<T> implements Deserializer<Timestamped<T>> {
        private Deserializer<T> deserializer;
        public UserDataOnlyDeserializer(Deserializer<T> deserializer) {
            this.deserializer = deserializer;
        }
        public Timestamped<T> deserialize(byte[] bytes) {
            return new Timestamped<T>(this.deserializer.deserialize(bytes), 0);
        }
    }

    @SuppressWarnings("unchecked")
    private Decompressor setupDecompressor(ReaderParameters parameters) {
        if (parameters.isDataCompression()) {
            Object compression = parameters.getConfig().get("compression");
            try {
                return CompressionFactory.createDecompressor((Map<String, Object>) compression);
            }
            catch (ClassCastException e) {
                throw new InvalidConfigurationException("the parameter compression must be map", e);
            }
        } else
            return (bytes) -> bytes;
    }

    static public class PktDeserializer implements Deserializer<Pair<byte[], Integer>> {
        public Pair<byte[], Integer> deserialize(byte[] bytes) {
            Packet pkt = Packet.decode(bytes);
            if (pkt != null) {
                Packet.Header hdr = pkt.getHeader();
                Integer keyVer = (int)hdr.getKeyVersion();
                return Pair.of(pkt.getMessage(), keyVer);
            }
            // not v3
            return Pair.of(bytes, null);
        }
    }

    static public class ThruDeserializer implements Deserializer<Pair<byte[], Integer>> {
        public Pair<byte[], Integer> deserialize(byte[] bytes) {
            return Pair.of(bytes, null);
        }
    }

    private Deserializer<Timestamped<T>> generateDeserializer(ReaderParameters parameters) {
        if (this.isUserDataOnly())
            return new UserDataOnlyDeserializer<T>(this.deserializer);

        int messageFormat = getMessageFormat();
        Deserializer<Pair<byte[], Integer>> pktdes;
        switch (messageFormat) {
        case 2:
            pktdes = new ThruDeserializer();
            break;
        case 3:
            pktdes = new PktDeserializer();
            break;
        default:
            throw new InvalidConfigurationException("message_format=" + messageFormat + " is not supported");
        }

        final Unmarshaller unmashaller = new Unmarshaller();
        Deserializer<Timestamped<T>> tsdes = (bytes) -> {
            Timestamped<byte[]> data = unmashaller.decode(bytes);
            byte[] decomped = decompressor.decompress(data.getValue());
            compressionMetrics.get().set(data.getValue().length, decomped.length);
            T value = this.deserializer.deserialize(decomped);
            return new Timestamped<>(value, data.getTstamp());
        };

        return CryptoDeserializerWrapper.getDeserializer(parameters.getConfig(), pktdes, tsdes);
    }

    protected Message<T> toMessage(PluginMessageWrapper pluginMessage) {
        if (Objects.isNull(pluginMessage)) {
            return null;
        }
        Timestamped<byte[]> tvalue = pluginMessage.getValue();
        byte[] payload = tvalue.getValue();
        Timestamped<T> tsRecord = getCompositeDeserializer().deserialize(payload);
        CompressionMetrics cm = compressionMetrics.get();
        updateMetrics(payload.length, cm.compLen, cm.uncompLen);
        long tstamp = tsRecord.getTstamp();
        if (tstamp == 0)
            tstamp = tvalue.getTstamp();
        return new Message<>(tsRecord.getValue(), pluginMessage.getTopic(), tstamp, pluginMessage.getRaw());
    }

    public String getTopic() {
        return String.join(",", topics);
    }

    public void debugDisconnectForcibly() throws Exception {
        System.err.println("XXX: SinetStreamBaseReader: target=" + target);
        target.debugDisconnectForcibly();
    }
}
