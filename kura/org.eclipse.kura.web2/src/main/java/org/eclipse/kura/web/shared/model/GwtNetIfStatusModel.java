/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.shared.model;

import org.eclipse.kura.web.client.util.KuraBaseModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GwtNetIfStatusModel extends KuraBaseModel {

    private static final long serialVersionUID = 2779596516813518500L;
    
    private static Logger logger = LoggerFactory.getLogger(GwtNetIfStatusModel.class);

    public static final String NAME = "name";
    public static final String STATUS = "status";
    public static final String TOOLTIP = "tooltip";

    protected GwtNetIfStatusModel() {

    }

    public GwtNetIfStatusModel(GwtNetIfStatus status, String name, String tooltip) {
        set(STATUS, status.name());
        set(NAME, name);
        set(TOOLTIP, tooltip);
    }

    public String getName() {
        return get(NAME);
    }

    public GwtNetIfStatus getStatus() {
        GwtNetIfStatus status = null;
        String statusStr = get(STATUS);

        try {
            status = GwtNetIfStatus.valueOf(statusStr);
        } catch (Exception e) {
            logger.warn("Error getting status.", e);
        }

        return status;
    }

    public String getTooltip() {
        return get(TOOLTIP);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!(obj instanceof GwtNetIfStatusModel)) {
            return false;
        }

        GwtNetIfStatusModel other = (GwtNetIfStatusModel) obj;

        if (getStatus() != null) {
            if (!getStatus().equals(other.getStatus())) {
                return false;
            }
        } else if (other.getStatus() != null) {
            return false;
        }

        if (getTooltip() != null) {
            if (!getTooltip().equals(other.getTooltip())) {
                return false;
            }
        } else if (other.getTooltip() != null) {
            return false;
        }

        return true;
    }
}
