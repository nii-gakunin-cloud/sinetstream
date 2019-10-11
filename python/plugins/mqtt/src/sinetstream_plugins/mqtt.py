#!/usr/local/bin/python3.6
# vim: expandtab shiftwidth=4

# Copyright (C) 2019 National Institute of Informatics
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

import logging
import queue
import threading

import paho.mqtt.client
from sinetstream import (
    make_message, AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE,
    InvalidArgumentError, ConnectionError, AlreadyConnectedError,
)
# from sinetstream.api import setdict

logger = logging.getLogger(__name__)


def to_qos(consistency):
    if consistency == AT_MOST_ONCE:
        return 0
    elif consistency == AT_LEAST_ONCE:
        return 1
    elif consistency == EXACTLY_ONCE:
        return 2
    else:
        assert False


def wait_for_connected(rdwrter):
    while not rdwrter.connected:
        logger.debug("MQTT:wait for connected")
        rdwrter.conn_cond.wait()


def on_connect(client, userdata, flags, rc):
    logger.debug(f"MQTT:on_connect: rc={rc}")
    if rc != 0:
        logger.error(f"MQTT: {paho.mqtt.client.connack_string(rc)} (rc={rc})")
        return
    rdwrter = userdata
    with rdwrter.lock:
        assert not rdwrter.connected
        rdwrter.connected = True
        rdwrter.conn_cond.notify_all()
    for topic in rdwrter.subscribing_topics:
        client.subscribe(topic, rdwrter.qos)


def on_disconnect(client, userdata, rc):
    logger.debug(f"MQTT:on_disconnect: rc={rc}")
    pass


def on_message(client, userdata, message):
    logger.debug(f"MQTT:on_message: message={message}")
    rdwrter = userdata
    rdwrter.push_rcvq(message)


def on_publish(client, userdata, mid):
    logger.debug(f"MQTT:on_publish: mid={mid}")
    writer = userdata
    writer.published(mid)


def on_subscribe(client, userdata, mid, granted_qos):
    logger.debug(f"MQTT:on_subscribe: mid={mid} granted_qos={granted_qos}")
    pass


def on_unsubscribe(client, userdata, mid):
    logger.debug(f"MQTT:on_unsubscribe: mid={mid}")
    pass


def on_log(client, userdata, level, buf):
    logger.debug(f"MQTT:on_log")
    pass


def on_socket_open(client, userdata, sock):
    logger.debug(f"MQTT:on_socket_open")
    pass


def on_socket_close(client, userdata, sock):
    logger.debug(f"MQTT:on_socket_close")
    pass


def on_socket_register_write(client, userdata, sock):
    logger.debug(f"MQTT:on_socket_register_write")
    pass


def on_socket_unregister_write(client, userdata, sock):
    logger.debug(f"MQTT:on_socket_unregister_write")
    pass


def set_callbacks(mqttc):
    mqttc.on_connect = on_connect
    mqttc.on_disconnect = on_disconnect
    mqttc.on_message = on_message
    mqttc.on_publish = on_publish
    mqttc.on_subscribe = on_subscribe
    mqttc.on_unsubscribe = on_unsubscribe
    mqttc.on_log = on_log
    mqttc.on_socket_open = on_socket_open
    mqttc.on_socket_close = on_socket_close
    mqttc.on_socket_register_write = on_socket_register_write
    mqttc.on_socket_unregister_write = on_socket_unregister_write


class MqttReaderHandleIter(object):
    def __init__(self, reader):
        logger.debug("MqttReaderHandleIter:init")
        self.reader = reader

    def __next__(self):
        logger.debug("MqttReaderHandleIter:next")
        message = self.reader.pop_rcvq()
        return make_message(self.reader._reader, message.payload, message.topic, message)


def mqtt_client_start(mqttc, params):
    brokers = params["brokers"]
    if len(brokers) > 1:
        logger.error("only one broker can be specified")
        raise InvalidArgumentError()
    hostport = brokers[0].rsplit(":", 1)
    if len(hostport) == 2:
        hostport[1] = int(hostport[1])

    ca_certs = None
    certfile = None
    keyfile = None
    cert_reqs = paho.mqtt.client.ssl.CERT_REQUIRED
    tls_version = paho.mqtt.client.ssl.PROTOCOL_TLS
    ciphers = None
    use_tls = False

    username_pw_set = params.get("username_pw_set")
    if username_pw_set:
        username = username_pw_set.get("username")
        password = username_pw_set.get("password")
        mqttc.username_pw_set(username, password)

    tls = params.get("tls")
    if tls:
        if type(tls) is bool:
            use_tls = tls
        elif type(tls) is dict:
            use_tls = True
            ca_certs = tls.get("ca_certs", ca_certs)
            certfile = tls.get("certfile", certfile)
            keyfile = tls.get("keyfile", keyfile)
            ciphers = tls.get("ciphers", ciphers)
        else:
            logger.error("tls: must be bool or associative array")
            raise InvalidArgumentError()

    tls = params.get("security_protocol")
    if tls == "SSL" or tls == "TLS" or use_tls:
        use_tls = True
        ca_certs = params.get("ca_certs", ca_certs)
        certfile = params.get("certfile", certfile)
        keyfile = params.get("keyfile", keyfile)
        ciphers = params.get("ciphers", ciphers)

    tls = params.get("tls_set")
    if tls:
        use_tls = True
        ca_certs = tls.get("ca_certs", ca_certs)
        certfile = tls.get("certfile", certfile)
        keyfile = tls.get("keyfile", keyfile)
        tls_version = tls.get("tls_version", tls_version)
        ciphers = tls.get("ciphers", ciphers)

    if use_tls:
        logger.debug(f"mqttc.tls_set:"
                     f" ca_certs={ca_certs}"
                     f",certfile={certfile}"
                     f",keyfile={keyfile}"
                     f",cert_reqs={cert_reqs}"
                     f",tls_version={tls_version}"
                     f",ciphers={ciphers}")
        mqttc.tls_set(ca_certs=ca_certs,
                      certfile=certfile,
                      keyfile=keyfile,
                      cert_reqs=cert_reqs,
                      tls_version=tls_version,
                      ciphers=ciphers)

    set_callbacks(mqttc)

    try:
        mqttc.connect(*hostport)
    except ConnectionRefusedError:
        logger.error(f"cannot connect broker: {brokers}")
        raise ConnectionError()
    mqttc.loop_start()


