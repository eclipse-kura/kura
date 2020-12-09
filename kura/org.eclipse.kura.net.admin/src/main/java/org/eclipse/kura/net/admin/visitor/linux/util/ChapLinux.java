/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.admin.visitor.linux.util;

/**
 * Defines CHAP authentication in Linux
 */
public class ChapLinux extends PppAuthSecrets {

    /*
     * #Secrets for authentication using CHAP
     * #client server secret IP addresses Provider
     * ISP@CINGULARGPRS.COM * CINGULAR1 * #att
     * mobileweb * password * #o2
     * user * pass * #orange
     * web * web * #vodaphone
     */

    private static final String CHAP_SECRETS_FILE = "/etc/ppp/chap-secrets";

    private static ChapLinux instance = null;

    /**
     * ChapLinux constructor
     */
    private ChapLinux() {
        super(CHAP_SECRETS_FILE);
    }

    /**
     * Get a singleton instance
     *
     * @return ChapLinux
     */
    public static ChapLinux getInstance() {
        if (instance == null) {
            instance = new ChapLinux();
        }

        return instance;
    }
}
