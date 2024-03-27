/*
 * Copyright (C) 2024 National Institute of Informatics
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

package jp.ad.sinet.stream.packet;

import jp.ad.sinet.stream.api.InvalidMessageException;
import jp.ad.sinet.stream.api.SinetStreamException;
import jp.ad.sinet.stream.api.SinetStreamIOException;

import lombok.extern.java.Log;
import lombok.Data;

import java.io.IOException;
import java.nio.ByteBuffer;

@Log
@Data
public class Packet {

    private static final byte messageMarker = (byte)0xDF;
    //private static final byte[] messageMarkerV3 = { (byte)0xDF, (byte)0x03, (byte)0x00, (byte)0x00 };
    private static final int messageMarkerLength = 4;
    private static final int keyVersionLength = 2;

    @Data
    static public class Header {
        private byte formatVersion;
        private short keyVersion;
    }

    private Header header;
    private byte[] message;

    public Packet() {
        /*
        log.setLevel(java.util.logging.Level.FINE);
        java.util.logging.Logger logger = log;
        java.util.logging.Logger rootlogger = null;
        java.util.logging.Handler roothandler = null;
        while (logger != null) {
            String name = logger.getName();
            java.util.logging.Level level = logger.getLevel();
            //System.err.println("XXX <" + name + "> " + (level != null ? level.toString() : "<null>"));
            java.util.logging.Handler[] handlers = logger.getHandlers();
            for (java.util.logging.Handler handler : handlers) {
                //System.err.println("XXX handler " + handler.toString() + " " + handler.getLevel());
                roothandler = handler;
            }
            rootlogger = logger;
            logger = logger.getParent();
        }
        //System.err.println("XXX --");
        //rootlogger.setLevel(java.util.logging.Level.FINE);
        roothandler.setLevel(java.util.logging.Level.FINE);
        */
    }

    static public byte[] encode(byte formatVersion, int keyVersion, byte[] message) {
        if (keyVersion > Short.MAX_VALUE)
            throw new SinetStreamException("keyVersion must be <= Short.MAX_VALUE");
        return encode(formatVersion, (short)keyVersion, message);
    }
    static public byte[] encode(byte formatVersion, short keyVersion, byte[] message) {
        switch (formatVersion) {
        case 0:
        case 1:
        case 2:
            return message;
        case 3:
            assert(formatVersion >= 3);
            assert(keyVersion >= 0);
            assert(message != null);
            ByteBuffer buf = ByteBuffer.allocate(messageMarkerLength + keyVersionLength + message.length);
            buf.put(messageMarker);
            buf.put(formatVersion);
            buf.put((byte)0);
            buf.put((byte)0);
            buf.putShort(keyVersion);
            buf.put(message);
            return buf.array();
        }
        throw new SinetStreamException("INTERNAL ERROR");
    }

    static public Packet decode(byte[] buf) {
        if (buf.length < messageMarkerLength) {
            log.fine(() -> String.format("too short: length=%d", (int)buf.length));
            return null;
        }
        if (buf[0] != messageMarker) {
            log.fine(() -> String.format("marker mismatch: hdr0=%h", (int)buf[0]));
            return null;
        }
        byte formatVersion = buf[1];
        if (formatVersion != 3) {
            log.fine(() -> String.format("version mismatch: hdr1=%h", (int)formatVersion));
            return null;
        }
        if (buf[2] != 0 || buf[3] != 0) {
            log.fine(() -> String.format("malformed header: hdr2=%h hdr3=%h", (int)buf[2], (int)buf[3]));
            return null;
        }

        if (buf.length < messageMarkerLength + keyVersionLength) {
            log.fine(() -> String.format("too short: length=%d", (int)buf.length));
            return null;
        }
        ByteBuffer bbuf = ByteBuffer.wrap(buf);
        bbuf.position(messageMarkerLength);
        short keyVersion = bbuf.getShort();
        if (keyVersion < 0) {
            String emsg = String.format("invalid keyVersion: keyVersion=%d", keyVersion);
            log.fine(emsg);
            throw new InvalidMessageException(emsg);
        }

        byte[] msg = new byte[buf.length - messageMarkerLength - keyVersionLength];
        bbuf.get(msg);

        Header hdr = new Header();
        hdr.setFormatVersion(formatVersion);
        hdr.setKeyVersion(keyVersion);
        Packet pkt = new Packet();
        pkt.setHeader(hdr);
        pkt.setMessage(msg);
        return pkt;
    }
}
