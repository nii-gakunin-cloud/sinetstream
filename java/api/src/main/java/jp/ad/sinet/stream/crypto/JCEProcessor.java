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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import jp.ad.sinet.stream.api.Crypto;
import jp.ad.sinet.stream.api.InvalidConfigurationException;
import jp.ad.sinet.stream.api.SinetStreamException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.java.Log;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.xml.bind.DatatypeConverter;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

@Log
public class JCEProcessor implements Crypto {

    private final boolean debug = false;
    private final Cipher cipher;
    private final SecureRandom random;

    private int ivLength;
    private int keyLength;
    private SecretKeyFactory keyFactory;
    private char[] password;
    private int saltBytes;
    private int iterationCount;
    private String mode;
    private LoadingCache<String, byte[]> saltCache;
    private LoadingCache<SaltBytes, byte[]> keyCache;

    JCEProcessor(Cipher cipher, Map<String, ?> parameters) {
        this.cipher = cipher;
        this.random = new SecureRandom();
        setupKeyFactory(parameters);
        setupCache();
    }

    private void setupKeyFactory(Map<String,?> parameters) {
        keyLength =
                Optional.ofNullable(parameters.get("key_length"))
                        .filter(Integer.class::isInstance).map(Integer.class::cast).orElse(128);

        if (parameters.get("algorithm").equals("AES")) {
            // The IV length is the same as the block length of the cipher, which in the case of AES is 128 bits.
            ivLength = 128 / 8;
        } else {
            ivLength = keyLength / 8;
        }

        mode = Optional.ofNullable(parameters.get("mode"))
                .filter(String.class::isInstance).map(String.class::cast)
                .map(x -> {
                    if ("OpenPGP".equals(x)) {
                        return "OpenPGPCFB";
                    }
                    return x;
                })
                .orElseThrow(InvalidConfigurationException::new);

        Map keyParams = Optional.ofNullable(parameters.get("key_derivation"))
                .filter(Map.class::isInstance).map(Map.class::cast)
                .orElseGet(() -> {
                    Map<String, String> params = new HashMap<>();
                    params.put("algorithm", "pbkdf2");
                    return params;
                });

        String algorithm = Optional.ofNullable(keyParams.get("algorithm"))
                .filter(String.class::isInstance).map(String.class::cast)
                .map(x -> {
                    if ("pbkdf2".equals(x)) {
                        return "PBKDF2WithHmacSHA256";
                    }
                    throw new SinetStreamException("Unsupported algorithm: " + x);
                })
                .orElse("PBKDF2WithHmacSHA256");
        try {
            this.keyFactory = SecretKeyFactory.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new SinetStreamException(e);
        }
        if (parameters.containsKey("password")) {
            if (parameters.get("password") instanceof String) {
                this.password = Optional.ofNullable(parameters.get("password"))
                        .filter(String.class::isInstance).map(String.class::cast)
                        .map(String::toCharArray)
                        .orElseThrow(InvalidConfigurationException::new);
            } else if (parameters.get("password") instanceof Map) {
                Map pw = (Map)parameters.get("password");
                String value = Optional.ofNullable(pw.get("value"))
                                .filter(String.class::isInstance).map(String.class::cast).orElse(null);
                String path = Optional.ofNullable(pw.get("path"))
                                .filter(String.class::isInstance).map(String.class::cast).orElse(null);
                if (value != null && path != null || value == null && path == null)
                    throw new InvalidConfigurationException();
                if (path != null) {
                    try {
                        value = String.join("\n", Files.readAllLines(Paths.get(path)));
                    }
                    catch (IOException e) {
                        throw new InvalidConfigurationException();
                    }
                }
                this.password = value.toCharArray();
            } else {
                throw new InvalidConfigurationException();
            }
        }
        this.saltBytes = Optional.ofNullable(keyParams.get("salt_bytes"))
                .filter(Integer.class::isInstance).map(Integer.class::cast).orElse(8);
        this.iterationCount = Optional.ofNullable(keyParams.get("iteration"))
                .filter(Integer.class::isInstance).map(Integer.class::cast).orElse(10000);
    }

