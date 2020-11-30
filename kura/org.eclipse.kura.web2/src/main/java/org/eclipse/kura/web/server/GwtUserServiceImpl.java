/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.web.server;

import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.web.UserManager;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtUserConfig;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtUserService;

public class GwtUserServiceImpl extends OsgiRemoteServiceServlet implements GwtUserService {

    private static final long serialVersionUID = 6065248347373180366L;
    private final UserManager userManager;

    public GwtUserServiceImpl(final UserManager userManager) {
        this.userManager = userManager;
    }

    @Override
    public void createUser(final GwtXSRFToken token, final String userName) throws GwtKuraException {
        checkXSRFToken(token);

        userManager.createUser(userName);
    }

    @Override
    public void deleteUser(final GwtXSRFToken token, final String userName) throws GwtKuraException {
        checkXSRFToken(token);

        userManager.deleteUser(userName);
    }

    @Override
    public void setUserPassword(final GwtXSRFToken token, final String userName, final String password)
            throws GwtKuraException {
        checkXSRFToken(token);

        try {
            userManager.setUserPassword(userName, password);
        } catch (KuraException e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    @Override
    public Set<String> getDefinedPermissions(final GwtXSRFToken token) throws GwtKuraException {
        checkXSRFToken(token);

        return userManager.getDefinedPermissions();
    }

    @Override
    public Set<GwtUserConfig> getUserConfig(final GwtXSRFToken token) throws GwtKuraException {
        checkXSRFToken(token);

        return userManager.getUserConfig();
    }

    @Override
    public void setUserConfig(final GwtXSRFToken token, final Set<GwtUserConfig> userConfig) throws GwtKuraException {
        checkXSRFToken(token);

        try {
            userManager.setUserConfig(userConfig);
        } catch (KuraException e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }
}