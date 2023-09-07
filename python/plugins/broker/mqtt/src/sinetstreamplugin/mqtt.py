#!/usr/bin/env python3

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
from collections import OrderedDict
from math import inf
from queue import Queue, Empty
from sys import exc_info
from threading import Condition, Lock

from paho.mqtt.client import (
    Client, MQTT_ERR_QUEUE_SIZE, MQTT_ERR_NO_CONN,
    connack_string, MQTTv31, MQTTv311, MQTTv5, WebsocketConnectionError,
    MQTT_ERR_SUCCESS)
from paho.mqtt.properties import (Properties, MQTTException)
from paho.mqtt.packettypes import PacketTypes
from promise import Promise

from sinetstream import (
    AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE,
    InvalidArgumentError, ConnectionError,
    SinetError)

from sinetstream import SINETStreamMessageEncoder

logger = logging.getLogger(__name__)

QOS_MAP = {
    AT_MOST_ONCE: 0,
    AT_LEAST_ONCE: 1,
    EXACTLY_ONCE: 2,
    'AT_MOST_ONCE': 0,
    'AT_LEAST_ONCE': 1,
    'EXACTLY_ONCE': 2,
}


def _to_qos(params):
    qos = params.get('qos')
    if qos is not None:
        if qos not in QOS_MAP.values():
            raise InvalidArgumentError('Invalid QoS level')
        return qos

    consistency = params.get('consistency')
    if consistency is not None:
        assert consistency in QOS_MAP
        return QOS_MAP[consistency]

    return None


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
        clean_session=params.get('clean_session'),  # NOTE: MUST be None in MQTTv5, USE clean_start
        protocol=_to_protocol(params.get('protocol')),
        transport=_get_transport(params),
        # reconnect_on_failure=params.get("reconnect_on_failure", True),
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
                'value': not (tls['check_hostname']),
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
             if not (key in ['cert_reqs', 'tls_version'] and
                     isinstance(value, str) and hasattr(ssl, value))
             else getattr(ssl, value))
        )
        for key, value in params['tls_set'].items()
    ])


def _replace_will_params(params):
    if 'will_set' not in params:
        return
    will_params = params['will_set']

    for k in ['topic', 'payload']:
        if k not in will_params:
            raise InvalidArgumentError(f'the parameter {k} in will_set must be specified')

    if 'delay_interval' in will_params:
        props = Properties(PacketTypes.WILLMESSAGE)
        props.WillDelayInterval = will_params.pop('delay_interval')
        will_params['properties'] = props

    qos = _to_qos(will_params)
    if qos is not None:
        will_params['qos'] = qos
    will_params.pop('consistency', None)  # note: del will_params['consistency'] safely.

    args = ['topic', 'payload', 'qos', 'retain', 'properties']
    will_params2 = {k: will_params[k] for k in args if k in will_params}
    writer_params = {k: will_params[k] for k in will_params.keys() if k not in args}
    with SINETStreamMessageEncoder(**writer_params) as enc:
        payload = will_params['payload']
        will_params2['payload'] = enc.encode(payload, timestamp=0)

    params['will_set'] = will_params2


class MqttClient(object):
    def __init__(self, params):
        self._params = _translate_tls_params(params)
        self._params.update(params)
        _replace_ssl_params(self._params)
        _replace_will_params(self._params)
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
        self.protocol = _to_protocol(self._params.get('protocol'))
        self._connection_result = None
        self._conn_cond = Condition()

    def _make_properties(self):
        def name2(s):
            name = s.replace(" ", "")
            name_ = (s.replace("Information", "Info")
                      .replace("Authentication", "Auth")
                      .replace(" ", "_")
                      .lower())
            return (name, name_)
        properties = Properties(PacketTypes.CONNECT)
        n = 0
        for (name, name_) in [name2(k) for k in properties.names.keys()]:
            # print(f"name={name} name_={name_}")
            # ptype = PacketTypes.CONNECT
            # if ptype not in properties.properties[properties.getIdentFromName(name)][1]:
            #     continue
            if name_ in self._params:
                n += 1
                val = self._params[name_]
                if name_ == "user_property":
                    val = [(k, v) for k, v in val.items()]
                try:
                    properties.__setattr__(name, val)
                except MQTTException as ex:
                    # ok, maybe packet type mismatch.
                    logger.warning(f"{name_}: {ex}")
        logger.debug(f"properties={properties}")
        return properties if n > 0 else None

    def open(self, timeout=None):
        logger.debug("open")
        try:
            kwargs = {
                "host": self.host,
                "port": self.port,
                "keepalive": self.keepalive,
            }
            if "clean_start" in self._params:
                kwargs["clean_start"] = self._params["clean_start"]
            properties = self._make_properties()
            if properties is not None:
                kwargs["properties"] = properties
            self._mqttc.connect(**kwargs)
        except (socket.error, OSError, WebsocketConnectionError, ValueError):
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

    def metrics(self):
        return None

    def reset_metrics(self):
        pass

    def _is_connected(self):
        return self._connection_result is not None

    def _on_connect(self, client, userdata, flags, rc, properties=None):
        logger.debug(f"MQTT:on_connect: rc={rc}")
        if rc != 0:
            logger.error(f"MQTT: {connack_string(rc)}: {rc}")
        with self._conn_cond:
            self._connection_result = rc
            self._conn_cond.notify_all()

    def _debug_get_socket(self):
        return self._mqttc.socket()


