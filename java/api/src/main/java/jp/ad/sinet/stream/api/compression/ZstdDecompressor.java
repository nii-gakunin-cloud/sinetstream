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

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdDecompressCtx;
import jp.ad.sinet.stream.api.Decompressor;
import jp.ad.sinet.stream.api.SinetStreamIOException;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

class ZstdDecompressor implements Decompressor {
    int imp;
    ZstdDecompressCtx ctx;
    public ZstdDecompressor(Map<String, Object> parameters) {
        this.ctx = new ZstdDecompressCtx();
        this.imp = (int)parameters.getOrDefault("_zstd_imp", 2);
    }
    private byte[] decompress0(byte[] bytes) {
        int bufsz = 1000;
        ByteArrayInputStream decompIn = new ByteArrayInputStream(bytes);
        ByteArrayOutputStream data = new ByteArrayOutputStream(bufsz);
        try (InputStream decompOut = new ZstdCompressorInputStream(decompIn)) {
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
    private byte[] decompress1(byte[] bytes) {
        int origSz = (int) Zstd.decompressedSize(bytes);
        return origSz >= 0 ? Zstd.decompress(bytes, origSz) : decompress0(bytes);
    }
    private byte[] decompress2(byte[] bytes) {
        int origSz = (int) Zstd.decompressedSize(bytes);
        ctx.reset();
        return origSz >= 0 ? ctx.decompress(bytes, origSz) : decompress0(bytes);
    }
    public byte[] decompress(byte[] bytes) {
        switch (imp%10) {
        case 0: return decompress0(bytes);
        case 1: return decompress1(bytes);
        case 2: return decompress2(bytes);
        default: return null;
        }
    }
}
