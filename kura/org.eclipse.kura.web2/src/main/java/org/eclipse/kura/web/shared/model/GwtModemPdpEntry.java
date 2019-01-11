/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
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

import java.io.Serializable;

import org.eclipse.kura.web.client.util.KuraBaseModel;

public class GwtModemPdpEntry extends KuraBaseModel implements Serializable {

    private static final long serialVersionUID = -5616083805637215506L;

    public Integer getContextNumber() {
        if (get("contextnum") != null) {
            return (Integer) get("contextnum");
        } else {
            return 0;
        }
    }
    
    public String getPdpType() {
        return get("pdptype");
    }
    
    public String getApn() {
        return get("apn");
    }
    
    public void setContextNumber(int contextnum) {
        set("contextnum", contextnum);
    }
    
    public void setPdpType(String pdptype) {
        set("pdptype", pdptype);
    }
    
    public void setApn(String apn) {
        set("apn", apn);
    }
}
