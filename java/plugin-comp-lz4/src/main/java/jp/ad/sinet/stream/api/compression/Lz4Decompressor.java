/*
 * Copyright (C) 2022 National Institute of Informatics
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

package jp.ad.sinet.stream.api.compression;

import jp.ad.sinet.stream.api.Decompressor;
import jp.ad.sinet.stream.api.SinetStreamIOException;
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

class Lz4Decompressor implements Decompressor {
    public Lz4Decompressor(Map<String, Object> parameters) {
    }
    public byte[] decompress(byte[] bytes) {
        int bufsz = 1000;
        ByteArrayInputStream decompIn = new ByteArrayInputStream(bytes);
        ByteArrayOutputStream data = new ByteArrayOutputStream(bufsz);
        try (InputStream decompOut = new FramedLZ4CompressorInputStream(decompIn)) {
            byte[] buf = new byte[bufsz];
            int n = 0;
            while ((n = decompOut.read(buf)) != -1) {
                data.write(buf, 0, n);
            }
        } catch (IOException e) {
            throw new SinetStreamIOException(e);
        }
        return data.toByteArray();
    }
}
