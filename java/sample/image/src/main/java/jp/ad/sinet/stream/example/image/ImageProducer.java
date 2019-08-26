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

package jp.ad.sinet.stream.example.image;

import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.api.MessageWriter;
import jp.ad.sinet.stream.api.ValueType;
import jp.ad.sinet.stream.utils.MessageWriterFactory;
import org.apache.commons.cli.*;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

@SuppressWarnings("WeakerAccess")
public class ImageProducer {

    private final String service;
    private final String topic;
    private final Path movie;

    public ImageProducer(String service, String topic, Path movie) {
        this.service = service;
        this.topic = topic;
        this.movie = movie;
    }


    public void run() throws Exception {
        Java2DFrameConverter converter = new Java2DFrameConverter();
        MessageWriterFactory<BufferedImage> factory =
                MessageWriterFactory.<BufferedImage>builder()
                        .service(service)
                        .topic(topic)
                        .consistency(Consistency.AT_LEAST_ONCE)
                        .valueType(ValueType.IMAGE)
                        .build();
        try(MessageWriter<BufferedImage> writer = factory.getWriter();
            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(movie.toFile())) {
            Frame frame;
            grabber.start();
            while (Objects.nonNull(frame = grabber.grab())) {
                BufferedImage image = converter.convert(frame);
                writer.write(image);
            }
            grabber.stop();
            grabber.release();
        }
    }

    public static void main(String[] args) {
        Options opts = new Options();
        opts.addOption(Option.builder("s").required().hasArg().longOpt("service").build());
        opts.addOption(Option.builder("t").required().hasArg().longOpt("topic").build());
        opts.addOption(Option.builder("f").required().hasArg().longOpt("input-video").build());

        CommandLineParser parser = new DefaultParser();
        ImageProducer producer = null;
        try {
            CommandLine cmd = parser.parse(opts, args);
            producer = new ImageProducer(
                    cmd.getOptionValue("service"),
                    cmd.getOptionValue("topic"),
                    Paths.get(cmd.getOptionValue("input-video")));
        } catch (ParseException e) {
            System.err.println("Parsing failed: " + e.getMessage());
            new HelpFormatter().printHelp(ImageProducer.class.getSimpleName(), opts, true);
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
