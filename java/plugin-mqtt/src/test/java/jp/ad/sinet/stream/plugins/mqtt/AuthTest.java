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

package jp.ad.sinet.stream.plugins.mqtt;

import jp.ad.sinet.stream.api.AuthenticationException;
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

@EnabledIfEnvironmentVariable(named="MQTT_USER_PASSWD_BROKER", matches = ".+")
class AuthTest extends ConfigFileWriter {

	/**
	 * 試験：平文パスワード
	 */
	static Stream<Arguments> paramProviderPassword() {
		String host = System.getenv().getOrDefault("MQTT_USER_PASSWD_BROKER", "broker:1884");
		String ip = System.getenv().getOrDefault("MQTT_USER_PASSWD_BROKER_IP", "127.0.0.1:1884");
		List<Arguments> list = new ArrayList<>();

		//
		// 引数：	ブローカー:			host, ip
		//			user	:			user01 (正常), xxxxxx (異常), "" (空文字), null (パラメータなし)
		//			pass:				user01 (正常), xxxxxx (異常), "" (空文字), null (パラメータなし)
		//			期待値(Exception)	null (正常終了), その他 Exception を指定
		//
		//	(例)
		//	Arguments.of(host, "user01", "user01",	null)
		//	Arguments.of(host, "user01", "xxxxxx",	AuthenticationException.class)
		//	Arguments.of(ip,   "user01", null,		AuthenticationException.class)

		// ユーザーでループ
		Stream.of("user01", "xxxxxx", "", null).forEach( user -> {

			// パスワードでループ
			Stream.of("user01", "xxxxxx", "", null).forEach( pass -> {

				// 正常値を調べる
				boolean valid_u = !Objects.isNull(user) && user.equals("user01");
				boolean valid_p = !Objects.isNull(pass) && pass.equals("user01");

				// 期待値を設定する (初期値はとりあえず Exception)
				Class<? extends Throwable> expected = AuthenticationException.class;

				// 全て正常値の時は期待値を正常に設定
				if (valid_u && valid_p) {
					expected = null;
				}

				// 試験用のパラメータを追加する。
				list.add(Arguments.of(host, user, pass, expected));
			});
		});

		// ブローカーの試験を追加する。
		list.add(Arguments.of(ip,    "user01", "user01", null));
		list.add(Arguments.of("xxx", "user01", "user01", ConnectionException.class));
		list.add(Arguments.of("",    "user01", "user01", InvalidConfigurationException.class));
		list.add(Arguments.of(null,  "user01", "user01", InvalidConfigurationException.class));

		return list.stream();
	}

	@ParameterizedTest
	@MethodSource("paramProviderPassword")
	void testPassword(String broker, String user, String pass, Class<Throwable> expected) throws IOException {

		// 定数定義
		String label	= "password";
		String prefixService = "authentication-test-";
		String service	= prefixService + label;

		// コンフィグファイルを作成する
		writeConfigFile(Arrays.asList(
				service + ":",
				"  type: "				+ serviceType,
				"  topic: "				+ getTopic(label),
				Objects.isNull(broker)	? "" : "  brokers: " +  broker,
				"  username_pw_set: ",
				Objects.isNull(user)	? "" : "    username: " +  user,
				Objects.isNull(pass)	? "" : "    password: " +  pass
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

		// 下の追加テストも実施する
		// execTest(service, expected, this::testAdditional);
	}

//	// 追加試験用のコールバックメソッド
//	private void testAdditional(MessageWriter<?> writer, MessageReader<?> reader) {
//		System.out.println("★★★★★★★★★★★★");
//		assertNotNull(reader.getConfig().get("topic"), "reader.getConfig().get(\"topic\")");
//	}
}
