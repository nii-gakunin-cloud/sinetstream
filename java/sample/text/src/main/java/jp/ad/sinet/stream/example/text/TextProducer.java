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

package jp.ad.sinet.stream.example.text;

import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.api.MessageWriter;
import jp.ad.sinet.stream.api.valuetype.SimpleValueType;
import jp.ad.sinet.stream.utils.MessageWriterFactory;
import org.apache.commons.cli.*;

import java.util.Scanner;

@SuppressWarnings("WeakerAccess")
public class TextProducer {

    private final String service;
    private final String config;

    public TextProducer(String service, String config) {
        this.service = service;
        this.config = config;
    }

    public void run() {
        MessageWriterFactory<String> factory =
                MessageWriterFactory.<String>builder()
                        .service(service)
                        .configName(config)
                        //.consistency(Consistency.AT_LEAST_ONCE)
                        .valueType(SimpleValueType.TEXT)
                        .build();
        try (MessageWriter<String> writer = factory.getWriter(); Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                writer.write(line);
            }
        }
    }

    public static void main(String[] args) {
        Options opts = new Options();
        opts.addOption(Option.builder("s").hasArg().longOpt("service").build());
        opts.addOption(Option.builder("c").hasArg().longOpt("config").build());

        CommandLineParser parser = new DefaultParser();
        TextProducer producer = null;
        try {
            CommandLine cmd = parser.parse(opts, args);
            String service = cmd.getOptionValue("service");
            String config = cmd.getOptionValue("config");
            if (service == null && config == null)
                throw new ParseException("-s/--service must be specified if -c/--config is not specified.");
            producer = new TextProducer(service, config);
        } catch (ParseException e) {
            System.err.println("Parsing failed: " + e.getMessage());
            new HelpFormatter().printHelp(TextProducer.class.getSimpleName(), opts, true);
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