class BaseMqttReader(MqttClient):
    def __init__(self, params):
        super().__init__(params)
        self.topics = self._get_topics()
        timeout_ms = self._params["receive_timeout_ms"]
        self._timeout = timeout_ms / 1000.0 if timeout_ms != inf else None

    def _get_topics(self):
        topics = self._params.get('topics', [])
        return topics if isinstance(topics, list) else [topics]

    def _on_connect(self, client, userdata, flags, rc, properties=None):
        super()._on_connect(client, userdata, flags, rc, properties)
        for topic in self.topics:
            client.subscribe(topic, self.qos)


class MqttReader(BaseMqttReader):
    def __init__(self, params):
        logger.debug("MqttReader:init")
        super().__init__(params)
        self._rcvq = Queue()
        self._mqttc.on_message = self._on_message

    def _on_message(self, client, userdata, message):
        logger.debug(f"MQTT:on_message: message={message}")
        self._rcvq.put(message)

    def pop_rcvq(self):
        message = self._rcvq.get(block=True, timeout=self._timeout)
        return message.payload, message.topic, message

    def __iter__(self):
        assert self._mqttc is not None
        return MqttReaderHandleIter(self)


class MqttAsyncReader(BaseMqttReader):
    def __init__(self, params):
        logger.debug("MqttAsyncReader:init")
        super().__init__(params)
        self._mqttc.on_message = self._mqtt_callback
        self._on_message = None

    def _mqtt_callback(self, client, userdata, message):
        logger.debug(f"MQTT:on_message: message={message}")
        if self._on_message is not None:
            self._on_message(message.payload, message.topic, message)

    @property
    def on_message(self):
        return self._on_message

    @on_message.setter
    def on_message(self, on_message):
        self._on_message = on_message

    @property
    def on_failure(self):
        pass


class BaseMqttWriter(MqttClient):
    def __init__(self, params):
        logger.debug("MqttWriter:init")
        super().__init__(params)
        self.topic = self._params['topic']
        self.retain = self._params.get('retain', False)

    def publish(self, msg):
        if not isinstance(msg, bytes):
            logger.error("MqttWriter: msg must be bytes")
            raise InvalidArgumentError("MqttWriter: msg must be bytes")
        return self._mqttc.publish(
            self.topic, payload=msg, qos=self.qos, retain=self.retain)


class MqttWriter(BaseMqttWriter):
    def __init__(self, params):
        super().__init__(params)

    def publish(self, msg):
        msg_info = super().publish(msg)
        if msg_info.rc != MQTT_ERR_QUEUE_SIZE:
            msg_info.wait_for_publish()
        elif msg_info.rc == MQTT_ERR_NO_CONN:
            raise ConnectionError('client is not currently connected')
        return msg_info


class MqttAsyncWriter(BaseMqttWriter):
    def __init__(self, params):
        super().__init__(params)
        self._callbacks = OrderedDict()
        self._lock = Lock()
        self._mqttc.on_publish = self._on_publish

    def publish(self, msg):
        msg_info = super().publish(msg)

        def executor(resolve, reject):
            if msg_info.rc == MQTT_ERR_NO_CONN:
                reject(ConnectionError('client is not currently connected'))
            try:
                if msg_info.is_published():
                    resolve(msg_info)
                elif msg_info.rc != MQTT_ERR_SUCCESS:
                    reject(SinetError(f'mqtt error: rc={msg_info.rc}'))
                else:
                    self._add_on_publish(msg_info, resolve)
            except Exception as e:
                tb = exc_info()[2]
                reject(e, tb)

        return Promise(executor)

    def _on_publish(self, client, user_data, mid):
        cb = self._callbacks.pop(mid, None)
        if cb is not None:
            cb[0](cb[1])

    def _add_on_publish(self, msg_info, callback):
        with self._lock:
            self._callbacks[msg_info.mid] = (callback, msg_info)
