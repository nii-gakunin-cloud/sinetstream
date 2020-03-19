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
import socket
import ssl
from math import inf
from queue import Queue, Empty
from threading import Condition

from paho.mqtt.client import (
    Client, MQTT_ERR_QUEUE_SIZE, MQTT_ERR_NO_CONN,
    connack_string, MQTTv31, MQTTv311, MQTTv5, WebsocketConnectionError,
)
from sinetstream import (
    AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE,
    InvalidArgumentError, ConnectionError,
)

logger = logging.getLogger(__name__)


QOS_MAP = {
    AT_MOST_ONCE: 0,
    AT_LEAST_ONCE: 1,
    EXACTLY_ONCE: 2,
}


def _to_qos(params):
    qos = params.get('qos')
    if qos is not None:
        if qos not in QOS_MAP.values():
            raise InvalidArgumentError('Invalid QoS level')
        return qos

    consistency = params.get('consistency')
    assert consistency in QOS_MAP
    return QOS_MAP[consistency]


class MqttReaderHandleIter(object):
    def __init__(self, reader):
        logger.debug("MqttReaderHandleIter:init")
        self._reader = reader

    def __next__(self):
        logger.debug("MqttReaderHandleIter:next")
        if self._reader is None:
            raise StopIteration()
        try:
            return self._reader.pop_rcvq()
        except Empty:
            self._reader = None
            raise StopIteration()


def _get_broker(params):
    if 'brokers' not in params:
        logger.error("You must specify one broker.")
        raise InvalidArgumentError("You must specify one broker.")

    brokers = params["brokers"]
    if isinstance(brokers, list):
        if len(brokers) > 1:
            logger.error("only one broker can be specified")
            raise InvalidArgumentError("only one broker can be specified")
        elif len(brokers) == 0:
            logger.error("You must specify one broker.")
            raise InvalidArgumentError("You must specify one broker.")
        host_port = brokers[0].split(':', 1)
    else:
        host_port = brokers.split(':', 1)
    if len(host_port) == 2:
        return host_port[0], int(host_port[1])
    else:
        return host_port[0], _get_default_port(params)


def _get_default_port(params):
    transport = params.get('transport')
    is_tls = 'tls_set' in params

    if transport != 'websockets':
        return 1883 if not is_tls else 8883
    else:
        return 80 if not is_tls else 443


_MQTT_NESTED_PARAMETER = [
    'max_inflight_messages_set',
    'max_queued_messages_set',
    'message_retry_set',
    'ws_set_options',
    'tls_set',
    'tls_insecure_set',
    'username_pw_set',
    'will_set',
    'reconnect_delay_set',
]


PROTOCOL_MAP = {
    'MQTTv31': MQTTv31,
    'MQTTv311': MQTTv311,
    'MQTTv5': MQTTv5,
    None: MQTTv311,
}


def _to_protocol(protocol):
    if protocol not in PROTOCOL_MAP:
        raise InvalidArgumentError(f'protocol: invalid value: {protocol}')
    return PROTOCOL_MAP[protocol]


def _get_transport(params):
    transport = params.get('transport', 'tcp')
    if transport not in ['tcp', 'websockets']:
        raise InvalidArgumentError(f'transport: invalid value: {transport}')
    return transport


def _create_mqtt_client(params):
    mqttc = Client(
        client_id=params.get("client_id", ''),
        clean_session=params.get('clean_session'),
        protocol=_to_protocol(params.get('protocol')),
        transport=_get_transport(params),
    )

    for name in _MQTT_NESTED_PARAMETER:
        if name not in params:
            continue
        logger.debug(f'invoke: {name}({params[name]})')
        getattr(mqttc, name)(**params[name])

    mqttc.enable_logger(logger)
    return mqttc


def _translate_tls_params(params):
    tls = params.get('tls')
    if tls is None:
        return {}
    elif type(tls) is bool:
        return {'tls_set': {}}
    elif type(tls) is dict:
        tls_set = dict([
            (key, tls[key])
            for key in ['ca_certs', 'certfile', 'keyfile', 'ciphers']
            if key in tls
        ])
        mqtt_params = {'tls_set': tls_set}
        if 'check_hostname' in tls:
            mqtt_params['tls_insecure_set'] = {
                'value': not(tls['check_hostname']),
            }
        return mqtt_params
    else:
        logger.error("tls: must be bool or associative array")
        raise InvalidArgumentError("tls: must be bool or associative array")


