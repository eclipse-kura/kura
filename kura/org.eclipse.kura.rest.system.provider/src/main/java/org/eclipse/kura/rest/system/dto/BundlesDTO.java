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

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.Bundle;

public class BundlesDTO {

    private List<BundleDTO> bundles;

    public BundlesDTO(Bundle[] bundles) {
        this.bundles = new ArrayList<>();
        for (Bundle bundle : bundles) {
            this.bundles.add(new BundleDTO(bundle));
        }
    }

    public List<BundleDTO> getBundles() {
        return this.bundles;
    }

}
