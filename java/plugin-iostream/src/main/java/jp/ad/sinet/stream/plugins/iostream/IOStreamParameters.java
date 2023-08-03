/*
 * Copyright (C) 2023 National Institute of Informatics
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

package jp.ad.sinet.stream.plugins.iostream;

import jp.ad.sinet.stream.api.InvalidConfigurationException;

import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

@Value
@RequiredArgsConstructor
class IOStreamParameters {
    final InputStream ist;
    final byte[] iba;
    final OutputStream ost;

    @SuppressWarnings("unchecked")
    public static IOStreamParameters create(Map<String, Object> config) {
        Map<String, Object> ioconfig = null;
        try {
            if (config != null)
                ioconfig = (Map<String, Object>) config.get("iostream");
        }
        catch (ClassCastException e) {
            throw new InvalidConfigurationException("the parameter iostream must be mapping");
        }
        if (ioconfig == null)
            throw new InvalidConfigurationException("the parameter iostream must be specified");

        Object o;
        InputStream ist = null;
        o = ioconfig.get("input_stream");
        if (o != null) {
            try {
                ist = (InputStream) o;
            }
            catch (ClassCastException e) {
                throw new InvalidConfigurationException("iostream.input_stream must be a class InputStream");
            }
        }

        byte[] iba = null;
        o = ioconfig.get("input_byte_array");
        if (o != null) {
            try {
                iba = (byte[]) o;
            }
            catch (ClassCastException e) {
                throw new InvalidConfigurationException("iostream.input_byte_array must be byte[]");
            }
        }

        OutputStream ost = null;
        o = ioconfig.get("output_stream");
        if (o != null) {
            try {
                ost = (OutputStream) o;
            }
            catch (ClassCastException e) {
                throw new InvalidConfigurationException("iostream.output_stream must be a class OutputStream");
            }
        }

        return new IOStreamParameters(ist, iba, ost);
    }
}
