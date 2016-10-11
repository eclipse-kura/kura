/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
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

    private static ChapLinux s_instance = null;

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
        if (s_instance == null) {
            s_instance = new ChapLinux();
        }

        return s_instance;
    }
}
