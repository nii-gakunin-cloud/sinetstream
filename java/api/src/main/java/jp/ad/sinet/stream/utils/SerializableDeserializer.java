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

package jp.ad.sinet.stream.utils;

import jp.ad.sinet.stream.api.Deserializer;
import jp.ad.sinet.stream.api.SinetStreamIOException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Objects;

@Deprecated
public class SerializableDeserializer<T extends Serializable> implements Deserializer<T> {
    @Override
    public T deserialize(byte[] bytes) {
        if (Objects.isNull(bytes)) {
            return null;
        }
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        try (ObjectInputStream ois = new ObjectInputStream(in)) {
            return readObject(ois);
        } catch (IOException | ClassNotFoundException e) {
            // TODO
            e.printStackTrace();
            throw new SinetStreamIOException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private T readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        Object ret = ois.readObject();
        return (T) ret;
    }
}
