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
import jp.ad.sinet.stream.crypto.CryptoSerializerWrapper;
import jp.ad.sinet.stream.marshal.Marshaller;
import jp.ad.sinet.stream.packet.Packet;
import jp.ad.sinet.stream.spi.PluginMessageIO;
import jp.ad.sinet.stream.spi.WriterParameters;
import jp.ad.sinet.stream.utils.CtxSerializer;
import jp.ad.sinet.stream.utils.Pair;
import jp.ad.sinet.stream.utils.Timestamped;

import lombok.Getter;

import java.util.Map;
import java.util.Objects;

public class SinetStreamBaseWriter<T, U extends PluginMessageIO> extends SinetStreamIO<U> {

    @Getter
    private final String topic;

    @Getter
    private final Serializer<T> serializer;

    @Getter
    private final Compressor compressor;

    @Getter
    private final Serializer<Timestamped<T>> compositeSerializer;

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

    private class PacketSerializer<T> implements Serializer<T> {
        private byte formatVersion;
        private CtxSerializer<T, Integer> ctxser;

        PacketSerializer(byte formatVersion, CtxSerializer<T, Integer> ctxser) {
            this.formatVersion = formatVersion;
            this.ctxser = ctxser;
        }

        public byte[] serialize(T data) {
            Pair<byte[], Integer> serialized = ctxser.serialize(data);
            return Packet.encode(formatVersion, serialized.getV2(), serialized.getV1());
        }
    }

    public SinetStreamBaseWriter(U pluginWriter, WriterParameters parameters, Serializer<T> serializer) {
        super(parameters, pluginWriter);
        this.topic = parameters.getTopic();
        this.serializer = setupSerializer(parameters, serializer);
        this.compressor = setupCompressor(parameters);
        this.compositeSerializer = generateSerializer(parameters);
    }

    @SuppressWarnings("unchecked")
    private Serializer<T> setupSerializer(WriterParameters parameters, Serializer<T> serializer) {
        if (Objects.isNull(serializer)) {
            return parameters.getValueType().getSerializer();
        }
        return serializer;
    }

    @SuppressWarnings("unchecked")
    private Compressor setupCompressor(WriterParameters parameters) {
        if (parameters.isDataCompression()) {
            Object compression = parameters.getConfig().get("compression");
            try {
                return CompressionFactory.createCompressor((Map<String, Object>) compression);
            }
            catch (ClassCastException e) {
                throw new InvalidConfigurationException("the parameter compression must be map", e);
            }
        } else
            return (data) -> data;
    }

    private Serializer<Timestamped<T>> generateSerializer(WriterParameters parameters) {
        if (this.isUserDataOnly())
            return (data) -> this.serializer.serialize(data.getValue());

        final Marshaller marshaller = new Marshaller();
        Serializer<Timestamped<T>> tsser = (data) -> {
            byte[] bytes = this.serializer.serialize(data.getValue());
            byte[] comped = this.compressor.compress(bytes);
            compressionMetrics.get().set(comped.length, bytes.length);
            return marshaller.encode(data.getTstamp(), comped);
        };
        CtxSerializer<Timestamped<T>, Integer> ctxser = CryptoSerializerWrapper.getSerializer(parameters.getConfig(), tsser);
        int messageFormat = this.getMessageFormat();
        switch (messageFormat) {
        case 2:
            return CtxSerializer.getSerializer(ctxser);
        case 3:
            return new PacketSerializer<Timestamped<T>>((byte)messageFormat, ctxser);
        default:
            assert(messageFormat == 2 || messageFormat == 3);
            throw new SinetStreamException("INTERNAL ERROR: messageFormat=" + messageFormat);
        }
    }

    byte[] toPayload(T message) {
        byte[] payload = getCompositeSerializer().serialize(new Timestamped<>(message));
        CompressionMetrics cm = compressionMetrics.get();
        updateMetrics(payload.length, cm.compLen, cm.uncompLen);
        return payload;
    }

    byte[] toPayload(T message, long tstamp) {
        byte[] payload = getCompositeSerializer().serialize(new Timestamped<>(message, tstamp));
        CompressionMetrics cm = compressionMetrics.get();
        updateMetrics(payload.length, cm.compLen, cm.uncompLen);
        return payload;
    }

    public void debugDisconnectForcibly() throws Exception {
        target.debugDisconnectForcibly();
    }
}
