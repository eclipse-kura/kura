/*******************************************************************************
 * Copyright (c) 2020, 2021 Eurotech and/or its affiliates and others
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
import java.util.HashSet;
import java.util.Set;

import org.eclipse.kura.web.shared.KuraPermission;

public class GwtUserData extends GwtBaseModel implements Serializable {

    private static final long serialVersionUID = -1334340006399833329L;
    private static final int MAX_TEXT_LENGTH = 22;

    @SuppressWarnings("unused")
    private HashSet<String> unused;

    public GwtUserData() {
    }

    public GwtUserData(final String userName, final Set<String> permissions) {
        setUserName(userName);
        setPermissions(permissions);
    }

    public void setUserName(final String userName) {
        set("userName", userName);
    }

    public void setPermissions(final Set<String> permissions) {
        set("permissions", new HashSet<>(permissions));
    }

    public String getUserName() {
        return get("userName");
    }

    public String getUserNameEllipsed() {
        String username = get("userName");
        if (username.length() > MAX_TEXT_LENGTH) {
            return username.substring(0, MAX_TEXT_LENGTH) + "...";
        } else {
            return username;
        }
    }

    public Set<String> getPermissions() {
        return get("permissions");
    }

    public boolean isAdmin() {
        return getPermissions().contains(KuraPermission.ADMIN);
    }

    public boolean checkPermissions(final Set<String> permissions) {
        if (isAdmin()) {
            return true;
        }
        return getPermissions().containsAll(permissions);
    }

    public boolean checkPermission(String permission) {
        if (isAdmin()) {
            return true;
        }
        return getPermissions().contains(permission);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (isAdmin() ? 1231 : 1237);
        result = prime * result + (getPermissions() == null ? 0 : getPermissions().hashCode());
        result = prime * result + (getUserName() == null ? 0 : getUserName().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        GwtUserData other = (GwtUserData) obj;
        if (isAdmin() != other.isAdmin()) {
            return false;
        }
        if (getPermissions() == null) {
            if (other.getPermissions() != null) {
                return false;
            }
        } else if (!getPermissions().equals(other.getPermissions())) {
            return false;
        }
        if (getUserName() == null) {
            if (other.getUserName() != null) {
                return false;
            }
        } else if (!getUserName().equals(other.getUserName())) {
            return false;
        }
        return true;
    }

}