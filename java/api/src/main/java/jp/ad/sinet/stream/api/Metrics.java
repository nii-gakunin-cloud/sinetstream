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

package jp.ad.sinet.stream.api;

import lombok.Getter;
import lombok.Setter;

public class Metrics {

    @Getter
    @Setter
    private long startTimeMillis;

    @Getter
    @Setter
    private long endTimeMillis;

    public double getStartTime() { return (double)startTimeMillis / 1000; }
    public double getEndTime() { return (double)endTimeMillis / 1000; }
    public double getTime() { return (double)(endTimeMillis - startTimeMillis) / 1000; }
    public long getTimeMillis() { return endTimeMillis - startTimeMillis; }

    @Getter
    @Setter
    private long msgCountTotal;

    public double getMsgCountRate() {
        double t = getTime();
        if (t == 0)
            return 0;
        return (double)msgCountTotal / t;
    }

    @Getter
    @Setter
    private long msgUncompressedBytesTotal;

    @Getter
    @Setter
    private long msgCompressedBytesTotal;

    public double getMsgCompressionRatio() {
        long u = msgUncompressedBytesTotal;
        long c = msgCompressedBytesTotal;
        return u > 0 ? (double)c / (double)u : 1.0;
    }

    @Getter
    @Setter
    private long msgBytesTotal;

    public double getMsgBytesRate() {
        double t = getTime();
        if (t == 0)
            return 0;
        return (double)msgBytesTotal / t;
    }

    @Getter
    @Setter
    private long msgSizeMin;

    public double getMsgSizeAvg() {
        if (msgCountTotal == 0)
            return 0;
        return (double)msgBytesTotal / msgCountTotal;
    }

    @Getter
    @Setter
    private long msgSizeMax;

    @Getter
    @Setter
    private long errorCountTotal;

    public double getErrorCountRate() {
        double t = getTime();
        if (t == 0)
            return 0;
        return (double)errorCountTotal / t;
    }

    @Getter
    @Setter
    private Object raw;

    public String toString() {
        return "Metrics{"
            + "startTime=" + getStartTime()
            + ",endTime=" + getEndTime()
            + ",time=" + getTime()
            + ",msgCountTotal=" +  getMsgCountTotal()
            + ",msgCountRate=" +  getMsgCountRate()
            + ",msgCompressedBytesTotal=" + getMsgCompressedBytesTotal()
            + ",msgUncompressedBytesTotal=" + getMsgUncompressedBytesTotal()
            + ",msgBytesTotal=" + getMsgBytesTotal()
            + ",msgBytesRate=" + getMsgBytesRate()
            + ",msgSizeMin=" + getMsgSizeMin()
            + ",msgSizeAvg=" + getMsgSizeAvg()
            + ",msgSizeMax=" + getMsgSizeMax()
            + ",errorCountTotal=" + getErrorCountTotal()
            + ",errorCountRate=" + getErrorCountRate()
            + "}";
    }
}
