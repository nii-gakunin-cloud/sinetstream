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

import java.io.File;
import java.io.FileWriter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class CliUtil {
    static String prog = "sinetstream_cli";
    static boolean debugmode = System.getenv("SINETSTREAM_CLI_DEBUG") != null;

    static void err(String fmt, Object... args) {
        System.err.printf("%s:ERROR: %s\n", prog, String.format(fmt, args));
        System.exit(1);
    }

    static void info(String fmt, Object... args) {
        System.err.printf("%s:INFO: %s\n", prog, String.format(fmt, args));
    }

    static void debug(String fmt, Object... args) {
        if (debugmode)
            System.err.printf("%s:DEBUG: %s\n", prog, String.format(fmt, args));
    }

    static String makeTempConfig() throws Exception {
        String service = "tempserv";  // NOTE: keep the same value in sinetstream_cli.py
        Path path = Paths.get("./.sinetstream_config.yml");
        try {
            Files.createFile(path);
        }
        catch (FileAlreadyExistsException e) {
            return null;
        }
        // XXX fixme race condition exists
        File file = new File(path.toUri());
        file.deleteOnExit();
        try (FileWriter fw = new FileWriter(file)) {
            /// NOTE: KEEP the same content in sinetstream_cli.py
            fw.write("header:\n" +
                     "  version: 2\n" +
                     "config:\n" +
                     "  " + service + ": {}\n");
        }
        CliUtil.info("the temporary config file " + path + " created");
        return service;
    }
}
