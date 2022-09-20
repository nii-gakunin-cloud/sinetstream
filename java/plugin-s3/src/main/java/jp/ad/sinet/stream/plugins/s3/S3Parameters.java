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

import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.Map;

@Value
@RequiredArgsConstructor
class S3Parameters {
    String endpointUrl;
    String bucket;
    String prefix;
    String name;
    String utcOffset;
    String suffix;
    String awsAccessKeyId;
    String awsSecretAccessKey;
    Boolean createBucketOnInit;
    Boolean deleteObjectsOnInit;

    private static String getp(Map<String, Object> m, String k, boolean mustp) {
        Object o = m.get(k);
        if (o == null) {
            if (mustp)
                throw new InvalidConfigurationException("s3." + k + " must be specified");
            return null;
        }
        try {
            return (String) o;
        }
        catch (ClassCastException e) {
            throw new InvalidConfigurationException("s3." + k + " must be string");
        }
    }

    private static boolean getp(Map<String, Object> m, String k, boolean mustp, boolean dflt) {
        Object o = m.get(k);
        if (o == null) {
            if (mustp)
                throw new InvalidConfigurationException("s3." + k + " must be specified");
            return dflt;
        }
        try {
            return (Boolean) o;
        }
        catch (ClassCastException e) {
            throw new InvalidConfigurationException("s3." + k + " must be string");
        }
    }

    @SuppressWarnings("unchecked")
    public static S3Parameters create(Map<String, Object> config) {
        try {
            Map<String, Object> s3config = null;
            if (config != null)
                s3config = (Map<String, Object>) config.get("s3");
            if (s3config == null)
                throw new InvalidConfigurationException("the parameter s3 must be specified");
            return s3create(s3config);
        }
        catch (ClassCastException e) {
            throw new InvalidConfigurationException("the parameter s3 must be mapping");
        }
    }

    private static S3Parameters s3create(Map<String, Object> s3config) {
        S3Parameters r = new S3Parameters(
            getp(s3config, "endpoint_url", true),
            getp(s3config, "bucket", true),
            getp(s3config, "prefix", true),
            getp(s3config, "name", true),
            getp(s3config, "utc_offset", false),
            getp(s3config, "suffix", true),
            getp(s3config, "aws_access_key_id", false),
            getp(s3config, "aws_secret_access_key", false),
            getp(s3config, "create_bucket_on_init", false, false),
            getp(s3config, "delete_objects_on_init", false, false));
        r.check();
        return r;
    }

    private void check() {
        if ((awsAccessKeyId != null) != (awsSecretAccessKey != null)) {
            throw new InvalidConfigurationException("both aws_access_key_id and aws_secret_access_key must be specified");
        }
    }

    URI getEndpointURI() {
        try {
            return new URI(endpointUrl);
        } catch (URISyntaxException e) {
            throw new InvalidConfigurationException("invalid endpoint_url", e);
        }
    }
}
