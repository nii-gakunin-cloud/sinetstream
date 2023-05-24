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
import jp.ad.sinet.stream.api.SinetStreamIOException;

import lombok.extern.java.Log;
import lombok.Value;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.regions.Region; // XXX
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;

import java.util.function.Predicate;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;

@Log
class S3Cli {
    S3Client s3;
    S3Cli(S3Parameters s3parameters) {
        log.fine("S3Cli:ctor: s3parameters=" + s3parameters);
        Region regionLst[] = { null, Region.AWS_GLOBAL }; // XXX Hack: builder.build() requires region.
        RuntimeException lastex = null;
        for (Region region : regionLst) {
            try {
                S3ClientBuilder builder = S3Client.builder();
                if (region != null)
                    builder.region(region);
                if (s3parameters.getAwsAccessKeyId() != null && s3parameters.getAwsSecretAccessKey() != null) {
                    AwsCredentials credentials = AwsBasicCredentials.create(s3parameters.getAwsAccessKeyId(), s3parameters.getAwsSecretAccessKey());
                    builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
                } else {
                    builder.credentialsProvider(DefaultCredentialsProvider.builder().build());
                }
                if (s3parameters.getEndpointURI() != null) {
                    builder.endpointOverride(s3parameters.getEndpointURI());
                }
                builder.forcePathStyle(true);  // XXX Hack
                s3 = builder.build();
                return;
            }
            catch (RuntimeException e) {
                // log.info("S3:" + e);
                lastex = e;
            }
        }
        throw lastex;
    }
    void close() {
        if (s3 != null) {
            s3.close();
            s3 = null;
        }
    }
    protected void finalize() {
        close();
    }

    void createBucket(String bucket) {
        try {
            S3Waiter s3w = s3.waiter();
            CreateBucketRequest req = CreateBucketRequest.builder()
                    .bucket(bucket)
                    .build();
            s3.createBucket(req);
            HeadBucketRequest wreq = HeadBucketRequest.builder()
                    .bucket(bucket)
                    .build();
            // Wait until the bucket is created and print out the response.
            WaiterResponse<HeadBucketResponse> wres = s3w.waitUntilBucketExists(wreq);
            wres.matched().response().ifPresent(x -> log.info(x.toString()));
            log.info("XXX " + bucket + " is ready");
        } catch (BucketAlreadyOwnedByYouException e) {
            log.fine("createBucket failure: " + e);
        } catch (S3Exception e) {
            throw new SinetStreamIOException("createBucket failure", e);
        }
    }

    void putObj(String bucket, String path, byte[] msg, long timestamp_us) {
        log.fine("pubObj: bucket=" + bucket);
        log.fine("pubObj: path=" + path);
        log.fine("pubObj: msg.len=" + msg.length);
        log.fine("pubObj: timestamp_us=" + timestamp_us);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("timestamp", String.valueOf(timestamp_us));
        try {
            PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(path)
                    .metadata(metadata)
                    .build();
            PutObjectResponse res = s3.putObject(req, RequestBody.fromBytes(msg));
            log.fine("PutObjectResponse=" + res);
        } catch (S3Exception e) {
            throw new SinetStreamIOException("putObject failure", e);
        }
    }

    @Value
    class ListObjRes {
        public List<String> keyList;
        public String continuationToken;
    }

    ListObjRes listObj(String bucket, String prefix, Predicate<String> keyFilter, String continuationToken) {
        log.fine("listObj: bucket=" + bucket);
        log.fine("listObj: prefix=" + prefix);
        log.fine("listObj: continuationToken=" + continuationToken);
        ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder();
        int maxKeys = 1; // for debug
        builder.bucket(bucket).maxKeys(maxKeys).prefix(prefix);
        if (continuationToken != null) {
            builder.continuationToken(continuationToken);
        }
        ListObjectsV2Request req = builder.build();
        ListObjectsV2Response res = s3.listObjectsV2(req);
        List<S3Object> objs = res.contents();
        List<String> keys = objs.stream()
                .map((obj) -> obj.key())
                .peek(x -> log.fine("listObj.peek: " + x))
                .filter((key) -> keyFilter.test(key))
                .collect(Collectors.toList());
        ListObjRes r = new ListObjRes(keys, res.nextContinuationToken());
        log.fine("listObjRes=" + r);
        return r;
    }

    @Value
    class GetObjRes {
        public byte[] data;
        public long timestamp_us;
        public GetObjectResponse res;
    }

    GetObjRes getObj(String bucket, String key) {
        log.fine("getObj: bucket=" + bucket);
        log.fine("getObj: key=" + key);
        GetObjectRequest req = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        ResponseBytes<GetObjectResponse> resbytes = s3.getObjectAsBytes(req);
        GetObjectResponse res = resbytes.response();
        String timestamp_us = res.metadata().get("timestamp");
        long tstamp_us = 0;
        if (timestamp_us != null) {
            try {
                tstamp_us = Long.parseLong(timestamp_us);
            } catch (NumberFormatException e) {
                //ignore
            }
        }
        GetObjRes r = new GetObjRes(resbytes.asByteArray(), tstamp_us, res);
        log.fine("GetObjRes=" + r);
        return r;
    }

    void deleteObjs(String bucket, List<String> keys) {
        log.fine("deleteObjs: bucket=" + bucket);
        log.fine("deleteObjs: keys=" + keys);
        List<ObjectIdentifier> xxx = new LinkedList<>();
        for (String key : keys)
            xxx.add(ObjectIdentifier.builder().key(key).build());
        Delete del = Delete.builder().objects(xxx).build();
        DeleteObjectsRequest req = DeleteObjectsRequest.builder()
                .bucket(bucket)
                .delete(del)
                .build();
        try {
            s3.deleteObjects(req);
        } catch (S3Exception e) {
            throw new SinetStreamIOException("deleteObjects failure", e);
        }
    }
}
