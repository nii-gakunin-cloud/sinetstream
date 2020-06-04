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

package jp.ad.sinet.stream.example.image;

import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.api.MessageReader;
import jp.ad.sinet.stream.api.valuetype.ValueTypeFactory;
import jp.ad.sinet.stream.utils.MessageReaderFactory;
import org.apache.commons.cli.*;
import org.bytedeco.javacv.CanvasFrame;

import javax.swing.*;
import java.awt.image.BufferedImage;

@SuppressWarnings({"WeakerAccess", "CodeBlock2Expr"})
public class ImageConsumer {

    private final String service;
    private final CanvasFrame canvas;

    public ImageConsumer(String service) {
        this.service = service;
        this.canvas = new CanvasFrame(this.getClass().getSimpleName());
        this.canvas.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public void run() {
        MessageReaderFactory<BufferedImage> factory =
                MessageReaderFactory.<BufferedImage>builder()
                        .service(service)
                        .consistency(Consistency.AT_LEAST_ONCE)
                        .valueType(new ValueTypeFactory().get("image"))
                        .build();
        try (MessageReader<BufferedImage> reader = factory.getReader()) {
            reader.stream().forEach(msg -> {
                show(msg.getValue());
            });
        }
    }

    private void show(BufferedImage image) {
        canvas.setCanvasSize(image.getWidth(), image.getHeight());
        canvas.showImage(image);
    }

    public static void main(String[] args) {
        Options opts = new Options();
        opts.addOption(Option.builder("s").required().hasArg().longOpt("service").build());

        CommandLineParser parser = new DefaultParser();
        ImageConsumer producer = null;
        try {
            CommandLine cmd = parser.parse(opts, args);
            producer = new ImageConsumer(cmd.getOptionValue("service"));
        } catch (ParseException e) {
            System.err.println("Parsing failed: " + e.getMessage());
            new HelpFormatter().printHelp(ImageConsumer.class.getSimpleName(), opts, true);
            System.exit(1);
        }
        try {
            producer.run();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
