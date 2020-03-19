/*
 * Copyright (C) 2020 National Institute of Informatics
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

package jp.ad.sinet.stream.plugins.kafka;

import jp.ad.sinet.stream.api.AuthenticationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

class SecurityProtocolTest extends ConfigFileWriter {

	private final String prefixService = "security-protocol-test-";

	/**
	 * 試験：セキュリティプロトコル SSL
	 */
	static Stream<Arguments> paramProviderSSL() {
		SecurityProtocol  protocol = SecurityProtocol.SSL;
		String port = "9093";
		String host = ConfigFileWriter.BROKER_HOST + ":" + port;
		String ip	= ConfigFileWriter.BROKER_IP + ":" + port;
		return Stream.of(
			//
			// 引数：	プロトコル:			SSL 固定
			//			ブローカー:			host, ip
			//			ca	:				ca.pem									※ 異常や null もやった方がよい
			//			cert:				client0.crt (正常), client1.crt (異常)	※ null もやった方がよい
			//			key::				client0.key (正常), client1.key (異常)	※ null もやった方がよい
			//			check_hostname::	null (パラメータを付けない), "true" (パラメータ true を付ける), "false" (パラメータ false を付ける)
			//			期待値(Exception)	null (正常終了), その他 Exception を指定
			//
			Arguments.of(protocol, host, "ca.pem", "client0.crt", "client0.key",  null,   null),
			Arguments.of(protocol, host, "ca.pem", "client0.crt", "client1.key",  null,   AuthenticationException.class),
			Arguments.of(protocol, host, "ca.pem", "client1.crt", "client1.key",  null,   AuthenticationException.class),
			Arguments.of(protocol, host, "ca.pem", "client1.crt", "client0.key",  null,   AuthenticationException.class),

			Arguments.of(protocol, host, "ca.pem", "client0.crt", "client0.key", "true",  null),
			Arguments.of(protocol, host, "ca.pem", "client0.crt", "client1.key", "true",  AuthenticationException.class),
			Arguments.of(protocol, host, "ca.pem", "client1.crt", "client1.key", "true",  AuthenticationException.class),
			Arguments.of(protocol, host, "ca.pem", "client1.crt", "client0.key", "true",  AuthenticationException.class),

			Arguments.of(protocol, host, "ca.pem", "client0.crt", "client0.key", "false", null),
			Arguments.of(protocol, host, "ca.pem", "client0.crt", "client1.key", "false", AuthenticationException.class),
			Arguments.of(protocol, host, "ca.pem", "client1.crt", "client1.key", "false", AuthenticationException.class),
			Arguments.of(protocol, host, "ca.pem", "client1.crt", "client0.key", "false", AuthenticationException.class),

			Arguments.of(protocol, ip,	 "ca.pem", "client0.crt", "client0.key",  null,   AuthenticationException.class),
			Arguments.of(protocol, ip,	 "ca.pem", "client0.crt", "client1.key",  null,   AuthenticationException.class),
			Arguments.of(protocol, ip,	 "ca.pem", "client1.crt", "client1.key",  null,   AuthenticationException.class),
			Arguments.of(protocol, ip,	 "ca.pem", "client1.crt", "client0.key",  null,   AuthenticationException.class),

			Arguments.of(protocol, ip,	 "ca.pem", "client0.crt", "client0.key", "true",  AuthenticationException.class),
			Arguments.of(protocol, ip,	 "ca.pem", "client0.crt", "client1.key", "true",  AuthenticationException.class),
			Arguments.of(protocol, ip,	 "ca.pem", "client1.crt", "client1.key", "true",  AuthenticationException.class),
			Arguments.of(protocol, ip,	 "ca.pem", "client1.crt", "client0.key", "true",  AuthenticationException.class),

			Arguments.of(protocol, ip,	 "ca.pem", "client0.crt", "client0.key", "false", null),
			Arguments.of(protocol, ip,	 "ca.pem", "client0.crt", "client1.key", "false", AuthenticationException.class),
			Arguments.of(protocol, ip,	 "ca.pem", "client1.crt", "client1.key", "false", AuthenticationException.class),
			Arguments.of(protocol, ip,	 "ca.pem", "client1.crt", "client0.key", "false", AuthenticationException.class)
		);
	}

	@ParameterizedTest
	@MethodSource("paramProviderSSL")
	void testSSL(SecurityProtocol protocol, String broker, String ca, String crt, String key, String check, Class<Throwable> expected) throws IOException {

		// 定数定義
		String label	= protocol.getLabel();
		String service	= prefixService + label;

		// コンフィグファイルを作成する
		super.writeConfigFile(Arrays.asList(
				service + ":",
				"  value_type: text",
				"  type: "				+ SERVICE_TYPE,
				"  topic: "				+ super.getTopic(label),
				"  security_protocol: "	+ protocol.getProtocol(),
				Objects.isNull(broker)	? "" : "  brokers: " +  broker,
				Objects.isNull(ca)		? "" : "  ssl_cafile:   ${" + ca  + "}",
				Objects.isNull(crt)		? "" : "  ssl_certfile: ${" + crt + "}",
				Objects.isNull(key)		? "" : "  ssl_keyfile:  ${" + key + "}",
				Objects.isNull(check)	? "" : "  ssl_check_hostname: " + check
		));

		// 試験を実行する
		super.execTest(service, expected);
	}

	/**
	 * 試験：セキュリティプロトコル PLAINTEXT
	 */
	static Stream<Arguments> paramProviderPLAINTEX() {
		SecurityProtocol  protocol = SecurityProtocol.PLAINTEXT;
		String port = "9092";
		String host = ConfigFileWriter.BROKER_HOST + ":" + port;
		String ip	= ConfigFileWriter.BROKER_IP + ":" + port;
		return Stream.of(
			//
			// 引数：	プロトコル:			PLAINTEXT 固定
			//			ブローカー:			host, ip
			//			期待値(Exception)	null (正常終了), その他 Exception を指定
			//
			// プロトコル, ブローカー, 期待値(Exception)
			Arguments.of(protocol, host, null),
			Arguments.of(protocol, ip,	 null)
		);
	}

	@ParameterizedTest
	@MethodSource("paramProviderPLAINTEX")
	void testPLAINTEXT(SecurityProtocol protocol, String broker, Class<Throwable> expected) throws IOException {

		// 定数定義
		String label	= protocol.getLabel();
		String service	= prefixService + label;

		// コンフィグファイルを作成する
		super.writeConfigFile(Arrays.asList(
				service + ":",
				"  value_type: text",
				"  type: "				+ SERVICE_TYPE,
				"  topic: "				+ super.getTopic(label),
				"  security_protocol: "	+ protocol.getProtocol(),
				"  brokers: "			+ broker
		));

		// 試験を実行する
		super.execTest(service, expected);
	}

	/**
	 * 試験：セキュリティプロトコル SASL
	 */
	static Stream<Arguments> paramProviderSASL_PLAINTEXT() {
		// SASL_PLAINTEXT
		return paramProviderSASL(SecurityProtocol.SASL_PLAINTEXT, "9096");
	}

	static Stream<Arguments> paramProviderSASL_SSL() {
		// SASL_SSL
		return paramProviderSASL(SecurityProtocol.SASL_SSL, "9097");
	}

	static private Stream<Arguments> paramProviderSASL(SecurityProtocol protocol, String port) {
		String host = ConfigFileWriter.BROKER_HOST + ":" + port;
		String ip	= ConfigFileWriter.BROKER_IP + ":" + port;
		List<Arguments> list = new ArrayList<>();

		//
		// 引数		CA証明書			"${ca.pem}", "xxxxxx", "", null
		//			mechanism:			ENUM(PLAN, SCRAM-SHA-256, SCRAM-SHA-512) +  xxxxxx, "", null
		//			broker:				host, ip, xxxxxx, "", null
		//			user:				user01, xxxxxx, "", null
		//			pass:				user01, xxxxxx, "", null
		//			check:				true, false, "", null
		//			期待値(Exception)	null (正常終了), その他 Exception を指定
		//
		//			※ null はパラメータ自体を設定しない
		//
		// (例)		list.add(Arguments.of(host, mechanism, "user01",	"user01",	null));
		//			list.add(Arguments.of(host, mechanism, "uesr01",	"xxxxxx",	AuthenticationException.class));
		//			list.add(Arguments.of(host, mechanism, "uesr01",	 null,		InvalidConfigurationException.class));
		//

		// CA証明書でループする
		for (String ca : new String[]{"${ca.pem}", "xxxxxx", "", null}) {

			// チェックホストでループ
			for (String check : new String[]{"true", "false", "", null}) {

				// メカニズムの ENUM でループする
				Stream.of(SaslMechanism.values()).forEach(m -> {
					String mechanism = m.getMechanism();

					// ブローカーでループ
					Stream.of(host, ip, "xxxxxx", "", null).forEach( broker -> {

						// ユーザーでループ
						Stream.of("user01", "xxxxxx", "", null).forEach( user -> {

							// パスワードでループ
							Stream.of("user01", "xxxxxx", "", null).forEach( pass -> {

								// 正常値を調べる
								boolean valid_c = !Objects.isNull(ca) && ca.equals("${ca.pem}");
								boolean valid_b = !Objects.isNull(broker) && (broker.equals(host) || broker.equals(ip));
								boolean valid_u = !Objects.isNull(user) && user.equals("user01");
								boolean valid_p = !Objects.isNull(pass) && pass.equals("user01");
								boolean false_h = !Objects.isNull(check) && check.equals("false");

								// 期待値を設定する (初期値はとりあえず Exception)
								Class<? extends Throwable> expected = Exception.class;

								// 全て正常値の時は期待値を正常に設定
								if (protocol == SecurityProtocol.SASL_PLAINTEXT) {
									// SASL_PLAINTEXT はCA証明書を含めない
									if (valid_b && valid_u && valid_p) {
										expected = null;
									}
								} else {
									// SASL_SSL はCA証明書も含める
									if (valid_c && valid_b && valid_u && valid_p && broker.equals(host)) {
										// ブローカーがホストの時 ssl_check_hostname 不要
										expected = null;
									} else
									if (valid_c && valid_b && valid_u && valid_p && broker.equals(ip) && false_h) {
										// ブローカーがIPの時 ssl_check_hostname = false が必要になる
										expected = null;
									}
								}

								// 試験用のパラメータを追加する。
								list.add(Arguments.of(protocol, broker, ca, mechanism, user, pass, check, expected));
							});
						});
					});
				});

				if (protocol == SecurityProtocol.SASL_PLAINTEXT) {
					// SASL_PLAINTEXT は check_host のループは行わない
					break;
				}
			}

			if (protocol == SecurityProtocol.SASL_PLAINTEXT) {
				// SASL_PLAINTEXT はCA証明書のループは行わない
				break;
			}
		}

		// 不正メカニズムの試験を追加
		list.add(Arguments.of(protocol, host, "${ca.pem}", "xxxxxx",	"user01", "user01", null, Exception.class));	
		list.add(Arguments.of(protocol, host, "${ca.pem}", ""	,		"user01", "user01", null, Exception.class));
		list.add(Arguments.of(protocol, host, "${ca.pem}", null,		"user01", "user01", null, Exception.class));

		return list.stream();
	}

	@ParameterizedTest
	@MethodSource("paramProviderSASL_PLAINTEXT")
	void testSASL_PLAINTEXT(SecurityProtocol protocol, String broker, String ca, String mech, String user, String pass, String check, Class<Throwable> expected) throws IOException {

		// 定数定義
		String label	= protocol.getLabel();
		String service	= prefixService + label;

		// コンフィグファイルを作成する
		super.writeConfigFile(Arrays.asList(
				service + ":",
				"  value_type: text",
				"  type: "				+ SERVICE_TYPE,
				"  topic: "				+ super.getTopic(label),
				"  security_protocol: "	+ protocol.getProtocol(),
				Objects.isNull(broker)	? "" : "  brokers: "             + broker,
				Objects.isNull(mech)	? "" : "  sasl_mechanism: "      + mech,
				Objects.isNull(user)	? "" : "  sasl_plain_username: " + user,
				Objects.isNull(pass)	? "" : "  sasl_plain_password: " + pass
		));

		// 試験を実行する
		super.execTest(service, expected);
	}

	@ParameterizedTest
	@MethodSource("paramProviderSASL_SSL")
	void testSASL_SSL(SecurityProtocol protocol, String broker, String ca, String mech, String user, String pass, String check, Class<Throwable> expected) throws IOException {

		// 定数定義
		String label	= protocol.getLabel();
		String service	= prefixService + label;

		// コンフィグファイルを作成する
		super.writeConfigFile(Arrays.asList(
				service + ":",
				"  value_type: text",
				"  type: "				+ SERVICE_TYPE,
				"  topic: "				+ super.getTopic(label),
				"  security_protocol: "	+ protocol.getProtocol(),
				Objects.isNull(broker)	? "" : "  brokers: "             + broker,
				Objects.isNull(ca)		? "" : "  ssl_cafile: "          + ca,
				Objects.isNull(mech)	? "" : "  sasl_mechanism: "      + mech,
				Objects.isNull(user)	? "" : "  sasl_plain_username: " + user,
				Objects.isNull(pass)	? "" : "  sasl_plain_password: " + pass,
				Objects.isNull(check)	? "" : "  ssl_check_hostname: " + check
		));

		// 試験を実行する
		super.execTest(service, expected);
	}
}
