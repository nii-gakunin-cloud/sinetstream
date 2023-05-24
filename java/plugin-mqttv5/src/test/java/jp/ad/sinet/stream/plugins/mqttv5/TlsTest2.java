/*
 * Copyright (C) 2020 National Institute of Informatics
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.	See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package jp.ad.sinet.stream.plugins.mqttv5;

import jp.ad.sinet.stream.api.ConnectionException;
import jp.ad.sinet.stream.api.InvalidConfigurationException;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@EnabledIfEnvironmentVariable(named="RUN_INTERGRATION_TEST", matches = "(?i)true")
class TlsTest2 extends ConfigFileWriter {

	private final String prefixService = "tls-test-";

	/**
	 * 試験：SSL/TLS+証明書によるクライアント認証
	 */
	static Stream<Arguments> paramProviderTlsClientAuth() {
		String host = System.getenv().getOrDefault("MQTT_SSL_CERT_AUTH_BROKER", "broker:8885");
		String ip = System.getenv().getOrDefault("MQTT_SSL_CERT_AUTH_BROKER_IP", "127.0.0.1:8885");
		List<Arguments> list = new ArrayList<>();

		//
		// 引数：	ブローカー:	host, ip, "xxxxxx" (異常), "" (空文字), null (パラメータなし)
		//			ca:			${cacert.pem} (正常), "xxxxxx" (異常), "" (空文字), null (パラメータなし)
		//			crt:		${client0.crt} (正常), ${bad-client.crt} (異常), "" (空文字), null (パラメータなし)
		//			key:		${client0-enc.key} (正常), ${bad-client.key} (異常), "" (空文字), null (パラメータなし)
		//			pass:		client1-pass (正常), "xxxxxx" (異常), "" (空文字), null (パラメータなし)
		//			insecure:	"true" (IPで接続)
		//			ver:		"TLSv1.2" (正常), "TLSv1.0" (不正), "xxxxxx" (不正), "" (空文字)
		//			期待値(Exception)	null (正常終了), その他 Exception を指定
		//
		//	(例)
		//	Arguments.of(host, ${cacert.pem}, ${bad-client.crt}, ${bad-client.key}, bad-client-pass, AuthenticationException.class)
		//


		//
		// TODO パスフレーズ付きの key を作成する。
		//		key を作成するまで認証のループはコメントアウトしておく
		//

//		// CA でループ
//		Stream.of("${cacert.pem}", "xxxxxx", "", null).forEach( ca -> {
//
//			// CRT でループ
//			Stream.of("${client0.crt}", "${bad-client.crt}", "", null).forEach( crt -> {
//
//				// KEY でループ
//				Stream.of("${client0-enc.key}", "${bad-client.key}", "", null).forEach( key -> {
//
//					// PASS でループ
//					Stream.of("client1-pass", "xxxxxx", "", null).forEach( pass -> {
//
//						// 正常値を調べる
//						boolean valid_c = (!Objects.isNull(ca)     &&  ca.equals("${cacert.pem}")) ? true : false;
//						boolean valid_r = (!Objects.isNull(crt)    &&  crt.equals("${client0.crt}")) ? true : false;
//						boolean valid_k = (!Objects.isNull(key)    &&  key.equals("${client0-enc.key}")) ? true : false;
//						boolean valid_p = (!Objects.isNull(pass)   &&  pass.equals("client1-pass"))  ? true : false;
//
//						// 期待値を設定する (初期値はとりあえず Exception)
//						Class expected = AuthenticationException.class;
//
//						// 全て正常値の時は期待値を正常に設定
//						if (valid_c && valid_r && valid_k && valid_p) {
//							expected = null;
//						}
//
//						// 試験用のパラメータを追加する。
//						list.add(Arguments.of(host, ca, crt, key, pass, null, null, expected));
//					});
//				});
//			});
//		});

	//	// パスワード不正でも接続できた。 (key にパスフレーズが設定されていないらしい)
	//	list.add(Arguments.of(host,  "${cacert.pem}", "${client0.crt}", "${client0-enc.key}", "xxxxxx", null, null, null));

		// ブローカーの試験を追加する。 (IPは insecure も試す)
		list.add(Arguments.of(ip,    "${cacert.pem}", "${client0.crt}", "${client0-enc.key}", "client1-pass", null,		null, ConnectionException.class));
		list.add(Arguments.of(ip,    "${cacert.pem}", "${client0.crt}", "${client0-enc.key}", "client1-pass", "",		null, InvalidConfigurationException.class));
		list.add(Arguments.of(ip,    "${cacert.pem}", "${client0.crt}", "${client0-enc.key}", "client1-pass", "xxx",	null, InvalidConfigurationException.class));
		list.add(Arguments.of(ip,    "${cacert.pem}", "${client0.crt}", "${client0-enc.key}", "client1-pass", "true",	null, null));
		list.add(Arguments.of(ip,    "${cacert.pem}", "${client0.crt}", "${client0-enc.key}", "client1-pass", "false",	null, ConnectionException.class));
		list.add(Arguments.of("xxx", "${cacert.pem}", "${client0.crt}", "${client0-enc.key}", "client1-pass", null,		null, ConnectionException.class));
		list.add(Arguments.of("",    "${cacert.pem}", "${client0.crt}", "${client0-enc.key}", "client1-pass", null,		null, InvalidConfigurationException.class));
		list.add(Arguments.of(null,  "${cacert.pem}", "${client0.crt}", "${client0-enc.key}", "client1-pass", null,		null, InvalidConfigurationException.class));

		// tls_version の試験を追加する。(AuthenticationException -> ConnectionException に変更)
		list.add(Arguments.of(host,  "${cacert.pem}", "${client0.crt}", "${client0-enc.key}", "client1-pass", null, "TLSv1.2", null));
		list.add(Arguments.of(host,  "${cacert.pem}", "${client0.crt}", "${client0-enc.key}", "client1-pass", null, "TLSv1.1", null));
		list.add(Arguments.of(host,  "${cacert.pem}", "${client0.crt}", "${client0-enc.key}", "client1-pass", null, "TLSv1.0", ConnectionException.class));
		list.add(Arguments.of(host,  "${cacert.pem}", "${client0.crt}", "${client0-enc.key}", "client1-pass", null, "xxxxxx",  ConnectionException.class));
		list.add(Arguments.of(host,  "${cacert.pem}", "${client0.crt}", "${client0-enc.key}", "client1-pass", null, "",        null));

		// 現在 267 件
		return list.stream();
	}

	@ParameterizedTest
	@MethodSource("paramProviderTlsClientAuth")
	void testTlsClientAuth(String broker, String ca, String crt, String key, String pass, String insecure, String ver, Class<Throwable> expected) throws IOException {

		// 定数定義
		String label	= "ssl-cilent-authentication";
		String service	= prefixService + label;

		// コンフィグファイルを作成する
		writeConfigFile(Arrays.asList(
				service + ":",
				"  type: "				+ serviceType,
				"  protocol: "				+ protocol,
				"  topic: "				+ getTopic(label),
				Objects.isNull(broker)	? "" : "  brokers: "			+  broker,
			//	"  tls_set: ",
				"  tls: ",
				Objects.isNull(ca)			? "" : "    ca_certs: "			+  ca,
				Objects.isNull(crt)			? "" : "    certfile: "			+  crt,
				Objects.isNull(key)			? "" : "    keyfile: "			+  key,
				Objects.isNull(pass)		? "" : "    keyfilePassword: "	+  pass,
				Objects.isNull(ver)			? "" : "    tls_version: "		+  ver,
				Objects.isNull(insecure)	? "" : "  tls_insecure_set:",
				Objects.isNull(insecure)	? "" : "    value: "			+ insecure
		));

		// 正常試験は write read を実行する
		if (Objects.isNull(expected)) {
			testWriteRead(service, expected);
		}

		// 異常試験は write read を個別に実行する
		else {
			testWrite(service, expected);
			testRead(service, expected);
		}
	}
}
