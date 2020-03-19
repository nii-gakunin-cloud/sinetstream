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

import jp.ad.sinet.stream.api.Deserializer;
import jp.ad.sinet.stream.api.SinetStreamIOException;
import lombok.EqualsAndHashCode;
import lombok.extern.java.Log;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;

@EqualsAndHashCode
@Log
public class ImageDeserializer implements Deserializer<BufferedImage> {

    @Override
    public BufferedImage deserialize(byte[] bytes) {
        if (Objects.isNull(bytes)) {
            return null;
        }
        return Optional.ofNullable(getImage(bytes)).orElseThrow(SinetStreamIOException::new);
    }

    private BufferedImage getImage(byte[] bytes) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
            return ImageIO.read(in);
        } catch (IOException e) {
            log.log(Level.WARNING, "Image decoding failed", e);
            throw new SinetStreamIOException(e);
        }
    }
}
