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
import com.github.luben.zstd.ZstdCompressCtx;
import jp.ad.sinet.stream.api.Compressor;
import jp.ad.sinet.stream.api.SinetStreamIOException;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

class ZstdCompressor implements Compressor {
    int imp;
    ZstdCompressCtx ctx;
    Integer level;

    public ZstdCompressor(Integer level, Map<String, Object> parameters) {
        this.ctx = new ZstdCompressCtx();
        this.level = level;
        this.imp = (int)parameters.getOrDefault("_zstd_imp", 0);
    }

    // using apache commons-compress
    public byte[] compress0(byte[] data) {
        ByteArrayOutputStream compOut = new ByteArrayOutputStream();
        try (OutputStream compIn = (level != null ? new ZstdCompressorOutputStream(compOut, level)
                                                  : new ZstdCompressorOutputStream(compOut))) {
            compIn.write(data);
        } catch (IOException e) {
            throw new SinetStreamIOException(e);
        }
        return compOut.toByteArray();
    }

    // using zstd-jni
    public byte[] compress1(byte[] data) {
        return level != null ? Zstd.compress(data, level) : Zstd.compress(data);
    }

    // using zstd-jni ZstdCompressCtx
    public byte[] compress2(byte[] data) {
        ctx.reset();
        if (level != null)
            ctx.setLevel(level);
        return ctx.compress(data);
    }

    public byte[] compress(byte[] data) {
        switch (imp/10) {
        case 0: return compress0(data);
        case 1: return compress1(data);
        case 2: return compress2(data);
        default: return null;
        }
    }
}