def _replace_ssl_params(params):
    if 'tls_set' not in params:
        return
    params['tls_set'] = dict([
        (
            key,
            (value
             if not(key in ['cert_reqs', 'tls_version'] and
                    isinstance(value, str) and hasattr(ssl, value))
             else getattr(ssl, value))
        )
        for key, value in params['tls_set'].items()
    ])


class MqttClient(object):
    def __init__(self, params):
        self._params = _translate_tls_params(params)
        self._params.update(params)
        _replace_ssl_params(self._params)
        logger.debug(self._params)

        try:
            self._mqttc = _create_mqtt_client(self._params)
        except ValueError as ex:
            raise InvalidArgumentError(ex)

        self._mqttc.on_connect = self._on_connect
        self.qos = _to_qos(self._params)
        self.connection_timeout = self._params.get('connection_timeout', 10)
        self.keepalive = self._params.get('keepalive', 60)
        self.host, self.port = _get_broker(self._params)
        logger.debug(f'broker={self.host} port={self.port}')
        self.protocol = _to_protocol(self._params.get('protocol')),
        self._connection_result = None
        self._conn_cond = Condition()

    def open(self, timeout=None):
        logger.debug("open")
        try:
            self._mqttc.connect(self.host, self.port, self.keepalive)
        except (socket.error, OSError, WebsocketConnectionError):
            logger.error(f"cannot connect broker: {self.host}:{self.port}")
            self.close()
            raise ConnectionError(
                f"cannot connect broker: {self.host}:{self.port}")

        self._mqttc.loop_start()

        if timeout is None:
            timeout = self.connection_timeout
        with self._conn_cond:
            ret = self._conn_cond.wait_for(self._is_connected, timeout)
            if not ret:
                self.close()
                raise ConnectionError('connection timed out')
            if self._connection_result != 0:
                if self.protocol == MQTTv5:
                    reason = str(self._connection_result)
                else:
                    reason = connack_string(self._connection_result)
                self.close()
                raise ConnectionError(f'connection error: reason={reason}')
        return self

    def close(self):
        logger.debug("close")
        try:
            self._mqttc.disconnect()
            self._mqttc.loop_stop()
        except Exception:
            logger.error("mqtt close() error")

    def _is_connected(self):
        return self._connection_result is not None

    def _on_connect(self, client, userdata, flags, rc, properties=None):
        logger.debug(f"MQTT:on_connect: rc={rc}")
        if rc != 0:
            logger.error(f"MQTT: {connack_string(rc)}: {rc}")
        with self._conn_cond:
            self._connection_result = rc
            self._conn_cond.notify_all()


class MqttReader(MqttClient):
    def __init__(self, params):
        logger.debug("MqttReader:init")
        super().__init__(params)
        self._rcvq = Queue()
        self.topics = self._get_topics()
        timeout_ms = self._params["receive_timeout_ms"]
        self._timeout = timeout_ms / 1000.0 if timeout_ms != inf else None
        self._mqttc.on_message = self._on_message

    def _get_topics(self):
        topics = self._params.get('topics', [])
        return topics if isinstance(topics, list) else [topics]

    def _on_connect(self, client, userdata, flags, rc, properties=None):
        super()._on_connect(client, userdata, flags, rc, properties)
        for topic in self.topics:
            client.subscribe(topic, self.qos)

    def _on_message(self, client, userdata, message):
        logger.debug(f"MQTT:on_message: message={message}")
        self._rcvq.put(message)

    def pop_rcvq(self):
        message = self._rcvq.get(block=True, timeout=self._timeout)
        return message.payload, message.topic, message

    def __iter__(self):
        assert self._mqttc is not None
        return MqttReaderHandleIter(self)


class MqttWriter(MqttClient):
    def __init__(self, params):
        logger.debug("MqttWriter:init")
        super().__init__(params)
        self.topic = self._params['topic']
        self.retain = self._params.get('retain', False)
        logger.debug(f'qos={self.qos} topic={self.topic} retain={self.retain}')

    def publish(self, msg):
        logger.debug("MqttWriter:publish")
        if type(msg) != bytes:
            logger.error("MqttWriter: msg must be bytes")
            raise InvalidArgumentError("MqttWriter: msg must be bytes")
        msginfo = self._mqttc.publish(
            self.topic, payload=msg, qos=self.qos, retain=self.retain)
        logger.debug(f"publish => rc={msginfo.rc} mid={msginfo.mid}")
        if msginfo.rc != MQTT_ERR_QUEUE_SIZE:
            msginfo.wait_for_publish()
        elif msginfo.rc == MQTT_ERR_NO_CONN:
            raise ConnectionError('client is not currently connected')
        return msginfo
