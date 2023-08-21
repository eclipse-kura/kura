/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.rest.system.dto;

import org.osgi.framework.Bundle;

public class BundleDTO {

    private long bundleId;
    private String location;
    private String state;
    private String symbolicName;
    private String version;
    private boolean signed;
    private long lastModified;

    public BundleDTO(Bundle bundle) {
        this.bundleId = bundle.getBundleId();
        this.location = bundle.getLocation();
        this.state = getStateStringFrom(bundle.getState());
        this.symbolicName = bundle.getSymbolicName();

        StringBuilder versionSb = new StringBuilder();
        versionSb.append(bundle.getVersion().getMajor());
        versionSb.append(".");
        versionSb.append(bundle.getVersion().getMinor());
        versionSb.append(".");
        versionSb.append(bundle.getVersion().getMicro());
        versionSb.append(".");
        versionSb.append(bundle.getVersion().getQualifier());

        this.version = versionSb.toString();
        this.signed = !bundle.getSignerCertificates(Bundle.SIGNERS_ALL).isEmpty();
        this.lastModified = bundle.getLastModified();
    }

    public long getBundleId() {
        return this.bundleId;
    }

    public String getLocation() {
        return this.location;
    }

    public String getState() {
        return this.state;
    }

    public String getSymbolicName() {
        return this.symbolicName;
    }

    public String getVersion() {
        return this.version;
    }

    public boolean getSigned() {
        return this.signed;
    }

    public long getLastModified() {
        return this.lastModified;
    }

    private String getStateStringFrom(int code) {
        switch (code) {
        case Bundle.ACTIVE:
            return "ACTIVE";
        case Bundle.INSTALLED:
            return "INSTALLED";
        case Bundle.RESOLVED:
            return "RESOLVED";
        case Bundle.STARTING:
            return "STARTING";
        case Bundle.STOPPING:
            return "STOPPING";
        case Bundle.UNINSTALLED:
            return "UNINSTALLED";
        default:
            return "UNKNOWN";
        }
    }

}
