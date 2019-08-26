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

package jp.ad.sinet.stream.example.text;

import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.api.MessageReader;
import jp.ad.sinet.stream.api.Message;
import jp.ad.sinet.stream.api.ValueType;
import jp.ad.sinet.stream.utils.MessageReaderFactory;
import org.apache.commons.cli.*;

import java.time.Duration;
import java.util.Objects;

@SuppressWarnings({"WeakerAccess", "CodeBlock2Expr"})
public class TextConsumer {

    private final String service;
    private final String topic;

    public TextConsumer(String service, String topic) {
        this.service = service;
        this.topic = topic;
    }

    public void run() throws Exception {
        MessageReaderFactory<String> factory =
                MessageReaderFactory.<String>builder()
                        .service(service)
                        .topic(topic)
                        .consistency(Consistency.AT_LEAST_ONCE)
                        .valueType(ValueType.TEXT)
                        .receiveTimeout(Duration.ofSeconds(30))
                        .build();
        try(MessageReader<String> reader = factory.getReader()) {
            Message<String> msg;
            while (Objects.nonNull(msg = reader.read())) {
                System.out.println(msg.getValue());
            }
        }
    }

    public static void main(String[] args) {
        Options opts = new Options();
        opts.addOption(Option.builder("s").required().hasArg().longOpt("service").build());
        opts.addOption(Option.builder("t").required().hasArg().longOpt("topic").build());

        CommandLineParser parser = new DefaultParser();
        TextConsumer consumer = null;
        try {
            CommandLine cmd = parser.parse(opts, args);
            consumer = new TextConsumer(cmd.getOptionValue("service"), cmd.getOptionValue("topic"));
        } catch (ParseException e) {
            System.err.println("Parsing failed: " + e.getMessage());
            new HelpFormatter().printHelp(TextProducer.class.getSimpleName(), opts, true);
            System.exit(1);
        }
        try {
            consumer.run();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
