/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.core.tamper.detection.util;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.tamper.detection.model.TamperDetectionServiceInfo;
import org.eclipse.kura.core.tamper.detection.model.TamperStatusInfo;
import org.eclipse.kura.security.tamper.detection.TamperDetectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TamperDetectionRemoteService {

    private static final Logger logger = LoggerFactory.getLogger(TamperDetectionRemoteService.class);
    private final Map<String, TamperDetectionService> tamperDetectionServices = new ConcurrentHashMap<>();

    public void setTamperDetectionService(final TamperDetectionService tamperDetectionService,
            final Map<String, Object> properties) {
        final Optional<String> pid = getPid(properties);

        if (pid.isPresent()) {
            this.tamperDetectionServices.put(pid.get(), tamperDetectionService);
        } else {
            logger.warn("Tamper detection service must set either service.pid or kura.service.pid");
        }

    }

    public void unsetTamperDetectionService(final TamperDetectionService tamperDetectionService,
            final Map<String, Object> properties) {
        getPid(properties).ifPresent(this.tamperDetectionServices::remove);
    }

    protected List<TamperDetectionServiceInfo> listTamperDetectionServicesInternal() {
        return tamperDetectionServices.entrySet().stream()
                .map(e -> new TamperDetectionServiceInfo(e.getKey(), e.getValue().getDisplayName()))
                .collect(Collectors.toList());
    }

    protected TamperStatusInfo getTamperStatusInternal(final String pid) throws KuraException {

        final TamperDetectionService service = this.tamperDetectionServices.get(pid);

        if (service == null) {
            throw new KuraException(KuraErrorCode.NOT_FOUND);
        }

        return new TamperStatusInfo(service.getTamperStatus());
    }

    public void resetTamperStatusInternal(final String pid) throws KuraException {
        final TamperDetectionService service = this.tamperDetectionServices.get(pid);

        if (service == null) {
            throw new KuraException(KuraErrorCode.NOT_FOUND);
        }

        service.resetTamperStatus();
    }

    private static final Optional<String> getPid(final Map<String, Object> properties) {
        final Object kuraServicePid = properties.get("kura.service.pid");

        if (kuraServicePid instanceof String) {
            return Optional.of((String) kuraServicePid);
        }

        final Object servicePid = properties.get("service.pid");

        if (servicePid instanceof String) {
            return Optional.of((String) servicePid);
        }

        return Optional.empty();
    }
}
