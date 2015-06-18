/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.web.shared.model;

public class GwtBSNetIfStatusModel extends GwtBSBaseModel {

    private static final long serialVersionUID = 2779596516813518500L;

    public static final String NAME = "name";
    public static final String STATUS = "status";
    public static final String TOOLTIP = "tooltip";
    
    protected GwtBSNetIfStatusModel() {
        
    }
    
    public GwtBSNetIfStatusModel(GwtBSNetIfStatus status, String name, String tooltip) {
        set(STATUS, status.name());
        set(NAME, name);
        set(TOOLTIP, tooltip);
    }

    public String getName() {
        return (String) get(NAME);
    }
    
    public GwtBSNetIfStatus getStatus() {
        GwtBSNetIfStatus status = null;
        String statusStr = (String) get(STATUS);
        
        try {
            status = GwtBSNetIfStatus.valueOf(statusStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return status;
    }
    
    public String getTooltip() {
        return (String) get(TOOLTIP);
    }
    
    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        
        if (!(obj instanceof GwtBSNetIfStatusModel)) {
            return false;
        }
        
        GwtBSNetIfStatusModel other = (GwtBSNetIfStatusModel) obj;
        
        if(getStatus() != null) {
            if(!getStatus().equals(other.getStatus())) {
                return false;
            }
        } else if(other.getStatus() != null) {
            return false;
        }
        
        if(getTooltip() != null) {
            if(!getTooltip().equals(other.getTooltip())) {
                return false;
            }
        } else if(other.getTooltip() != null) {
            return false;
        }
        
        return true;
    }
}