    @EqualsAndHashCode
    private static class SaltBytes {
        @Getter
        private byte[] salt;
        SaltBytes(byte[] salt) {
            this.salt = salt;
        }
    }

    private void setupCache() {
        CacheLoader<String, byte[]> saltLoader = new CacheLoader<String, byte[]>() {
            @SuppressWarnings("NullableProblems")
            @Override
            public byte[] load(String key) {
                byte[] salt = new byte[saltBytes];
                random.nextBytes(salt);
                return salt;
            }
        };
        saltCache = CacheBuilder.newBuilder().expireAfterWrite(60, TimeUnit.MINUTES).build(saltLoader);

        CacheLoader<SaltBytes, byte[]> keyCacheLoader = new CacheLoader<SaltBytes, byte[]>() {
            @Override
            public byte[] load(SaltBytes salt) throws InvalidKeySpecException {
                PBEKeySpec keySpec = new PBEKeySpec(password, salt.getSalt(), iterationCount, keyLength);
                SecretKey pbeKey = keyFactory.generateSecret(keySpec);
                return pbeKey.getEncoded();
            }
        };
        keyCache = CacheBuilder.newBuilder().maximumSize(5).build(keyCacheLoader);
    }

    @Override
    public Function<byte[], byte[]> getEncoder(Map<String, ?> parameters) {
        if (isAAD()) {
            return this::encryptAAD;
        } else {
            return this::encrypt;
        }
    }

    private boolean isAAD() {
        switch (mode) {
            case "CCM":
            case "EAX":
            case "GCM":
            case "OCB":
                return true;
            default:
                return false;
        }
    }

