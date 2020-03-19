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

import lombok.Getter;

public enum SecurityProtocol {

	SSL("SSL", true, false, false),
	PLAINTEXT("PLAINTEXT", false, false, true),
	SASL_PLAINTEXT("SASL_PLAINTEXT", false, true, true),
	SASL_SSL("SASL_SSL", false, true, false);

	@Getter
	private final String protocol;

	@Getter
	private final String label;	// service の命名用を想定

    @Getter
	private final boolean ssl;
    @Getter
	private final boolean sasl;
    @Getter
	private final boolean plain;

	SecurityProtocol(String protocol, boolean ssl, boolean sasl, boolean plain) {
		this.protocol = protocol;
		this.label = protocol.toLowerCase().replaceAll("_", "-");
		this.ssl   = ssl;
		this.sasl  = sasl;
		this.plain = plain;
	}
}

