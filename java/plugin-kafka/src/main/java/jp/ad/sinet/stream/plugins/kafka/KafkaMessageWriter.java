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

package jp.ad.sinet.stream.plugins.kafka;

import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.spi.PluginMessageWriter;
import jp.ad.sinet.stream.spi.WriterParameters;
import lombok.extern.java.Log;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.logging.Level;

@Log
public class KafkaMessageWriter extends KafkaBaseWriter implements PluginMessageWriter {

    KafkaMessageWriter(WriterParameters parameters) {
        super(parameters);
    }

    public void write(byte[] message) {
        ProducerRecord<String, byte[]> record = new ProducerRecord<>(topic, message);
        sendRecord(record);
    }

    private void sendRecord(ProducerRecord<String, byte[]> record) {
        log.finer(() -> "KAFKA send: " + getClientId() + ": " + record.toString());
        if (getConsistency().equals(Consistency.EXACTLY_ONCE)) {
            transactionalSendRecord(record);
        } else {
            try {
                this.producer.send(record).get();
            } catch (Throwable e) {
                log.log(Level.FINER, "send", e);
                throw wrapSinetStreamException(e);
            }
        }
    }

    private void transactionalSendRecord(ProducerRecord<String, byte[]> record) {
        beginTransaction();
        try {
            producer.send(record);
            commitTransaction();
        } catch (Throwable e) {
            abortTransaction();
            throw wrapSinetStreamException(e);
        }
    }

    public boolean isThreadSafe() {
        return ! Consistency.EXACTLY_ONCE.equals(getConsistency());
    }
}
