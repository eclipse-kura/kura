/*******************************************************************************
 * Copyright (c) 2021, 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.rest.network.configuration.provider.test.responses;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.configuration.metatype.Icon;
import org.eclipse.kura.configuration.metatype.OCD;

public class MockComponentConfiguration {

    ComponentConfiguration componentConfiguration;

    public MockComponentConfiguration(int numberOfMock) {
        createComponentConfiguration(numberOfMock);
    }

    private void createComponentConfiguration(int numberOfMock) {
        this.componentConfiguration = new ComponentConfiguration() {

            @Override
            public String getPid() {
                return "CONF_COMP_PID_" + numberOfMock;
            }

            @Override
            public OCD getDefinition() {
                return createOcd(numberOfMock);
            }

            @Override
            public Map<String, Object> getConfigurationProperties() {
                return Collections.emptyMap();
            }
        };
    }

    private OCD createOcd(int numberOfMock) {
        return new OCD() {

            @Override
            public String getName() {
                return "OCD_MOCK_NAME_" + numberOfMock;
            }

            @Override
            public String getId() {
                return "OCD_MOCK_ID_" + numberOfMock;
            }

            @Override
            public List<Icon> getIcon() {
                return null;
            }

            @Override
            public String getDescription() {
                return "OCD_MOCK_DESC_" + numberOfMock;
            }

            @Override
            public List<AD> getAD() {
                return null;
            }
        };
    }

    public ComponentConfiguration getComponentConfiguration() {
        return this.componentConfiguration;
    }

}
