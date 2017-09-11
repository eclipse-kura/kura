/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.broker.artemis.core;

import java.util.HashSet;
import java.util.Set;

public class ServerConfiguration {

    private String securityDomain = "artemis";

    private String brokerXml;

    private Set<String> requiredProtocols = new HashSet<>();

    private UserAuthentication userAuthentication;

    public void setSecurityDomain(final String securityDomain) {
        this.securityDomain = securityDomain;
    }

    public String getSecurityDomain() {
        return this.securityDomain;
    }

    public void setBrokerXml(final String brokerXml) {
        this.brokerXml = brokerXml;
    }

    public String getBrokerXml() {
        return this.brokerXml;
    }

    public void setRequiredProtocols(final Set<String> requiredProtocols) {
        this.requiredProtocols = requiredProtocols;
    }

    public Set<String> getRequiredProtocols() {
        return this.requiredProtocols;
    }

    public void setUserAuthentication(UserAuthentication userAuthentication) {
        this.userAuthentication = userAuthentication;
    }

    public UserAuthentication getUserAuthentication() {
        return this.userAuthentication;
    }

}
