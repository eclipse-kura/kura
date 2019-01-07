/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.web.server.util.ServiceLocator;

public class AuthenticationManager {

    private static AuthenticationManager instance;

    private char[] password;
    private String username;

    public static AuthenticationManager getInstance() {
        if (instance == null) {
            instance = new AuthenticationManager();
        }
        return instance;
    }

    protected void setUsername(String username) {
        this.username = username;
    }

    protected void setPassword(char[] psw) {
        this.password = psw;
    }

    public boolean authenticate(String username, String password) throws KuraException {
        requireNonNull(this.username);
        requireNonNull(this.password);
        
        try {
            CryptoService cryptoService = ServiceLocator.getInstance().getService(CryptoService.class);
            String sha1Password = cryptoService.sha1Hash(password);
            boolean isUsernameMatching = username.equals(this.username);
            boolean isPasswordMatching = Arrays.equals(sha1Password.toCharArray(), this.password);
            return isUsernameMatching && isPasswordMatching;
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.SECURITY_EXCEPTION);
        }
    }
}
