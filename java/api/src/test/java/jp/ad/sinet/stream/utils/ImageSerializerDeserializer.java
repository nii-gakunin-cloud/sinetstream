/*
 * Copyright (C) 2019 National Institute of Informatics
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

package jp.ad.sinet.stream.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

class ImageSerializerDeserializer {

    private BufferedImage image;

    @BeforeEach
    void setup() throws IOException {
        URL url = ImageSerializerDeserializer.class.getResource("/GakuNinCloud.png");
        this.image = ImageIO.read(url);
    }

    @Test
    void png() {
        ImageSerializer<RenderedImage> ser = new ImageSerializer<>();
        ImageDeserializer des = new ImageDeserializer();
        byte[] bytes = ser.serialize(this.image);
        assertTrue(bytes.length > 0);
        assertArrayEquals(bytes, ser.serialize(des.deserialize(bytes)));
    }

    @Test
    void bmp() {
        ImageSerializer<RenderedImage> ser = new ImageSerializer<>("bmp");
        ImageDeserializer des = new ImageDeserializer();
        byte[] bytes = ser.serialize(this.image);
        assertTrue(bytes.length > 0);
        assertArrayEquals(bytes, ser.serialize(des.deserialize(bytes)));
    }

    @Test
    void nullSerialize() {
        ImageSerializer<RenderedImage> ser = new ImageSerializer<>();
        assertNull(ser.serialize(null));
    }

    @Test
    void nullDeserialize() {
        ImageDeserializer des = new ImageDeserializer();
        assertNull(des.deserialize(null));
    }
}
