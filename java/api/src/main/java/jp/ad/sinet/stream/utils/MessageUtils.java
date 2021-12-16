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

package jp.ad.sinet.stream.utils;

import jp.ad.sinet.stream.api.*;
import jp.ad.sinet.stream.api.valuetype.ValueTypeFactory;
import jp.ad.sinet.stream.crypto.SecretDecoder;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class MessageUtils {
    @SuppressWarnings("unchecked")
    Map<String,Object> loadServiceParameters(String service, Path configFile, String configName, Path authFile, Path privKeyFile, Object debugHttpTransport) {
        Map<String,Object> params;
        if (configName != null) {
            params = ConfigClient.getConfig(service, configName, authFile, debugHttpTransport);
        } else {
            Map<String,Map<String,Object>> configs = new ConfigLoader(configFile).loadConfigFile();
            Map<String,Object> header = configs.get("header");
            if (header != null) {
                Object ver = header.get("version");
                if (ver == null)
                    throw new NoServiceException("No config version in the header");
                if (!(ver instanceof Integer))
                    throw new NoServiceException("config version must be int");
                if ((Integer)ver != 2)
                    throw new InvalidConfigurationException("config verison must be 2, but " + ver);
                Map<String,Object> config = configs.get("config");
                if (config == null)
                    throw new InvalidConfigurationException("config section does not exist");
                params = (Map<String,Object>) getServiceParam(config, service);
            } else {
                // assume version 1
                params = getServiceParam(configs, service);
            }
        }
        if (params == null)
            throw new NoServiceException();
        try {
            if (privKeyFile == null)
                privKeyFile= Paths.get(System.getProperty("user.home"), ".config", "sinetstream", "private_key.pem");
            return decryptoParameters(params, privKeyFile);
        }
        catch (Exception e) {
            throw new NoServiceException(e);
        }
    }
    @SuppressWarnings("unchecked")
    public static <V> V getServiceParam(Map<String,V> configs, String service) {
        if (service == null) {
            Set<String> services = configs.keySet();
            if (services.isEmpty())
                throw new NoServiceException();
            if (services.size() > 1)
                throw new NoServiceException("too many services defined");
            service = services.iterator().next();
        }
        return configs.get(service);
    }
    @SuppressWarnings("unchecked")
    private Map<String,Object> decryptoParameters(Map<String,Object> params, Path privKeyFile) throws Exception {
        return (Map<String,Object>) decryptoParameters1(params, new SecretDecoder(privKeyFile));
    }
    @SuppressWarnings("unchecked")
    public Map<String,Object> debugDecryptoParameters(Map<String,Object> params, SecretDecoder decoder) throws Exception {
        return (Map<String,Object>) decryptoParameters1(params, decoder);
    }
    @SuppressWarnings("unchecked")
    private Object decryptoParameters1(Object x, SecretDecoder decoder) throws Exception {
        if (x instanceof Map) {
            Map<String,Object> map = (Map<String,Object>) x;
            Map<String,Object> map2 = new LinkedHashMap<String,Object>(map.size());
            for (Map.Entry<String, Object> e : map.entrySet()) {
                map2.put(e.getKey(), decryptoParameters1(e.getValue(), decoder));
            }
            return map2;
        } else if (x instanceof List) {
            List<Object> lst = (List<Object>) x;
            List<Object> lst2 = new ArrayList<Object>(lst.size());
            for (Object o : lst) {
                lst2.add(decryptoParameters1(o, decoder));
            }
            return lst2;
        } else if (x instanceof SecretValue) {
            SecretValue sv = (SecretValue) x;
            byte[] v2 = decoder.decode(sv.getValue(), sv.getFingerprint());
            return v2;
        } else {
            return x;
        }
    }

    void mergeParameters(Map<String, Object> target, Map<String, Object> newValues) {
        target.putAll(newValues);
    }

    static Consistency toConsistency(Object value) {
        if (value instanceof Consistency) {
            return (Consistency) value;
        } else if (value instanceof String) {
            return Consistency.valueOf((String) value);
        } else {
            return null;
        }
    }

    static ValueType toMessageType(Object value) {
        if (value instanceof ValueType) {
            return (ValueType) value;
        } else if (value instanceof String) {
            return new ValueTypeFactory().get((String) value);
        } else {
            return null;
        }
    }

    public static Boolean toBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
			String txtValue = (String) value;
			if (txtValue.equalsIgnoreCase("true")) {
				return true;
			} else if (txtValue.equalsIgnoreCase("false")) {
				return false;
			} else {
				throw new SinetStreamException("value (" + txtValue + ") cannot convert to Boolean");
			}
        } else {
            return null;
        }
    }

    public static Integer toInteger(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
                return ((Number) value).intValue();
        } else if (value instanceof String) {
            return Integer.parseInt((String) value);
        } else {
            return null;
        }
    }

    public static Long toLong(Object value) {
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            return Long.parseLong((String) value);
        } else {
            return null;
        }
    }

    public static String toString(Object value) {
        if (value instanceof String) {
            return (String) value;
        } else {
            return null;
        }
    }

    public static List<String> toStringList(Object value) {
        if (!(value instanceof List)) {
            return null;
        }
        return ((List<?>) value).stream().map(MessageUtils::toString).collect(Collectors.toList());
    }

    static Duration toReceiveTimeout(Object value) {
        if (value instanceof Duration) {
            return (Duration) value;
        } else if (value instanceof Number) {
            return Duration.ofMillis(((Number) value).longValue());
        } else if (value instanceof String) {
            return Duration.ofMillis(Long.parseLong((String) value));
        } else {
            return null;
        }
    }

    @SuppressWarnings("rawtypes")
    static Deserializer toDeserializer(Object value) {
        return toClassObject(Deserializer.class, value);
    }

    @SuppressWarnings("rawtypes")
    static Serializer toSerializer(Object value) {
        return toClassObject(Serializer.class, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T toClassObject(Class<T> targetClass, Object value) {
        Class cls = null;
        if (targetClass.isInstance(value)) {
            return targetClass.cast(value);
        } else if (value instanceof Class) {
            cls = (Class) value;
        } else if (value instanceof String) {
            try {
                cls = Class.forName((String) value);
            } catch (ClassNotFoundException e) {
                throw new SinetStreamIOException(e);
            }
        }
        if (Objects.nonNull(cls)) {
            if (targetClass.isAssignableFrom(cls)) {
                try {
                    return targetClass.cast(cls.getDeclaredConstructor().newInstance());
                } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                    throw new SinetStreamIOException(e);
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    static List<String> toBrokers(Object value) {
        if (value instanceof List) {
            return ((List<Object>) value).stream()
                    .filter(String.class::isInstance).map(String.class::cast)
                    .collect(Collectors.toList());
        } else if (value instanceof String) {
            return Arrays.stream(((String) value).split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
