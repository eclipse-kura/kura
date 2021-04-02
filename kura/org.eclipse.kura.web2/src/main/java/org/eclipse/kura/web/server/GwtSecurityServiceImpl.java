/*******************************************************************************
 * Copyright (c) 2011, 2021 Eurotech and/or its affiliates and others
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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.security.SecurityService;
import org.eclipse.kura.security.ThreatManagerService;
import org.eclipse.kura.security.tamper.detection.TamperDetectionService;
import org.eclipse.kura.security.tamper.detection.TamperStatus;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtSecurityCapabilities;
import org.eclipse.kura.web.shared.model.GwtTamperStatus;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtSecurityService;

public class GwtSecurityServiceImpl extends OsgiRemoteServiceServlet implements GwtSecurityService {

    /**
     *
     */
    private static final long serialVersionUID = -7664408886756367054L;

    @Override
    public void reloadSecurityPolicyFingerprint(GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        SecurityService securityService = ServiceLocator.getInstance().getService(SecurityService.class);
        try {
            securityService.reloadSecurityPolicyFingerprint();
        } catch (KuraException e) {
            throw new GwtKuraException(e.getMessage());
        }

    }

    @Override
    public void reloadCommandLineFingerprint(GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        SecurityService securityService = ServiceLocator.getInstance().getService(SecurityService.class);
        try {
            securityService.reloadCommandLineFingerprint();
        } catch (KuraException e) {
            throw new GwtKuraException(e.getMessage());
        }
    }

    @Override
    public List<GwtTamperStatus> getTamperStatus(final GwtXSRFToken token) throws GwtKuraException {
        checkXSRFToken(token);

        final List<GwtTamperStatus> result = new ArrayList<>();

        ServiceLocator.applyToAllServices(TamperDetectionService.class, s -> {
            try {

                final TamperStatus tamperStatus = s.getTamperStatus();

                final Map<String, String> properties = tamperStatus.getProperties().entrySet().stream()
                        .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().getValue().toString()));

                result.add(new GwtTamperStatus(s.getDisplayName(), tamperStatus.isDeviceTampered(), properties));
            } catch (KuraException e) {
                throw new GwtKuraException(e.getMessage());
            }
        });

        return result;
    }

    @Override
    public void resetTamperStatus(final GwtXSRFToken token) throws GwtKuraException {
        checkXSRFToken(token);

        final AtomicReference<Optional<GwtKuraException>> e = new AtomicReference<>(Optional.empty());

        ServiceLocator.applyToAllServices(TamperDetectionService.class, s -> {
            try {
                s.resetTamperStatus();
            } catch (KuraException ex) {
                e.set(Optional.of(new GwtKuraException(ex.getMessage())));
            }
        });

        final Optional<GwtKuraException> ex = e.get();

        if (ex.isPresent()) {
            throw ex.get();
        }
    }

    @Override
    public GwtSecurityCapabilities getCababilities() {
        return new GwtSecurityCapabilities(isDebugMode(), isSecurityServiceAvailable(), isThreatManagerAvailable(),
                isTamperDetectionAvailable());
    }

    private boolean isSecurityServiceAvailable() {
        SecurityService securityService;

        try {
            securityService = ServiceLocator.getInstance().getService(SecurityService.class);
            if (securityService == null) {
                return false;
            }
        } catch (GwtKuraException e) {
            return false;
        }
        return true;
    }

    private boolean isTamperDetectionAvailable() {
        try {
            return !ServiceLocator.getInstance().getServiceReferences(TamperDetectionService.class, null).isEmpty();
        } catch (GwtKuraException e) {
            return false;
        }
    }

    private boolean isDebugMode() {
        SecurityService securityService;

        try {
            securityService = ServiceLocator.getInstance().getService(SecurityService.class);
            if (securityService != null) {
                return securityService.isDebugEnabled();
            }
        } catch (GwtKuraException e) {
            // Nothing to do
        }
        return false;
    }

    private boolean isThreatManagerAvailable() {

        try {
            ThreatManagerService threatManagerService = ServiceLocator.getInstance()
                    .getService(ThreatManagerService.class);
            if (threatManagerService != null) {
                return true;
            }
        } catch (GwtKuraException e) {
            return false;
        }
        return false;
    }
}