    private byte[] encrypt(byte[] data) {
        try {
            byte[] salt = getSalt();
            SecretKeySpec key = getSecretKeySpec(salt);

            byte[] iv = new byte[ivLength];
            random.nextBytes(iv);

            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
            byte[] encrypted = cipher.doFinal(data);
            if (debug) {
                System.err.println("XXX:encrypt: saltBytes=" + saltBytes);
                System.err.println("XXX:encrypt: salt.length=" + salt.length);
                System.err.println("XXX:encrypt: salt='" + DatatypeConverter.printHexBinary(salt) + "'");
                System.err.println("XXX:encrypt: iv.length=" + iv.length);
                System.err.println("XXX:encrypt: iv='" + DatatypeConverter.printHexBinary(iv) + "'");
                System.err.println("XXX:encrypt: encrypted.length=" + encrypted.length);
                System.err.println("XXX:encrypt: encrypted='" + DatatypeConverter.printHexBinary(encrypted) + "'");
            }
            return ByteBuffer.allocate(saltBytes + iv.length + encrypted.length)
                    .put(salt).put(iv).put(encrypted).array();
        } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e) {
            throw new SinetStreamException(e);
        }
    }

    private SecretKeySpec getSecretKeySpec(byte[] salt) {
        byte[] key = keyCache.getUnchecked(new SaltBytes(salt));
        return new SecretKeySpec(key, cipher.getAlgorithm());
    }

    private byte[] encryptAAD(byte[] data) {
        try {
            byte[] salt = getSalt();
            SecretKeySpec key = getSecretKeySpec(salt);

            byte[] iv = new byte[ivLength];
            random.nextBytes(iv);

            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
            cipher.updateAAD(salt);
            cipher.updateAAD(iv);
            byte[] encrypted = cipher.doFinal(data);

            if (debug) {
                System.err.println("XXX:encryptAAD: saltBytes=" + saltBytes);
                System.err.println("XXX:encryptAAD: salt.length=" + salt.length);
                System.err.println("XXX:encryptAAD: salt='" + DatatypeConverter.printHexBinary(salt) + "'");
                System.err.println("XXX:encryptAAD: iv.length=" + iv.length);
                System.err.println("XXX:encryptAAD: iv='" + DatatypeConverter.printHexBinary(iv) + "'");
                System.err.println("XXX:encryptAAD: encrypted.length=" + encrypted.length);
                System.err.println("XXX:encryptAAD: encrypted='" + DatatypeConverter.printHexBinary(encrypted) + "'");
            }
            return ByteBuffer.allocate(saltBytes + iv.length + encrypted.length)
                    .put(salt).put(iv).put(encrypted).array();
        } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e) {
            throw new SinetStreamException(e);
        }
    }

    private byte[] getSalt() {
        return saltCache.getUnchecked(keyFactory.getAlgorithm());
    }

    @Override
    public Function<byte[], byte[]> getDecoder(Map<String, ?> parameters) {
        if (isAAD()) {
            return this::decryptAAD;
        } else {
            return this::decrypt;
        }
    }

    private byte[] decrypt(byte[] data) {
        try {
            if (debug)
                System.err.println("XXX:decrypt: data='" + DatatypeConverter.printHexBinary(data) + "'");
            byte[] salt = new byte[saltBytes];
            System.arraycopy(data, 0, salt, 0, salt.length);
            if (debug) {
                System.err.println("XXX:decrypt: saltBytes=" + saltBytes);
                System.err.println("XXX:decrypt: salt='" + DatatypeConverter.printHexBinary(salt) + "'");
            }
            SecretKeySpec key = getSecretKeySpec(salt);
            if (debug)
                System.err.println("XXX:decrypt: key='" + DatatypeConverter.printHexBinary(key.getEncoded()) + "'");

            byte[] iv = new byte[ivLength];
            System.arraycopy(data, saltBytes, iv, 0, iv.length);
            if (debug) {
                System.err.println("XXX:decrypt: iv.length=" + iv.length);
                System.err.println("XXX:decrypt: iv='" + DatatypeConverter.printHexBinary(iv) + "'");
            }

            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            byte[] encrypted = new byte[data.length - saltBytes - iv.length];
            System.arraycopy(data, saltBytes + iv.length, encrypted, 0, encrypted.length);
            if (debug) {
                System.err.println("XXX:decrypt: encrypted='" + DatatypeConverter.printHexBinary(encrypted) + "'");
            }
            byte[] x = cipher.doFinal(encrypted);
            if (debug)
                System.err.println("XXX:decrypt: final='" + DatatypeConverter.printHexBinary(x) + "'");
            return x;
        } catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new SinetStreamException(e);
        }
    }

    private byte[] decryptAAD(byte[] data) {
        try {
            if (debug)
                System.err.println("XXX:decryptAAD: data='" + DatatypeConverter.printHexBinary(data) + "'");
            byte[] salt = new byte[saltBytes];
            System.arraycopy(data, 0, salt, 0, salt.length);
            if (debug) {
                System.err.println("XXX:decryptAAD: saltBytes=" + saltBytes);
                System.err.println("XXX:decryptAAD: salt='" + DatatypeConverter.printHexBinary(salt) + "'");
            }
            SecretKeySpec key = getSecretKeySpec(salt);
            if (debug) {
                System.err.println("XXX:decryptAAD: key='" + DatatypeConverter.printHexBinary(key.getEncoded()) + "'");
            }

            byte[] iv = new byte[ivLength];
            System.arraycopy(data, saltBytes, iv, 0, iv.length);
            if (debug) {
                System.err.println("XXX:decryptAAD: iv.length=" + iv.length);
                System.err.println("XXX:decryptAAD: iv='" + DatatypeConverter.printHexBinary(iv) + "'");
            }

            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            cipher.updateAAD(salt);
            cipher.updateAAD(iv);
            byte[] encrypted = new byte[data.length - saltBytes - iv.length];
            System.arraycopy(data, saltBytes + iv.length, encrypted, 0, encrypted.length);
            if (debug) {
                System.err.println("XXX:decryptAAD: encrypted='" + DatatypeConverter.printHexBinary(encrypted) + "'");
            }
            byte[] x = cipher.doFinal(encrypted);
            if (debug) {
                System.err.println("XXX:decryptAAD: final='" + DatatypeConverter.printHexBinary(x) + "'");
            }
            return x;
        } catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new SinetStreamException(e);
        }
    }
}
