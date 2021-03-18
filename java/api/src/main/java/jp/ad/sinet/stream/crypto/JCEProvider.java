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

package jp.ad.sinet.stream.crypto;

import jp.ad.sinet.stream.api.Crypto;
import jp.ad.sinet.stream.api.InvalidConfigurationException;
import jp.ad.sinet.stream.api.SinetStreamException;
import jp.ad.sinet.stream.spi.CryptoProvider;
import lombok.extern.java.Log;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;

@Log
public class JCEProvider implements CryptoProvider {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Override
    public Crypto getCrypto(Map<String, ?> parameters) {
        Cipher cipher = getCipher(parameters);
        return new JCEProcessor(cipher, parameters);
    }

    @Override
    public boolean isSupported(Map<String, ?> parameters) {
        try {
            return Objects.nonNull(getCipher(parameters));
        } catch (SinetStreamException e) {
            log.log(Level.FINE, e.getMessage(), e);
            return false;
        }
    }

    private Cipher getCipher(Map<String, ?> parameters) {
        try {
            return Cipher.getInstance(getTransformation(parameters));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            log.log(Level.FINE, e.getMessage(), e);
            return null;
        }
    }

    private String getTransformation(Map<String,?> parameters) {
        String ret = Optional.ofNullable(parameters.get("algorithm"))
                .filter(String.class::isInstance).map(String.class::cast)
                .orElseThrow(InvalidConfigurationException::new) +
                //'_' + parameters.get("key_length") +
                '/' +
                Optional.ofNullable(parameters.get("mode"))
                        .filter(String.class::isInstance).map(String.class::cast)
                        .map(x -> {
                            if ("OpenPGP".equals(x)) {
                                return "OpenPGPCFB";
                            }
                            return x;
                        })
                        .orElseThrow(InvalidConfigurationException::new) +
                '/' +
                Optional.ofNullable(parameters.get("padding"))
                        .filter(String.class::isInstance).map(String.class::cast)
                        .map(x -> {
                            switch (x.toLowerCase()) {
                                case "pkcs7":
                                    return "PKCS5Padding";
                                case "iso7816":
                                    return "ISO7816-4Padding";
                                case "x923":
                                    return "X9.23Padding";
                                case "none":
                                    return "NoPadding";
                                default:
                                    if (x.endsWith("Padding")) {
                                        return x;
                                    } else {
                                        // TODO
                                        throw new InvalidConfigurationException(x);
                                    }
                            }
                        })
                        .orElse("NoPadding");
        log.fine("TRANSFORMATION:" + ret);
        return ret;
    }
}
