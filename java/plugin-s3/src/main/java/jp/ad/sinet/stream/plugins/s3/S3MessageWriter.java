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

import jp.ad.sinet.stream.utils.Timestamped;
import jp.ad.sinet.stream.plugins.s3.S3Cli;
import jp.ad.sinet.stream.plugins.s3.S3Parameters;
import jp.ad.sinet.stream.plugins.s3.Util;

import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.api.SinetStreamIOException;
import jp.ad.sinet.stream.api.InvalidConfigurationException;
import jp.ad.sinet.stream.spi.PluginMessageWriter;
import jp.ad.sinet.stream.spi.WriterParameters;

import lombok.Getter;
import lombok.extern.java.Log;
import lombok.Value;

import java.util.Map;
import java.util.HashMap;

import java.util.List;
import java.util.LinkedList;

import java.net.URI;
import java.net.URISyntaxException;
import java.lang.RuntimeException;

import java.util.UUID;

import java.time.OffsetDateTime;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

import java.lang.StringBuilder;

/*
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.Level;
*/
import software.amazon.awssdk.utils.Logger;


import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
//import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;

import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.core.ResponseBytes;

import java.util.List;
import java.util.stream.Collectors;
import java.util.function.Predicate;

import java.time.OffsetDateTime;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Log
public class S3MessageWriter implements PluginMessageWriter {

    @Getter
    private final String topic;

    @Getter
    private final Consistency consistency;

    @Getter
    private final String clientId;

    S3Cli s3cli;
    Util.ObjPath objPath;

    void connect() {
        s3cli = new S3Cli(s3parameters);
    }

    S3Parameters s3parameters;

    S3MessageWriter(WriterParameters parameters) {
        log.fine("S3MessageWriter: ctor=" + parameters);
        this.topic = parameters.getTopic();
        if (parameters.getConfig().get("brokers") != null)
            throw new InvalidConfigurationException("brokers: cannot be specified for s3; use s3.endpoint_url");
        this.consistency = parameters.getConsistency();
        if (consistency != Consistency.AT_MOST_ONCE)
            throw new InvalidConfigurationException("consistency must be AT_MOST_ONCE for s3");
        this.s3parameters = S3Parameters.create(parameters.getConfig());
        log.fine("S3MessageWriter: s3parameters=" + s3parameters);

        this.objPath = new Util().new ObjPath(this.s3parameters.getPrefix(),
                                        this.topic,
                                        this.s3parameters.getName(),
                                        this.s3parameters.getUtcOffset(),
                                        this.s3parameters.getSuffix());

        String clientId = parameters.getClientId();
        this.clientId = clientId != null && ! clientId.isEmpty() ? clientId
                                                                 : this.objPath.getUuid();
            UUID.randomUUID().toString();

        connect();

        if (s3parameters.getCreateBucketOnInit()) {
            try {
                this.s3cli.createBucket(this.s3parameters.getBucket());
            } catch (SinetStreamIOException e) {
                log.warning("create_bucket_on_init failure: " + e);
                e.printStackTrace();
            }
        }

        if (s3parameters.getDeleteObjectsOnInit()) {
            try {
                List<String> keys = new LinkedList<>();
                String continuationToken = null;
                do {
                    S3Cli.ListObjRes res = s3cli.listObj(
                            this.s3parameters.getBucket(),
                            this.s3parameters.getPrefix(),
                            (k -> true),
                            continuationToken);
                    keys.addAll(res.keyList);
                    continuationToken = res.continuationToken;
                } while (continuationToken != null);

                if (!keys.isEmpty())
                    s3cli.deleteObjs(this.s3parameters.getBucket(), keys);
            } catch (SinetStreamIOException e) {
                log.warning("delete_objects_on_init failure: " + e);
                e.printStackTrace();
            }
        }
    }

    @Override
    public void write(Timestamped<byte[]> message) {
        log.fine("S3MessageWriter: write");
        long tstamp = message.getTstamp();
        String path = objPath.gen(tstamp);
        s3cli.putObj(s3parameters.getBucket(), path, message.getValue(), tstamp);
    }

    @Override
    public void close() {
        if (s3cli != null) {
            s3cli.close();
            s3cli = null;
        }
    }

    @Override
    public Map<String, Object> getConfig() { return null; }

    @Override
    public Object getInfo(List<String> ipath) {
        int ipath_size;
        if (ipath == null || (ipath_size = ipath.size()) == 0) {
            Map<String, Object> info = new HashMap<>();
            Map<String, String> winfo = new HashMap<>();
            winfo.put("uuid", objPath.getUuid());
            info.put("writer", winfo);
            return info;
        } else if (ipath_size == 1) {
            if (ipath.get(0).equals("writer")) {
                Map<String, String> winfo = new HashMap<>();
                winfo.put("uuid", objPath.getUuid());
                return winfo;
            }
        } else if (ipath_size == 2) {
            if (ipath.get(0).equals("writer")) {
                if (ipath.get(1).equals("uuid"))
                    return objPath.getUuid();
            }
        }
        return null;
    }
}
