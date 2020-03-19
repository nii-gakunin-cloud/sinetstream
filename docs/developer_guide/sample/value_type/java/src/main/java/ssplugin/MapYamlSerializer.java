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
package ssplugin;

import jp.ad.sinet.stream.api.Serializer;
import org.yaml.snakeyaml.Yaml;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@SuppressWarnings("rawtypes")
class MapYamlSerializer implements Serializer<Map> {
    @Override
    public byte[] serialize(Map map) {
        Yaml yaml = new Yaml();
        return yaml.dump(map).getBytes(StandardCharsets.UTF_8);
    }
}
