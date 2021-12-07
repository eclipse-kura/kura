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
package org.eclipse.kura.rest.configuration.api;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.configuration.metatype.Icon;
import org.eclipse.kura.configuration.metatype.OCD;

public class OcdDTO implements OCD {

    private final List<AdDTO> ad;
    private final List<IconDTO> icon;
    private final String name;
    private final String description;
    private final String id;

    public OcdDTO(final OCD ocd) {
        this.name = ocd.getName();
        this.description = ocd.getDescription();
        this.id = ocd.getId();

        final List<AD> ocdAD = ocd.getAD();

        if (ocdAD == null || ocdAD.isEmpty()) {
            this.ad = null;
        } else {
            this.ad = ocdAD.stream().map(AdDTO::new).collect(Collectors.toList());
        }

        final List<Icon> ocdIcon = ocd.getIcon();

        if (ocdIcon == null || ocdIcon.isEmpty()) {
            this.icon = null;
        } else {
            this.icon = ocdIcon.stream().map(IconDTO::new).collect(Collectors.toList());
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    public List<AD> getAD() {
        return (List<AD>) (Object) ad;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Icon> getIcon() {
        return (List<Icon>) (Object) icon;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getId() {
        return id;
    }

}
