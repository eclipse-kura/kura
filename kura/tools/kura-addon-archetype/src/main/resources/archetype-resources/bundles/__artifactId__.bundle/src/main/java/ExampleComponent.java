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

import java.util.Map;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, //
        configurationPolicy = ConfigurationPolicy.REQUIRE, //
        property = { "kura.service.pid=it.coppola.ExampleComponent" } //
)
@Designate(ocd = ExampleComponentOCD.class, factory = false)
public class ExampleComponent implements ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(ExampleComponent.class);

    private ExampleComponentOptions options;

    @Reference
    private ExampleDependencyService exampleDependencyService;

    @Reference
    public void setExampleDependencyService(ExampleDependencyService exampleDependencyService) {
        this.exampleDependencyService = exampleDependencyService;
    }

    /*
     * In the in activate, modified, deactivate methods it is possible to provide
     * the ComponentContext and the ExampleComponentOCD as parameters.
     * All parameters in activate, modified, deactivate are optional and can be
     * removed if not needed
     * 
     * Examples:
     * 
     * public void activate()
     * public void activate(ExampleComponentOCD configuration)
     * public void activate(ComponentContext componentContext, final Map<String, Object> properties, final
     * ExampleComponentOCD configuration)
     */
    @Activate
    public void activate(final Map<String, Object> properties) {
        logger.info("Activating");

        updated(properties);

        logger.info("Activated");
    }

    @Modified
    public void updated(final Map<String, Object> properties) {
        logger.info("Updating");

        logger.debug("Updating with properties: {}", properties);
        this.options = new ExampleComponentOptions(properties);

        this.exampleDependencyService.run();

        logger.info("Updated");
    }

    @Deactivate
    public synchronized void deactivate() {
        logger.info("Deactivating");
        logger.info("Deactivated");
    }

    public ExampleComponentOptions getOptions() {
        return this.options;
    }

}
