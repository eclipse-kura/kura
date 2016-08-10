/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.internal.driver.opcua;

import static org.eclipse.kura.Preconditions.checkNull;

import java.util.Map;

import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.S7PlcMessages;
import org.eclipse.kura.util.base.StringUtil;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.UsernameProvider;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;

/**
 * The Class OpcUaOptions is responsible to provide all the required
 * configurable options for the OPC-UA Driver.<br/>
 * <br/>
 *
 * The different properties to configure a OPC-UA Driver are as follows:
 * <ul>
 * <li>endpoint.ip</li>
 * <li>endpoint.port</li>
 * <li>server.name</li>
 * </ul>
 */
final class OpcUaOptions {

	/**
	 * Configurable Property to set OPC-UA application certificate
	 */
	private static final String APPLICATION_CERTIFICATE = "certificate.location";

	/**
	 * Configurable Property to set OPC-UA application name
	 */
	private static final String APPLICATION_NAME = "application.name";

	/**
	 * Configurable Property to set OPC-UA application uri
	 */
	private static final String APPLICATION_URI = "application.uri";

	/** OPC-UA Endpoint IP */
	private static final String IP = "endpoint.ip";

	/**
	 * Configurable property to set client alias for the keystore
	 */
	private static final String KEYSTORE_CLIENT_ALIAS = "keystore.client.alias";

	/**
	 * Configurable Property to set keystore password
	 */
	private static final String KEYSTORE_PASSWORD = "keystore.password";

	/**
	 * Configurable Property to set server alias for the keystore
	 */
	private static final String KEYSTORE_SERVER_ALIAS = "keystore.server.alias";

	/**
	 * Configurable Property to set keystore type
	 */
	private static final String KEYSTORE_TYPE = "keystore.type";

	/**
	 * Configurable Property to OPC-UA server password
	 */
	private static final String PASSWORD = "password";

	/** OPC-UA Endpoint Port */
	private static final String PORT = "endpoint.port";

	/**
	 * Configurable property specifying the request timeout
	 */
	private static final String REQUEST_TIMEOUT = "request.timeout";

	/** Localization Resource. */
	private static final S7PlcMessages s_message = LocalizationAdapter.adapt(S7PlcMessages.class);

	/**
	 * Configurable property specifying the Security Policy
	 */
	private static final String SECURITY_POLICY = "security.policy";

	/** OPC-UA Server Name */
	private static final String SERVER_NAME = "server.name";

	/**
	 * Configurable property specifying the session timeout
	 */
	private static final String SESSION_TIMEOUT = "session.timeout";

	/**
	 * Configurable Property to set OPC-UA server username
	 */
	private static final String USERNAME = "password";

	/** The properties as associated */
	private final Map<String, Object> m_properties;

	/**
	 * Instantiates a new S7 PLC options.
	 *
	 * @param properties
	 *            the properties
	 */
	OpcUaOptions(final Map<String, Object> properties) {
		checkNull(properties, s_message.propertiesNonNull());
		this.m_properties = properties;
	}

	/**
	 * Returns the OPC-UA Application Certificate
	 *
	 * @return the OPC-UA Application Certificate
	 */
	String getApplicationCertificate() {
		String applicationCert = null;
		if ((this.m_properties != null) && this.m_properties.containsKey(APPLICATION_CERTIFICATE)
				&& (this.m_properties.get(APPLICATION_CERTIFICATE) != null)) {
			applicationCert = this.m_properties.get(APPLICATION_CERTIFICATE).toString();
		}
		return applicationCert;
	}

	/**
	 * Returns the OPC-UA Application Name
	 *
	 * @return the OPC-UA Application Name
	 */
	String getApplicationName() {
		String applicationName = null;
		if ((this.m_properties != null) && this.m_properties.containsKey(APPLICATION_NAME)
				&& (this.m_properties.get(APPLICATION_NAME) != null)) {
			applicationName = this.m_properties.get(APPLICATION_NAME).toString();
		}
		return applicationName;
	}

	/**
	 * Returns the OPC-UA Application URI
	 *
	 * @return the OPC-UA Application URI
	 */
	String getApplicationUri() {
		String applicationUri = null;
		if ((this.m_properties != null) && this.m_properties.containsKey(APPLICATION_URI)
				&& (this.m_properties.get(APPLICATION_URI) != null)) {
			applicationUri = this.m_properties.get(APPLICATION_URI).toString();
		}
		return applicationUri;
	}

	/**
	 * Returns the OPC-UA Identity Provider
	 *
	 * @return the OPC-UA Identity Provider
	 */
	IdentityProvider getIdentityProvider() {
		IdentityProvider identityProvider = null;
		final String username = this.getUsername();
		final String password = this.getPassword();
		if (StringUtil.isNullOrEmpty(username) && StringUtil.isNullOrEmpty(password)) {
			identityProvider = new AnonymousProvider();
		} else {
			identityProvider = new UsernameProvider(username, password);
		}
		return identityProvider;
	}

	/**
	 * Returns the OPC-UA Endpoint IP
	 *
	 * @return the OPC-UA Endpoint IP
	 */
	String getIp() {
		String ipAddress = null;
		if ((this.m_properties != null) && this.m_properties.containsKey(IP) && (this.m_properties.get(IP) != null)) {
			ipAddress = this.m_properties.get(IP).toString();
		}
		return ipAddress;
	}

