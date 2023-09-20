/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates. All rights reserved.
 *******************************************************************************/
package org.eclipse.kura.rest.deployment.agent.api;

public class InstallRequest implements Validable {

    private final String url;

    public InstallRequest(String url) {
        this.url = url;
    }

    public String getUrl() {
        return this.url;
    }

    @Override
    public boolean isValid() {
        return this.url != null;
    }

}
