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

package jp.ad.sinet.stream.api.valuetype;

import jp.ad.sinet.stream.api.Serializer;
import jp.ad.sinet.stream.api.SinetStreamIOException;
import lombok.EqualsAndHashCode;
import lombok.extern.java.Log;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;

@EqualsAndHashCode
@Log
public class ImageSerializer<T extends RenderedImage> implements Serializer<T> {

    private final String formatName;

    public ImageSerializer(String formatName) {
        this.formatName = formatName;
    }

    public ImageSerializer() {
        this("png");
    }

    @Override
    public byte[] serialize(T data) {
        if (Objects.isNull(data)) {
            return null;
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            boolean ret = ImageIO.write(data, this.formatName, out);
            if (ret) {
                return out.toByteArray();
            } else {
                throw new SinetStreamIOException("no appropriate writer is found");
            }
        } catch (IOException e) {
            log.log(Level.WARNING, "Image encoding failed", e);
            throw new SinetStreamIOException(e);
        }
    }
}