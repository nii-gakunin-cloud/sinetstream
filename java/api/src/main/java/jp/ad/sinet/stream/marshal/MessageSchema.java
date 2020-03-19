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

import jp.ad.sinet.stream.api.SinetStreamException;
import jp.ad.sinet.stream.api.SinetStreamIOException;
import lombok.extern.java.Log;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Parser;
import org.apache.avro.SchemaNormalization;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

@Log
public class MessageSchema {

    public Schema getSchema() {
        ClassLoader classLoader = getClass().getClassLoader();
        try (InputStream in = classLoader.getResourceAsStream("messageSchema.avsc")) {
            return new Parser().parse(in);
        } catch (IOException e) {
            throw new SinetStreamIOException(e);
        }
    }

    public byte[] getFingerprint(Schema schema) {
        try {
            return SchemaNormalization.parsingFingerprint("CRC-64-AVRO", schema);
        }
        catch (NoSuchAlgorithmException e) {
            throw new SinetStreamException(e);
        }
    }
}
