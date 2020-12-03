/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.shared.model;

import java.io.Serializable;

public class GwtSettings extends GwtBaseModel implements Serializable {

    private static final long serialVersionUID = -7285859217584861659L;

    public void setPasswordCurrent(String pwdCurrent) {
        set("pwdCurrent", pwdCurrent);
    }

    public String getPasswordCurrent() {
        return (String) get("pwdCurrent");
    }

    public void setPasswordNew(String pwdNew) {
        set("pwdNew", pwdNew);
    }

    public String getPasswordNew() {
        return (String) get("pwdNew");
    }
}
