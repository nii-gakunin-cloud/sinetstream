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

package jp.ad.sinet.stream.marshal;

import jp.ad.sinet.stream.api.InvalidMessageException;
import jp.ad.sinet.stream.utils.Timestamped;
import lombok.extern.java.Log;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.message.BinaryMessageDecoder;

import java.io.IOException;
import java.nio.ByteBuffer;

@Log
public class Unmarshaller {

    private Schema schema;
    private BinaryMessageDecoder<GenericRecord> decoder;

    public Unmarshaller() {
        MessageSchema messageSchema = new MessageSchema();
        this.schema = messageSchema.getSchema();
        this.decoder = new BinaryMessageDecoder<>(new GenericData(), schema);
    }

    public Timestamped<byte[]> decode(byte[] bytes) {
        try {
            GenericRecord rec = decoder.decode(bytes);
            long tstamp = (long) rec.get("tstamp");
            byte[] data = ((ByteBuffer) rec.get("msg")).array();
            return new Timestamped<>(data, tstamp);
        }
        catch (IOException | /*BadHeaderException | MissingSchemaException |*/ AvroRuntimeException | IllegalArgumentException e) {
            log.warning(e.getMessage());
            throw new InvalidMessageException(e);
        }
    }
}