/*
 * Copyright (C) 2021 National Institute of Informatics
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

import jp.ad.sinet.stream.api.InvalidMessageException;
import lombok.extern.java.Log;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.Security;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.RSAPrivateCrtKeySpec;

@Log
public class SecretDecoder {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private final Path privKeyFile;
    private RSAPrivateKey privKey;

    public SecretDecoder(Path privKeyFile) {
        this.privKeyFile = privKeyFile;
    }

    public byte[] decode(byte[] cipherText, String fingerprint) throws Exception {
        setupPrivKey();
        return parseSecret(privKey, cipherText);
    }

    public void setupPrivKey() throws Exception {
        if (privKey == null)
            privKey = getPrivateKey(this.privKeyFile);
    }

    private RSAPrivateKey getPrivateKey(Path privKeyFile) throws Exception {
        RSAPrivateKey privKey = readPrivateKey(privKeyFile.toFile());
        log.fine("privkey=" + privKey);
        log.fine("privkey.bitLength=" + privKey.getPrivateExponent().bitLength());
        return privKey;
    }

    static public String SINETSTREAM_PRIVATE_KEY_PASSPHRASE;
    private String getPass(String tgt) {
        if (SINETSTREAM_PRIVATE_KEY_PASSPHRASE != null)
            return SINETSTREAM_PRIVATE_KEY_PASSPHRASE;

        String env = System.getenv("SINETSTREAM_PRIVATE_KEY_PASSPHRASE");
        if (env != null)
            return env;

        Console console = System.console();
        if (console == null)
            return "";
        char[] passphrase = console.readPassword("Enter pass phrase for %s: ", tgt);
        return new String(passphrase);
    }

    private RSAPrivateKey readPrivateKey(File file) throws Exception {
        PEMParser pemParser = new PEMParser(new FileReader(file));
        Object object = pemParser.readObject();
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

        KeyPair kp;
        if (object instanceof PEMEncryptedKeyPair) {
            String password = getPass(file.getAbsolutePath());
            PEMEncryptedKeyPair ekp = (PEMEncryptedKeyPair) object;
            PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(password.toCharArray());
            kp = converter.getKeyPair(ekp.decryptKeyPair(decProv));
        } else {
            kp = converter.getKeyPair((PEMKeyPair) object);
        }
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        RSAPrivateCrtKeySpec privKeySpec = keyFactory.getKeySpec(kp.getPrivate(), RSAPrivateCrtKeySpec.class);
        return (RSAPrivateKey) keyFactory.generatePrivate(privKeySpec);
    }

    private byte[] decryptKey(byte[] encryptedKey, RSAPrivateKey privKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/None/OAEPWithSHA-256AndMGF1Padding", "BC");
        cipher.init(Cipher.DECRYPT_MODE,
                    privKey,
                    new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));
        byte[] key = cipher.doFinal(encryptedKey);
        return key;
    }
    private byte[] parseSecret(RSAPrivateKey privKey, byte[] secData) throws Exception {
        try {
            // get the private key length in bytes.
            int keySize = (privKey.getPrivateExponent().bitLength() + (8 - 1)) / 8;
            log.fine("XXX:keySize=" + keySize);
            int ivSize = 12;
            int tagSize = 16;

            ByteArrayInputStream bais = new ByteArrayInputStream(secData);
            DataInputStream dis = new DataInputStream(bais);

            int headerSize = 2 + 1 + 1;
            short ver = dis.readShort();
            if (ver != 1)
                throw new InvalidMessageException("Unsupported version");
            byte pubKeyType = dis.readByte();
            if (pubKeyType != 1)
                throw new InvalidMessageException("public key type in the encrypted data must be 1, but " + pubKeyType);
            byte commonKeyType = dis.readByte();
            if (commonKeyType != 1)
                throw new InvalidMessageException("common key type in the encrypted data must be 1, but " + commonKeyType);

            byte[] encryptedKey = new byte[keySize];
            dis.read(encryptedKey);
            byte[] iv = new byte[ivSize];
            dis.read(iv);
            byte[] datawithtag = new byte[dis.available()];
            dis.read(datawithtag);

            log.fine("parse:encryptedKey=" + DatatypeConverter.printHexBinary(encryptedKey));
            log.fine("parse:iv=" + DatatypeConverter.printHexBinary(iv));
            log.fine("parse:datawithtag=" + DatatypeConverter.printHexBinary(datawithtag));

            byte[] key = decryptKey(encryptedKey, privKey);

            Cipher cipher= Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec= new SecretKeySpec(key, "AES");
            GCMParameterSpec gcmParameterSpec= new GCMParameterSpec(tagSize * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);

            int aadSize = headerSize + keySize + ivSize;
            byte[] aad = new byte[aadSize];
            System.arraycopy(secData, 0, aad, 0, aadSize);
            cipher.updateAAD(aad);
            byte[] decryptedText= cipher.doFinal(datawithtag);
            log.fine("XXX:decryptedText=" + DatatypeConverter.printHexBinary(decryptedText));
            return decryptedText;
        }
        catch (IOException e) {
            throw new InvalidMessageException("malformed secret message", e);
        }
    }
}
