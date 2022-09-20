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

package jp.ad.sinet.stream.plugins.s3;

import jp.ad.sinet.stream.api.InvalidConfigurationException;

import lombok.extern.java.Log;

import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoField;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.function.Predicate;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Log
class Util {
    static ZoneId parseUtcOffset(String z) {
        if (z == null) {
            ZoneId zi = ZoneOffset.systemDefault();;
            log.fine("parseUtcOffset: null -> " + zi);
            return zi;
        }
        int i = Integer.parseInt(z);
        int h, m;
        if (i < -99 || i > 99) {
            h = i / 100;
            m = i % 100;
        } else {
            h = i;
            m = 0;
        }
        ZoneOffset zo = ZoneOffset.ofHoursMinutes(h, m);
        log.fine("parseUtcOffset: " + z + " -> " + h + "," + m + "=" + zo);
        return zo;
    }

    // note: key = prefix/topic/name.../topic-uuid-seqno.suffix

    class ObjPath {
        String prefix;
        String topic;
        DateTimeFormatter formatter;
        ZoneId zoneId;
        String uuid;
        int seq;
        String suffix;

        ObjPath(String prefix, String topic, String name, String utcOffset, String suffix) {
            this.prefix = prefix;
            this.topic = topic;
            String pat;
            switch (name) {
            case "day":     pat = "yyyy/MM/dd"; break;
            case "hour":    pat = "yyyy/MM/dd/HH"; break;
            case "minute":  pat = "yyyy/MM/dd/HH/mm"; break;
            case "_second": pat = "yyyy/MM/dd/HH/mm/ss"; break;
            default:
                throw new InvalidConfigurationException("invalid s3.name specified");
            }
            this.formatter = DateTimeFormatter.ofPattern(pat);
            try {
                this.zoneId = Util.parseUtcOffset(utcOffset);
            }
            catch (RuntimeException e) {
                throw new InvalidConfigurationException("invalid utc_offset");
            }

            this.uuid = UUID.randomUUID().toString();
            this.seq = 0;
            this.suffix = suffix;
        }

        String gen(long now_us) {
            long t = now_us / 1000_000;
            Instant i = Instant.ofEpochSecond(t);
            OffsetDateTime odt = OffsetDateTime.ofInstant(i, zoneId);
            seq++;
            StringBuilder s = new StringBuilder();
            s.append(prefix).append("/")
             .append(topic).append("/")
             .append(odt.format(formatter)).append("/")
             .append(topic).append("-").append(uuid).append("-").append(seq).append(suffix);
            return s.toString();
        }

        String getUuid() {
            return uuid;
        }
    }

    static Predicate<String> makePathFilter(String prefix, List<String> topics, String name, String suffix) {
        StringBuilder s = new StringBuilder();
        s.append("^")
         .append(Pattern.quote(prefix))
         .append("/");

        String re_topics; {
            StringBuilder sb = new StringBuilder();
            sb.append("(?:");
            sb.append(String.join("|", topics.stream()
                                             .map(x -> Pattern.quote(x))
                                             .collect(Collectors.toList())));
            sb.append(")");
            re_topics = sb.toString();
        }
        s.append(re_topics)
          .append("/");

        String re_year = "[1-9][0-9][0-9][0-9]/";
        String re_month = "(?:0[1-9]|1[012])/";
        String re_day = "(?:0[1-9]|[12][0-9]|3[01])/";
        String re_hour = "(?:[01][0-9]|2[0-3])/";
        String re_minute = "(?:[0-5][0-9])/";
        switch (name) {
        case "day":    s.append(re_year + re_month + re_day); break;
        case "hour":   s.append(re_year + re_month + re_day + re_hour); break;
        case "minute": s.append(re_year + re_month + re_day + re_hour + re_minute); break;
        default:
            throw new InvalidConfigurationException("invalid s3.name specified");
        }

        s.append(re_topics)
         .append("-")
         .append("[^/]+")
         .append(Pattern.quote(suffix))
         .append("$");

        log.fine("re=" + s.toString());
        return Pattern.compile(s.toString()).asPredicate();
    }

    static String extractTopicFromPath(String path) {
    // note: key = prefix/topic/name.../topic-uuid-seqno.suffix
        int i1 = path.indexOf('/');
        int i2 = path.indexOf('/', i1);
        if (i1 < 0 || i2 < 0)
            return null;
        return path.substring(i1, i2);
    }
}