def make_list(x):
    if x is None:
        return []
    elif isinstance(x, list):
        return x
    else:
        return [x]


class MqttReader(object):
    def __init__(self, message_reader):
        logger.debug("MqttReader:init")
        self._reader = message_reader
        self.qos = to_qos(message_reader.params["consistency"])
        self.subscribing_topics = make_list(message_reader.params["topics"])
        self.mqttc = None
        self.lock = threading.Lock()
        self.connected = False
        self.conn_cond = threading.Condition(self.lock)
        self.rcvq = queue.Queue()
        self.not_empty = threading.Condition(self.lock)

    def push_rcvq(self, message):
        self.rcvq.put_nowait(message)

    def pop_rcvq(self):
        with self.lock:
            wait_for_connected(self)
        timeout = self._reader.params["receive_timeout_ms"]
        if timeout != float("inf"):
            try:
                msg = self.rcvq.get(block=True, timeout=timeout/1000.0)
            except queue.Empty:
                raise StopIteration()
        else:
            msg = self.rcvq.get(block=True, timeout=None)
        return msg

    def open(self):
        logger.debug("MqttReader:open")
        if self.mqttc is not None:
            logger.error(f"already connected")
            raise AlreadyConnectedError()

        mqttc = paho.mqtt.client.Client(client_id=self._reader.params["client_id"], userdata=self)
        mqttc.enable_logger(logger)
        mqtt_client_start(mqttc, self._reader.params)
        self.mqttc = mqttc

        with self.lock:
            wait_for_connected(self)

        return self

    def close(self):
        logger.debug("MqttReader:close")
        self.mqttc.disconnect()
        self.mqttc = None

    def __iter__(self):
        assert self.mqttc is not None
        return MqttReaderHandleIter(self)

    def seek_to_beginning(self):
        logger.warning("MqttReader: seek_to_beginning() is not supoorted")

    def seek_to_end(self):
        logger.warning("MqttReader: seek_to_end() is not supoorted")


class MqttWriter(object):
    def __init__(self, message_writer):
        logger.debug("MqttWriter:init")
        self._writer = message_writer
        self.qos = to_qos(message_writer.params["consistency"])
        self.subscribing_topics = []
        self.mqttc = None
        self.lock = threading.Lock()
        self.connected = False
        self.conn_cond = threading.Condition(self.lock)
        self.inflight = dict()
        self.infl_cond = threading.Condition(self.lock)  # inflight is empty.

    def published(self, mid):
        with self.lock:
            # note: msginfo.is_published() == False at on_publish().
            # assert self.inflight[mid].is_published() will fail.
            del self.inflight[mid]
            if len(self.inflight) == 0:
                self.infl_cond.notify_all()

    def push_rcvq(self):
        pass

    def open(self):
        logger.debug("MqttWriter:open")
        if self.mqttc is not None:
            raise AlreadyConnectedError()

        mqttc = paho.mqtt.client.Client(client_id=self._writer.params["client_id"], userdata=self)
        mqttc.enable_logger(logger)
        mqtt_client_start(mqttc, self._writer.params)
        self.mqttc = mqttc

        return self

    def close(self):
        logger.debug("MqttWriter:close")

        while True:
            with self.lock:
                n = len(self.inflight)
                if n == 0:
                    break
                logger.debug(f"MqttWriter:wait inflight: n={n}")
                self.infl_cond.wait()
        self.mqttc.disconnect()
        self.mqttc = None

    def publish(self, msg):
        logger.debug("MqttWriter:publish")
        if type(msg) != bytes:
            logger.error("MqttWriter: msg must be bytes")
            raise InvalidArgumentError()
        with self.lock:
            if not self.connected:
                logger.warning("MqttWriter: not connected")
                wait_for_connected(self)
        with self.lock:
            msginfo = self.mqttc.publish(self._writer.params["topic"], payload=msg, qos=self.qos)
            logger.debug(f"publish => rc={msginfo.rc} mid={msginfo.mid}")
            if msginfo.rc != paho.mqtt.client.MQTT_ERR_SUCCESS:
                raise ConnectionError()
            self.inflight[msginfo.mid] = msginfo
