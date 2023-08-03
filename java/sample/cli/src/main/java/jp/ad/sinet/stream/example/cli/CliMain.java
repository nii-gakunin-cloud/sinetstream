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

package jp.ad.sinet.stream.example.cli;

import org.apache.commons.cli.*;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.Yaml;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
public class CliMain {
    private static void runWrite(
        boolean noConfigFile,
        String service,
        String config,
        boolean text,
        String file,
        String message,
        boolean line,
        Map<String, Object> configs)
    {
        try {
            new CliWriter(
                noConfigFile,
                service,
                config,
                text,
                file,
                message,
                line,
                configs).run();
        } catch (Exception e) {
            e.printStackTrace();
            CliUtil.err(e.toString());
        }
    }

    private static void runRead(
        boolean noConfigFile,
        String service,
        String config,
        boolean text,
        boolean verbose,
        boolean raw,
        String outdir,
        Long count,
        Map<String, Object> configs)
    {
        try {
            new CliReader(
                noConfigFile,
                service,
                config,
                text,
                verbose,
                raw,
                outdir,
                count,
                configs).run();
        } catch (Exception e) {
            e.printStackTrace();
            CliUtil.err(e.toString());
        }
    }

    private static void addConfig(Map<String, Object> configs, String ks[], Object v) {
        Map<String, Object> c = configs;
        int n = ks.length;
        for (String k : ks) {
            if (n == 1) {
                c.put(k, v);
                return;
            }
            Object w = c.get(k);
            if (w == null) {
                w = new HashMap<String, Object>();
                c.put(k, w);
            }
            c = (Map<String, Object>) w;
            n--;
        }
    }

    private static Object loadYaml(String x) {
        Yaml yaml = new Yaml(new SafeConstructor());
        return yaml.load(x);
    }

    private static String sep1 = "[.]";
    private static String sep2 = "[=]";

    private static Map<String, Object> buildConfigs(String[] args) {
        Map<String, Object> configs = new HashMap<String, Object>();
        for (String arg : args) {
            String[] kv = arg.split(sep2, 2);
            if (kv.length != 2) {
                CliUtil.err("invalid argument: %s", arg);
            }
            addConfig(configs, kv[0].split(sep1), loadYaml(kv[1]));
        }
        return configs;
    }

    private static Options buildOptions() {
        Options opts = new Options();
        opts.addOption(Option.builder("h").longOpt("help")
                                       .desc("this help")
                                       .build());
        opts.addOption(Option.builder("s").longOpt("service")
                                       .hasArg().argName("SERVICE")
                                       .desc("specify the service name")
                                       .build());
        opts.addOption(Option.builder("c").longOpt("config")
                                       .hasArg().argName("CONFIG")
                                       .desc("specify the config name when using config service")
                                       .build());
        opts.addOption(Option.builder("t").longOpt("text")
                                       .desc("text mode")
                                       .build());
        opts.addOption(Option.builder("nc").longOpt("no-config-file")
                                       .desc("don't load any sinetstream_config.yml")
                                       .build());
        opts.addOption(Option.builder("v").longOpt("verbose")
                                       .desc("verbose mode")
                                       .build());
        return opts;
    }

    private static void setLogLevel(CommandLine cmd) {
        for (Option o : cmd.getOptions())
            if (o.getOpt().equals("v"))
                CliUtil.verbosely();
    }

    private static void parseWrite(String[] args) {
        Options opts = buildOptions();
        opts.addOption(Option.builder("f").longOpt("file")
                                       .hasArg().argName("INPUT")
                                       .desc("write the contents of a file as the message")
                                       .build());
        opts.addOption(Option.builder("m").longOpt("message")
                                       .hasArg().argName("MESSAGE")
                                       .desc("write a single message from the command line")
                                       .build());
        opts.addOption(Option.builder("l").longOpt("line")
                                       .desc("split separate lines into separate messages")
                                       .build());

        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(opts, args);
            if (cmd.hasOption("help"))
                printHelp(opts);
            setLogLevel(cmd);
            runWrite(
                cmd.hasOption("no-config-file"),
                cmd.getOptionValue("service"),
                cmd.getOptionValue("config"),
                cmd.hasOption("text"),
                cmd.getOptionValue("file"),
                cmd.getOptionValue("message"),
                cmd.hasOption("line"),
                buildConfigs(cmd.getArgs()));
        } catch (ParseException e) {
            System.err.println("Parsing failed: " + e.getMessage());
            printHelp(opts);
        }
    }

    private static void parseRead(String[] args) {
        Options opts = buildOptions();
        opts.addOption(Option.builder("r").longOpt("raw")
                                       .desc("print just received messages")
                                       .build());
        opts.addOption(Option.builder("f").longOpt("file")
                                       .hasArg().argName("DIR")
                                       .desc("save received messages under the specified directory")
                                       .build());
        opts.addOption(Option.builder("c").longOpt("count")
                                       .hasArg().argName("N").type(Number.class)
                                       .desc("exit after the given count of messages have been received")
                                       .build());
        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(opts, args);
            if (cmd.hasOption("help"))
                printHelp(opts);
            setLogLevel(cmd);
            runRead(
                cmd.hasOption("no-config-file"),
                cmd.getOptionValue("service"),
                cmd.getOptionValue("config"),
                cmd.hasOption("text"),
                cmd.hasOption("verbose"),
                cmd.hasOption("raw"),
                cmd.getOptionValue("file"),
                (Long) cmd.getParsedOptionValue("count"),
                buildConfigs(cmd.getArgs()));
        } catch (ParseException e) {
            System.err.println("Parsing failed: " + e.getMessage());
            printHelp(opts);
        }
    }

    static void printHelp(Options opts) {
        new HelpFormatter().printHelp(CliUtil.prog + " [--option ...] [KEY=VALUE ...]", opts);
        System.exit(1);
    }

    static void printVersion() {
        CliUtil.printVersion();
    }

    public static void main(String[] args) {
        final String subcmdsHelp = "(only `write`, `read` or `version` can be specified)";
        if (args.length == 0)
            CliUtil.err("no subcommand specified " + subcmdsHelp);

        String arg0 = args[0];
        String[] args1 = Arrays.copyOfRange(args, 1, args.length);

        switch (arg0) {
        case "write":
            parseWrite(args1);
            break;
        case "read":
            parseRead(args1);
            break;
        case "--version":
        case "-v":
        case "version":
            printVersion();
            break;
        default:
            CliUtil.err("invalid subcommand: %s " + subcmdsHelp, arg0);
            break;
        }
    }
}
