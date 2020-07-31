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

    public double getMsgCountRate() { return (double)msgCountTotal / getTime(); }

    @Getter
    @Setter
    private long msgBytesTotal;

    public double getMsgBytesRate() { return (double)msgBytesTotal / getTime(); }

    @Getter
    @Setter
    private long msgSizeMin;

    public double getMsgSizeAvg() { return (double)msgBytesTotal / msgCountTotal; }

    @Getter
    @Setter
    private long msgSizeMax;

    @Getter
    @Setter
    private long errorCountTotal;

    public double getErrorCountRate() { return (double)errorCountTotal / getTime(); }

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
