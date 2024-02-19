/*******************************************************************************
 * Copyright (c) 2020, 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.web.Console;
import org.eclipse.kura.web.UserManager;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtUserConfig;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtUserService;
import org.eclipse.kura.web.shared.validator.PasswordStrengthValidators;
import org.eclipse.kura.web.shared.validator.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GwtUserServiceImpl extends OsgiRemoteServiceServlet implements GwtUserService {

    private static final Logger logger = LoggerFactory.getLogger(GwtUserServiceImpl.class);

    private static final long serialVersionUID = 6065248347373180366L;
    private final UserManager userManager;

    public GwtUserServiceImpl(final UserManager userManager) {
        this.userManager = userManager;
    }

    @Override
    public void createUser(final GwtXSRFToken token, final String userName) throws GwtKuraException {
        checkXSRFToken(token);

        try {
            this.userManager.createUser(userName);
        } catch (KuraException e) {
            logger.warn("failed to create user", e);
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public void deleteUser(final GwtXSRFToken token, final String userName) throws GwtKuraException {
        checkXSRFToken(token);

        try {
            this.userManager.deleteUser(userName);
        } catch (KuraException e) {
            logger.warn("failed to delete user", e);
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Set<String> getDefinedPermissions(final GwtXSRFToken token) throws GwtKuraException {
        checkXSRFToken(token);

        try {
            return this.userManager.getDefinedPermissions();
        } catch (KuraException e) {
            logger.warn("failed to get defined permissions", e);
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Set<GwtUserConfig> getUserConfig(final GwtXSRFToken token) throws GwtKuraException {
        checkXSRFToken(token);

        try {
            return this.userManager.getUserConfig();
        } catch (KuraException e) {
            logger.warn("failed to get user configuration", e);
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public GwtUserConfig getUserConfigOrDefault(GwtXSRFToken token, String name) throws GwtKuraException {
        checkXSRFToken(token);

        try {
            return this.userManager.getUserDefaultConfig(name);
        } catch (KuraException e) {
            logger.warn("failed to get user configuration", e);
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public void setUserConfig(final GwtXSRFToken token, final Set<GwtUserConfig> userConfig) throws GwtKuraException {
        checkXSRFToken(token);

        for (final GwtUserConfig config : userConfig) {
            final Optional<String> newPassword = config.getNewPassword();

            if (newPassword.isPresent()) {
                validateUserPassword(newPassword.get());
            }
        }

        try {
            this.userManager.setUserConfig(userConfig);
        } catch (KuraException e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, null, e.getMessage());
        }
    }

    private void validateUserPassword(final String password) throws GwtKuraException {
        final List<Validator<String>> validators = PasswordStrengthValidators
                .fromConfig(Console.getConsoleOptions().getUserOptions());

        final List<String> errors = new ArrayList<>();

        for (final Validator<String> validator : validators) {
            validator.validate(password, errors::add);
        }

        if (!errors.isEmpty()) {
            logger.warn("password strenght requirements not satisfied: {}", errors);
            throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT);
        }
    }

}
