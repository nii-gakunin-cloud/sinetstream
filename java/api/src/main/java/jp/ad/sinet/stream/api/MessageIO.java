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

package jp.ad.sinet.stream.api;

import java.util.Map;

public interface MessageIO extends AutoCloseable {

    String getService();

    String getTopic();

    Consistency getConsistency();

    String getClientId();

    Map<String, Object> getConfig();

    ValueType getValueType();

    boolean isDataEncryption();

    Metrics getMetrics();

    void resetMetrics(boolean reset_raw);

    default void resetMetrics() { resetMetrics(false); }

    Object getInfo(String ipath);

    default Object getInfo() {
        return getInfo(null);
    }

    void close();
}
