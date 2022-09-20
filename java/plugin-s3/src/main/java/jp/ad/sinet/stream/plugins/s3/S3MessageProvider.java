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

package jp.ad.sinet.stream.plugins.s3;

import jp.ad.sinet.stream.spi.*;
import lombok.extern.java.Log;

@Log
public class S3MessageProvider implements MessageWriterProvider, MessageReaderProvider /* , AsyncMessageWriterProvider, AsyncMessageReaderProvider */ {

    @Override
    public PluginMessageWriter getWriter(WriterParameters params) {
        log.fine(() -> "S3 getWriter: " + params.toString());
        return new S3MessageWriter(params);
    }

    @Override
    public PluginMessageReader getReader(ReaderParameters params) {
        log.fine(() -> "S3 getReader: " + params.toString());
        return new S3MessageReader(params);
    }

    /* NOT IMPLEMENTED
    @Override
    public PluginAsyncMessageWriter getAsyncWriter(WriterParameters params) {
        log.fine(() -> "S3 getAsyncWriter: " + params.toString());
        return new S3AsyncMessageWriter(params);
    }

    @Override
    public PluginAsyncMessageReader getAsyncReader(ReaderParameters params) {
        log.fine(() -> "S3 getAsyncReader: " + params.toString());
        return new S3AsyncMessageReader(params);
    }
    */

    @Override
    public String getType() {
        return "s3";
    }
}