	/**
	 * Returns the Keystore Client Alias
	 *
	 * @return the Keystore Client Alias
	 */
	String getKeystoreClientAlias() {
		String clientAlias = null;
		if ((this.m_properties != null) && this.m_properties.containsKey(KEYSTORE_CLIENT_ALIAS)
				&& (this.m_properties.get(KEYSTORE_CLIENT_ALIAS) != null)) {
			clientAlias = this.m_properties.get(KEYSTORE_CLIENT_ALIAS).toString();
		}
		return clientAlias;
	}

	/**
	 * Returns the Keystore Password
	 *
	 * @return the Keystore Password
	 */
	String getKeystorePassword() {
		String password = null;
		if ((this.m_properties != null) && this.m_properties.containsKey(KEYSTORE_PASSWORD)
				&& (this.m_properties.get(KEYSTORE_PASSWORD) != null)) {
			password = this.m_properties.get(KEYSTORE_PASSWORD).toString();
		}
		return password;
	}

	/**
	 * Returns the Keystore Server Alias
	 *
	 * @return the Keystore Server Alias
	 */
	String getKeystoreServerAlias() {
		String serverAlias = null;
		if ((this.m_properties != null) && this.m_properties.containsKey(KEYSTORE_SERVER_ALIAS)
				&& (this.m_properties.get(KEYSTORE_SERVER_ALIAS) != null)) {
			serverAlias = this.m_properties.get(KEYSTORE_SERVER_ALIAS).toString();
		}
		return serverAlias;
	}

	/**
	 * Returns the Keystore Type
	 *
	 * @return the Keystore Type
	 */
	String getKeystoreType() {
		String keystoreType = null;
		if ((this.m_properties != null) && this.m_properties.containsKey(KEYSTORE_TYPE)
				&& (this.m_properties.get(KEYSTORE_TYPE) != null)) {
			keystoreType = this.m_properties.get(KEYSTORE_TYPE).toString();
		}
		return keystoreType;
	}

	/**
	 * Returns the OPC-UA Password
	 *
	 * @return the OPC-UA Password
	 */
	String getPassword() {
		String password = null;
		if ((this.m_properties != null) && this.m_properties.containsKey(PASSWORD)
				&& (this.m_properties.get(PASSWORD) != null)) {
			password = this.m_properties.get(PASSWORD).toString();
		}
		return password;
	}

	/**
	 * Returns OPC-UA Endpoint Port
	 *
	 * @return the OPC-UA Endpoint Port
	 */
	int getPort() {
		int port = 0;
		if ((this.m_properties != null) && this.m_properties.containsKey(PORT)
				&& (this.m_properties.get(PORT) != null)) {
			port = Integer.valueOf(this.m_properties.get(PORT).toString());
		}
		return port;
	}

	/**
	 * Returns OPC-UA Request Timeout
	 *
	 * @return the OPC-UA Request Timeout
	 */
	long getRequestTimeout() {
		long requestTimeout = 0;
		if ((this.m_properties != null) && this.m_properties.containsKey(REQUEST_TIMEOUT)
				&& (this.m_properties.get(REQUEST_TIMEOUT) != null)) {
			requestTimeout = Long.valueOf(this.m_properties.get(REQUEST_TIMEOUT).toString());
		}
		return requestTimeout * 1000;
	}

	/**
	 * Returns the Security Policy
	 *
	 * @return the Security Policy
	 */
	SecurityPolicy getSecurityPolicy() {
		int securityPolicy = 0;
		if ((this.m_properties != null) && this.m_properties.containsKey(SECURITY_POLICY)
				&& (this.m_properties.get(SECURITY_POLICY) != null)) {
			securityPolicy = Integer.parseInt(this.m_properties.get(SECURITY_POLICY).toString());
		}
		switch (securityPolicy) {
		case 1:
			return SecurityPolicy.Basic128Rsa15;
		case 2:
			return SecurityPolicy.Basic256;
		case 3:
			return SecurityPolicy.Basic256Sha256;
		default:
			return SecurityPolicy.None;
		}
	}

	/**
	 * Returns the OPC-UA Server Name
	 *
	 * @return the OPC-UA Server Name
	 */
	String getServerName() {
		String serverName = null;
		if ((this.m_properties != null) && this.m_properties.containsKey(SERVER_NAME)
				&& (this.m_properties.get(SERVER_NAME) != null)) {
			serverName = this.m_properties.get(SERVER_NAME).toString();
		}
		return serverName;
	}

	/**
	 * Returns OPC-UA Session Timeout
	 *
	 * @return the OPC-UA Session Timeout
	 */
	long getSessionTimeout() {
		long sessionTimeout = 0;
		if ((this.m_properties != null) && this.m_properties.containsKey(SESSION_TIMEOUT)
				&& (this.m_properties.get(SESSION_TIMEOUT) != null)) {
			sessionTimeout = Long.valueOf(this.m_properties.get(SESSION_TIMEOUT).toString());
		}
		return sessionTimeout * 1000;
	}

	/**
	 * Returns the OPC-UA Username
	 *
	 * @return the OPC-UA Username
	 */
	String getUsername() {
		String username = null;
		if ((this.m_properties != null) && this.m_properties.containsKey(USERNAME)
				&& (this.m_properties.get(USERNAME) != null)) {
			username = this.m_properties.get(USERNAME).toString();
		}
		return username;
	}

}
