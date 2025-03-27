/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
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
package ${package};

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = ExampleDependencyService.class, immediate = true)
public class ExampleDependencyServiceComponent implements ExampleDependencyService {

    private static final Logger logger = LoggerFactory.getLogger(ExampleDependencyServiceComponent.class);

    @Override
    public void run() {
        logger.info("Running ExampleDependencyServiceComponent");
    }

}
