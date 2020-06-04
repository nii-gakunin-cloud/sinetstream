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

import jp.ad.sinet.stream.api.SinetStreamIOException;
import org.apache.commons.rng.RestorableUniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.text.RandomStringGenerator;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class KeyStoreUtil {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private final Map<String, String> params;

    private KeyStoreUtil(Map<String, String> params) {
        this.params = params;
    }

    public static Map<String, String> setupKeyStore(Map<String, String> params) {
        KeyStoreUtil util = new KeyStoreUtil(params);
        return util.getKeyStoreParams();
    }

    private Map<String, String> getKeyStoreParams() {
        HashMap<String, String> ret = new HashMap<>();
        String caPassword = generatePassword();
        String password = generatePassword();

        Optional.ofNullable(params.get("ca_certs")).ifPresent(cert -> {
            try {
                Path keyStore = x509ToKeyStore(cert, caPassword, "ca");
                ret.put("trustStore", keyStore.toAbsolutePath().normalize().toString());
                ret.put("trustStoreType", "JKS");
                ret.put("trustStorePassword", caPassword);
            } catch (IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException e) {
                throw new SinetStreamIOException(e);
            }
        });
        Optional.ofNullable(params.get("certfile")).ifPresent(cert -> {
            try {
                Path keyStore = keyPairToKeyStore(cert, params.get("keyfile"), params.get("keyfilePassword"), password);
                ret.put("keyStore", keyStore.toAbsolutePath().normalize().toString());
                ret.put("keyStoreType", "JKS");
                ret.put("keyStorePassword", password);
            } catch (IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException e) {
                throw new SinetStreamIOException(e);
            }
        });
        return ret;
    }


    private String generatePassword() {
        RestorableUniformRandomProvider rnd = RandomSource.create(RandomSource.MT);
        RandomStringGenerator gen = new RandomStringGenerator.Builder().withinRange(33, 126).usingRandom(rnd::nextInt).build();
        return gen.generate(16);
    }

    private Path x509ToKeyStore(String x509, String password, String alias)
            throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert;
        try (InputStream in = Files.newInputStream(Paths.get(x509))) {
            cert = (X509Certificate) cf.generateCertificate(in);
        }

        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, password.toCharArray());
        ks.setCertificateEntry(alias, cert);

        Path path = Files.createTempFile(null, ".jks");
        try (OutputStream out = Files.newOutputStream(path)) {
            ks.store(out, password.toCharArray());
        }
        path.toFile().deleteOnExit();
        return path;
    }

    private Path keyPairToKeyStore(String certfile, String keyfile, String keyfilePassword, String password) throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException {
        final String alias = "client";

        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, password.toCharArray());

        X509Certificate cert = loadX509File(certfile);
        ks.setCertificateEntry(alias, cert);

        PrivateKey pk = loadPrivateKey(keyfile, keyfilePassword);
        PrivateKeyEntry entry = new PrivateKeyEntry(pk, new Certificate[]{cert});
        ks.setEntry(alias, entry, new KeyStore.PasswordProtection(password.toCharArray()));

        Path path = Files.createTempFile(null, ".jks");
        try (OutputStream out = Files.newOutputStream(path)) {
            ks.store(out, password.toCharArray());
        }
        path.toFile().deleteOnExit();
        return path;
    }

    private X509Certificate loadX509File(String cert) throws CertificateException, IOException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        try (InputStream in = Files.newInputStream(Paths.get(cert))) {
            return (X509Certificate) cf.generateCertificate(in);
        }
    }

    private PrivateKey loadPrivateKey(String key, String password) throws IOException {
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        try (PEMParser parser = new PEMParser(Files.newBufferedReader(Paths.get(key)))) {
            Object obj = parser.readObject();
            if (obj instanceof PEMEncryptedKeyPair) {
                PEMEncryptedKeyPair ckp = (PEMEncryptedKeyPair) obj;
                PEMDecryptorProvider dec = new JcePEMDecryptorProviderBuilder().build(password.toCharArray());
                PEMKeyPair kp = ckp.decryptKeyPair(dec);
                return converter.getPrivateKey(kp.getPrivateKeyInfo());
            } else {
                return converter.getPrivateKey((PrivateKeyInfo) obj);
            }
        }
    }
}
