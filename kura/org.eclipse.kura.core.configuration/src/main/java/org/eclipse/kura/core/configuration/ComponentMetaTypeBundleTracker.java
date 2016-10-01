/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.configuration;

import java.util.Map;

import org.eclipse.kura.configuration.metatype.Designate;
import org.eclipse.kura.configuration.metatype.OCD;
import org.eclipse.kura.core.configuration.metatype.Tmetadata;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.util.ComponentUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.BundleTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BundleTracker to track all the Service which have defaults in MetaType.
 * When the ConfigurableComponet is found it is then registered to the ConfigurationService.
 */
public class ComponentMetaTypeBundleTracker extends BundleTracker<Bundle> {

    private static final Logger s_logger = LoggerFactory.getLogger(ComponentMetaTypeBundleTracker.class);

    private final BundleContext m_context;
    private final ConfigurationServiceImpl m_configurationService;

    public ComponentMetaTypeBundleTracker(BundleContext context, ConfigurationServiceImpl configurationService)
            throws InvalidSyntaxException {
        super(context, Bundle.ACTIVE, null);
        this.m_context = context;
        this.m_configurationService = configurationService;
    }

    // ----------------------------------------------------------------
    //
    // Override APIs
    //
    // ----------------------------------------------------------------

    @Override
    public void open() {
        s_logger.info("Opening ComponentMetaTypeBundleTracker...");
        super.open();
        s_logger.debug("open(): getting bundles...");
        Bundle[] bundles = this.m_context.getBundles();
        if (bundles != null) {
            for (Bundle bundle : bundles) {
                if (bundle.getState() == Bundle.ACTIVE) {
                    s_logger.debug("open(): processing MetaType for bundle: {}...", bundle.getSymbolicName());
                    processBundleMetaType(bundle);
                    s_logger.debug("open(): processed MetaType for bundle: {}", bundle.getSymbolicName());
                } else {
                    s_logger.debug("open(): bundle: {} is in state: {}. MetaType will be processed by addingBundle()",
                            bundle.getSymbolicName(), bundle.getState());
                }
            }
        }
        s_logger.debug("open(): done");
    }

    @Override
    public Bundle addingBundle(Bundle bundle, BundleEvent event) {
        Bundle bnd = super.addingBundle(bundle, event);
        s_logger.debug("addingBundle(): processing MetaType for bundle: {}...", bundle.getSymbolicName());
        processBundleMetaType(bundle);
        s_logger.debug("addingBundle(): processed MetaType for bundle: {}", bundle.getSymbolicName());
        return bnd;
    }

    // ----------------------------------------------------------------
    //
    // Private APIs
    //
    // ----------------------------------------------------------------

    private void processBundleMetaType(Bundle bundle) {
        // Push the latest configuration merging the properties in ConfigAdmin
        // with the default properties read from the component's meta-type.
        // This allows components to incrementally add new configuration
        // properties in the meta-type.
        // Only the new default properties are merged with the configuration
        // properties in ConfigurationAdmin.
        // Note: configuration properties in snapshots no longer present in
        // the meta-type are not purged.

        Map<String, Tmetadata> metas = ComponentUtil.getMetadata(this.m_context, bundle);
        for (String metatypePid : metas.keySet()) {
            try {

                // register the OCD for all the contained services
                Tmetadata metadata = metas.get(metatypePid);
                if (metadata != null) {

                    // check if this component is a factory
                    boolean isFactory = false;
                    Designate designate = ComponentUtil.getDesignate(metadata, metatypePid);
                    if (designate.getFactoryPid() != null && !designate.getFactoryPid().isEmpty()) {
                        isFactory = true;
                    }

                    // register the pid with the OCD and whether it is a factory
                    OCD ocd = ComponentUtil.getOCD(metadata, metatypePid);
                    this.m_configurationService.registerComponentOCD(metatypePid, (Tocd) ocd, isFactory);
                }
            } catch (Exception e) {
                s_logger.error("Error seeding configuration for pid: " + metatypePid, e);
            }
        }
    }
}
