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
import java.util.logging.Level;
import java.util.logging.Logger;

class CliUtil {
    static String prog = "sinetstream_cli";
    static boolean debugmode = System.getenv("SINETSTREAM_CLI_DEBUG") != null;
    static Logger rootLogger = Logger.getLogger("net");
    static Level level = Level.WARNING;
    static Level[] levels = {
        Level.SEVERE,
        Level.WARNING,
        Level.INFO,
        Level.CONFIG,
        Level.FINE,
        Level.FINER,
        Level.FINEST,
    };

    public static void verbosely() {
        boolean hit = false;
        for (Level lvl : levels) {
            if (hit) {
                //System.err.printf("Level: %s -> %s\n", level, lvl);
                level = lvl;
                break;
            }
            if (level == lvl)
                hit = true;
        }
        rootLogger.setLevel(level);
    }

    static void err(String fmt, Object... args) {
        if (level.intValue() <= Level.WARNING.intValue())
            System.err.printf("%s:ERROR: %s\n", prog, String.format(fmt, args));
        //rootLogger.warning(String.format("%s:ERROR: %s\n", prog, String.format(fmt, args)));
        System.exit(1);
    }

    static void info(String fmt, Object... args) {
        if (level.intValue() <= Level.INFO.intValue())
            System.err.printf("%s:INFO: %s\n", prog, String.format(fmt, args));
        //rootLogger.info(String.format("%s:INFO: %s\n", prog, String.format(fmt, args)));
    }

    static void debug(String fmt, Object... args) {
        if (level.intValue() <= Level.FINE.intValue())
            System.err.printf("%s:DEBUG: %s\n", prog, String.format(fmt, args));
        //rootLogger.fine(String.format("%s:DEBUG: %s\n", prog, String.format(fmt, args)));
    }

    static void printVersion() {
        System.out.printf("%s:version: %s\n", prog, CliMain.class.getPackage().getImplementationVersion());
        // xxx todo vesion of sinetstream, too.
    }
}
