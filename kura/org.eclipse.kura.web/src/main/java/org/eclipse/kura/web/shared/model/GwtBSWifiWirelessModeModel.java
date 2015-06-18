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


public class GwtBSWifiWirelessModeModel extends GwtBSBaseModel {

    private static final long serialVersionUID = -6095963356000494663L;

    public static final String NAME = "name";
    public static final String MODE = "mode";
    public static final String TOOLTIP = "tooltip";
    
    protected GwtBSWifiWirelessModeModel() {
        
    }
    
    public GwtBSWifiWirelessModeModel(GwtBSWifiWirelessMode mode, String name, String tooltip) {
        set(MODE, mode.name());
        set(NAME, name);
        set(TOOLTIP, tooltip);
    }

    public String getName() {
        return get(NAME);
    }
    
    public GwtBSWifiWirelessMode getMode() {
        GwtBSWifiWirelessMode mode = null;
        String modeStr = get(MODE);
        
        try {
            mode = GwtBSWifiWirelessMode.valueOf(modeStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return mode;
    }
    
    public String getTooltip() {
        return get(TOOLTIP);
    }
    
    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        
        if (!(obj instanceof GwtBSWifiWirelessModeModel)) {
            return false;
        }
        
        GwtBSWifiWirelessModeModel other = (GwtBSWifiWirelessModeModel) obj;
        
        if(getMode() != null) {
            if(!getMode().equals(other.getMode())) {
                return false;
            }
        } else if(other.getMode() != null) {
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
