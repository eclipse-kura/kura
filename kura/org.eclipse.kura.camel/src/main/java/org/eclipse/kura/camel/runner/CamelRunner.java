/*******************************************************************************
 * Copyright (c) 2016, 2018 Red Hat Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.kura.camel.runner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.core.osgi.OsgiDefaultCamelContext;
import org.apache.camel.core.osgi.OsgiServiceRegistry;
import org.apache.camel.impl.CompositeRegistry;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.model.OptionalIdentifiedDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RoutesDefinition;
import org.apache.camel.spi.ComponentResolver;
import org.apache.camel.spi.LanguageResolver;
import org.apache.camel.spi.Registry;
import org.apache.camel.util.function.ThrowingBiConsumer;
import org.eclipse.kura.camel.cloud.KuraCloudComponent;
import org.eclipse.kura.cloud.CloudService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A lifecycle manager for running a CamelContext
 * <p>
 * Use the {@link Builder} class to create instances of the {@link CamelRunner}.
 * </p>
 */
public class CamelRunner {

    private static final Logger logger = LoggerFactory.getLogger(CamelRunner.class);

    /**
     * Creates a new {@link ContextFactory} backed by {@link OsgiDefaultCamelContext}
     *
     * @param bundleContext
     *            the bundle context to use
     * @return a context factory creating {@link OsgiDefaultCamelContext}s
     */
    public static ContextFactory createOsgiFactory(final BundleContext bundleContext) {
        Objects.requireNonNull(bundleContext);

        return new ContextFactory() {

            @Override
            public CamelContext createContext(final Registry registry) {
                return new OsgiDefaultCamelContext(bundleContext, registry);
            }
        };
    }

    /**
     * Creates a new {@link RegistryFactory} backed by {@link OsgiServiceRegistry}
     *
     * @param bundleContext
     *            the bundle context to use
     * @return a registry factory creating {@link OsgiServiceRegistry}s
     */
    public static RegistryFactory createOsgiRegistry(final BundleContext bundleContext) {
        Objects.requireNonNull(bundleContext);

        return new RegistryFactory() {

            @Override
            public Registry createRegistry() {
                return new OsgiServiceRegistry(bundleContext);
            }
        };
    }

    public static RegistryFactory createOsgiRegistry(final BundleContext bundleContext,
            final Map<String, Object> services) {
        Objects.requireNonNull(bundleContext);

        if (services == null || services.isEmpty()) {
            return createOsgiRegistry(bundleContext);
        }

        return new RegistryFactory() {

            @Override
            public Registry createRegistry() {
                final List<Registry> registries = new LinkedList<>();

                // add simple registry

                final SimpleRegistry simple = new SimpleRegistry();
                simple.putAll(services);

                registries.add(simple);

                // add OSGi registry

                registries.add(new OsgiServiceRegistry(bundleContext));

                // return composite

                return new CompositeRegistry(registries);
            }
        };
    }

    /**
     * A builder for creating {@link CamelRunner} instances
     */
    public static final class Builder {

        private final BundleContext bundleContext;

        private RegistryFactory registryFactory;

        private ContextFactory contextFactory;

        private final List<ServiceDependency<?, CamelContext>> dependencies;

        private final List<BeforeStart> beforeStarts;

        private final List<ContextLifecycleListener> lifecycleListeners;

        private boolean disableJmx = true;

        private int shutdownTimeout = 5;

        public Builder() {
            this(FrameworkUtil.getBundle(CamelRunner.class).getBundleContext());
        }

        public Builder(final BundleContext bundleContext) {
            Objects.requireNonNull(bundleContext);

            this.bundleContext = bundleContext;

            this.registryFactory = createOsgiRegistry(bundleContext);
            this.contextFactory = createOsgiFactory(bundleContext);

            this.dependencies = new LinkedList<>();
            this.beforeStarts = new LinkedList<>();
            this.lifecycleListeners = new LinkedList<>();
        }

        public Builder(final Builder other) {
            Objects.requireNonNull(other);

            this.bundleContext = other.bundleContext;
            this.registryFactory = other.registryFactory;
            this.contextFactory = other.contextFactory;
            this.dependencies = new LinkedList<>(other.dependencies);
            this.beforeStarts = new LinkedList<>(other.beforeStarts);
            this.lifecycleListeners = new LinkedList<>(other.lifecycleListeners);
            this.disableJmx = other.disableJmx;
            this.shutdownTimeout = other.shutdownTimeout;
        }

        /**
         * Disable the use of JMX in the CamelContext
         * <p>
         * JMX is disabled by default.
         * </p>
         *
         * @param disableJmx
         *            whether JMX should be disabled or not
         * @return the builder instance
         */
        public Builder disableJmx(final boolean disableJmx) {
            this.disableJmx = disableJmx;
            return this;
        }

        /**
         * The shutdown timeout
         * <p>
         * This defaults to 5 seconds
         * </p>
         *
         * @param shutdownTimeout
         *            The shutdown timeout in seconds
         * @return the builder instance
         */
        public Builder shutdownTimeout(final int shutdownTimeout) {
            this.shutdownTimeout = shutdownTimeout;
            return this;
        }

        public Builder osgiContext(final BundleContext bundleContext) {
            Objects.requireNonNull(bundleContext);

            return contextFactory(createOsgiFactory(bundleContext));
        }

        public Builder registryFactory(final RegistryFactory registryFactory) {
            Objects.requireNonNull(registryFactory);

            this.registryFactory = registryFactory;
            return this;
        }

        public Builder contextFactory(final ContextFactory contextFactory) {
            Objects.requireNonNull(contextFactory);

            this.contextFactory = contextFactory;
            return this;
        }

        public <T> Builder dependOn(final Filter filter, final ServiceConsumer<T, CamelContext> consumer) {
            return dependOn(null, filter, consumer);
        }

        public <T> Builder dependOn(BundleContext bundleContext, final Filter filter,
                final ServiceConsumer<T, CamelContext> consumer) {
            Objects.requireNonNull(filter);
            Objects.requireNonNull(consumer);

            if (bundleContext == null) {
                bundleContext = Builder.this.bundleContext;
            }

            this.dependencies.add(new DefaultServiceDependency<>(bundleContext, filter, consumer));

            return this;
        }

        /**
         * Depend on a specific {@link CloudService} instance
         * <p>
         * If a filter is specified then it will be combined with the filter for the object class of the
         * {@link CloudService}.
         * If the filter expression is omitted then only the object class filter will be used.
         * </p>
         *
         * @param bundleContext
         *            the bundle context to use for service lookup
         * @param filter
         *            the filter expression to use searching for the cloud service instance
         * @param consumer
         *            the consumer processing the service instance
         * @return the builder instance
         */
        public Builder cloudService(BundleContext bundleContext, final String filter,
                final ServiceConsumer<CloudService, CamelContext> consumer) {
            final String baseFilter = String.format("(%s=%s)", Constants.OBJECTCLASS, CloudService.class.getName());

            try {
                if (filter != null && !filter.trim().isEmpty()) {
                    // combined filter
                    final Filter f = FrameworkUtil.createFilter(String.format("(&%s%s)", baseFilter, filter));
                    return dependOn(bundleContext, f, consumer);
                } else {
                    // empty custom filter, so only filter for class name
                    return dependOn(bundleContext, FrameworkUtil.createFilter(baseFilter), consumer);
                }
            } catch (InvalidSyntaxException e) {
                throw new RuntimeException("Failed to parse filter", e);
            }
        }

        /**
         * Depend on a specific {@link CloudService} instance
         * <p>
         * The cloud service will be injected into the camel context as component "kura-cloud".
         * </p>
         * <p>
         * If a filter is specified then it will be combined with the filter for the object class of the
         * {@link CloudService}.
         * If the filter expression is omitted then only the object class filter will be used.
         * </p>
         *
         * @param filter
         *            optional filter expression
         * @return the builder instance
         */
        public Builder cloudService(final String filter) {
            return cloudService((BundleContext) null, filter, addAsCloudComponent("kura-cloud"));
        }

        /**
         * Depend on a specific {@link CloudService} instance by key and value
         * <p>
         * This is a convenience method for {@link #cloudService(String)}. It will effectively call
         * this method with a filter of {@code "(" + attribute + "=" + value + ")"}
         * </p>
         *
         * @param attribute
         *            the OSGi attribute to look for
         * @param value
         *            the value the OSGi must have
         * @return the builder instance
         */
        public Builder cloudService(final String attribute, final String value) {
            Objects.requireNonNull(attribute);
            Objects.requireNonNull(value);

            return cloudService(String.format("(%s=%s)", attribute, value));
        }

        /**
         * Require a Camel component to be registered with OSGi before starting
         *
         * @param componentName
         *            the component name (e.g. "timer")
         * @return the builder instance
         */
        public Builder requireComponent(final String componentName) {
            try {
                final String filterString = String.format("(&(%s=%s)(%s=%s))", Constants.OBJECTCLASS,
                        ComponentResolver.class.getName(), "component", componentName);
                final Filter filter = FrameworkUtil.createFilter(filterString);
                dependOn(filter, (context, service) -> {
                });
            } catch (InvalidSyntaxException e) {
                throw new IllegalArgumentException(String.format("Illegal component name: '%s'", componentName), e);
            }

            return this;
        }

        /**
         * Require a Camel language to be registered with OSGi before starting
         *
         * @param languageName
         *            the language name (e.g. "javaScript")
         * @return the builder instance
         */
        public Builder requireLanguage(final String languageName) {
            try {
                final String filterString = String.format("(&(%s=%s)(%s=%s))", Constants.OBJECTCLASS,
                        LanguageResolver.class.getName(), "language", languageName);
                final Filter filter = FrameworkUtil.createFilter(filterString);
                dependOn(filter, (context, service) -> {
                });
            } catch (InvalidSyntaxException e) {
                throw new IllegalArgumentException(String.format("Illegal languageName name: '%s'", languageName), e);
            }

            return this;
        }

        public static ServiceConsumer<CloudService, CamelContext> addAsCloudComponent(final String componentName) {
            return new ServiceConsumer<CloudService, CamelContext>() {

                @Override
                public void consume(CamelContext context, CloudService service) {
                    context.addComponent(componentName, new KuraCloudComponent(context, service));
                }
            };
        }

        /**
         * Add an operation which will be executed before the Camel context is started
         *
         * @param beforeStart
         *            the action to start
         * @return the builder instance
         */
        public Builder addBeforeStart(final BeforeStart beforeStart) {
            Objects.requireNonNull(beforeStart);

            this.beforeStarts.add(beforeStart);

            return this;
        }

        /**
         * Add a context lifecylce listener.
         * 
         * @param listener
         *            The listener to add
         * @return the builder instance
         */
        public Builder addLifecycleListener(final ContextLifecycleListener listener) {
            Objects.requireNonNull(listener);

            this.lifecycleListeners.add(listener);

            return this;
        }

        /**
         * Build the actual CamelRunner instance based on the current configuration of the builder instance
         * <p>
         * Modifications which will be made to the builder after the {@link #build()} method was called will
         * no affect the created CamelRunner instance. It is possible though to call the {@link #build()} method
         * multiple times.
         * </p>
         *
         * @return the new instance
         */
        public CamelRunner build() {
            final List<BeforeStart> beforeStarts = new ArrayList<>(this.beforeStarts);
            final List<ServiceDependency<?, CamelContext>> dependencies = new ArrayList<>(this.dependencies);

            if (this.disableJmx) {
                beforeStarts.add(camelContext -> camelContext.disableJMX());
            }
            if (this.shutdownTimeout > 0) {
                final int shutdownTimeout = this.shutdownTimeout;
                beforeStarts.add(camelContext -> {
                    camelContext.getShutdownStrategy().setTimeUnit(TimeUnit.SECONDS);
                    camelContext.getShutdownStrategy().setTimeout(shutdownTimeout);
                });
            }
            return new CamelRunner(this.registryFactory, this.contextFactory, beforeStarts, this.lifecycleListeners,
                    dependencies);
        }
    }

    private final RegistryFactory registryFactory;
    private final ContextFactory contextFactory;
    private final List<BeforeStart> beforeStarts;
    private final List<ContextLifecycleListener> lifecycleListeners;
    private final List<ServiceDependency<?, CamelContext>> dependencies;

    private CamelContext context;
    private RoutesProvider routes = EmptyRoutesProvider.INSTANCE;
    private DependencyRunner<CamelContext> dependencyRunner;

    private CamelRunner(final RegistryFactory registryFactory, final ContextFactory contextFactory,
            final List<BeforeStart> beforeStarts, final List<ContextLifecycleListener> lifecycleListeners,
            final List<ServiceDependency<?, CamelContext>> dependencies) {
        this.registryFactory = registryFactory;
        this.contextFactory = contextFactory;
        this.beforeStarts = beforeStarts;
        this.lifecycleListeners = lifecycleListeners;
        this.dependencies = dependencies;
    }

    private Registry createRegistry() {
        return this.registryFactory.createRegistry();
    }

    private CamelContext createContext(final Registry registry) {
        return this.contextFactory.createContext(registry);
    }

    /**
     * Start the camel runner instance
     * <p>
     * This may not start the camel context right away if there are unresolved dependencies
     * </p>
     */
    public void start() {

        stop();

        logger.info("Starting...");

        this.dependencyRunner = new DependencyRunner<>(this.dependencies,
                new DependencyRunner.Listener<CamelContext>() {

                    @Override
                    public void ready(final List<ServiceDependency.Handle<CamelContext>> dependencies) {
                        try {
                            startCamel(dependencies);
                        } catch (Exception e) {
                            logger.warn("Failed to start context", e);
                        }
                    }

                    @Override
                    public void notReady() {
                        try {
                            stopCamel();
                        } catch (Exception e) {
                            logger.warn("Failed to stop context", e);
                        }
                    }
                });
        this.dependencyRunner.start();
    }

    /**
     * Stop the camel runner instance
     */
    public void stop() {

        if (this.dependencyRunner != null) {
            logger.info("Stopping...");
            this.dependencyRunner.stop();
            this.dependencyRunner = null;
        }
    }

    protected void startCamel(final List<ServiceDependency.Handle<CamelContext>> dependencies) throws Exception {

        if (this.context != null) {
            logger.warn("Camel already running");
            return;
        }

        final Registry registry = createRegistry();

        final CamelContext context = createContext(registry);
        beforeStart(context);

        for (final ServiceDependency.Handle<CamelContext> dep : dependencies) {
            dep.consume(context);
        }

        this.context = context;

        this.routes.applyRoutes(this.context);
        this.context.start();

        fireLifecycle(this.context, ContextLifecycleListener::started);
    }

    protected void stopCamel() throws Exception {
        if (this.context != null) {

            fireLifecycle(this.context, ContextLifecycleListener::stopping);

            this.context.stop();
            this.context = null;

        }
    }

    private void beforeStart(final CamelContext context) throws Exception {
        logger.debug("Running before starts...");

        for (final BeforeStart beforeStart : this.beforeStarts) {
            beforeStart.beforeStart(context);
        }
    }

    private void fireLifecycle(final CamelContext context,
            final ThrowingBiConsumer<ContextLifecycleListener, CamelContext, Exception> consumer) {

        for (final ContextLifecycleListener listener : this.lifecycleListeners) {

            try {
                consumer.accept(listener, context);
            } catch (Exception e) {
                logger.warn("Failed to call listener", e);
            }

        }

    }

    /**
     * Remove a set of routes from the context
     * <p>
     * This is a helper method intended to be used by implementations of {@link RoutesProvider}.
     * </p>
     *
     * @param context
     *            the context to work on
     * @param removedRouteIds
     *            the ID to remove
     */
    public static void removeRoutes(final CamelContext context, final Set<String> removedRouteIds) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(removedRouteIds);

        for (final String id : removedRouteIds) {
            try {
                logger.debug("Stopping route: {}", id);
                context.stopRoute(id);
                logger.debug("Removing route: {}", id);
                context.removeRoute(id);
            } catch (Exception e) {
                logger.warn("Failed to remove route: {}", id, e);
            }
        }
    }

    /**
     * Remove all routes from the context which are not in the new set
     * <p>
     * This is a helper method intended to be used by implementations of {@link RoutesProvider}.
     * </p>
     *
     * @param context
     *            the context to work on
     * @param routes
     *            the collection of new routes
     */
    public static void removeMissingRoutes(final CamelContext context, final Collection<RouteDefinition> routes) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(routes);

        // gather new IDs

        final Set<String> newRouteIds = fromDefs(routes);

        // eval removed

        final Set<String> removedRouteIds = new HashSet<>(fromRoutes(context.getRoutes()));
        removedRouteIds.removeAll(newRouteIds);

        // remove from running context

        removeRoutes(context, removedRouteIds);
    }

    /**
     * Remove all routes of a context
     *
     * @param context
     *            the context to work on
     */
    public static void removeAllRoutes(final CamelContext context) {
        Objects.requireNonNull(context);

        // remove all routes

        removeRoutes(context, fromDefs(context.getRouteDefinitions()));
    }

    /**
     * Clear all routes from the context
     */
    public void clearRoutes() {
        setRoutes(EmptyRoutesProvider.INSTANCE);
    }

    /**
     * Replace the current set of route with an new one
     *
     * @param routes
     *            the new set of routes, may be {@code null}
     */
    public void setRoutes(final RoutesProvider routes) {

        if (routes == null) {
            clearRoutes();
            return;
        }

        this.routes = routes;

        final CamelContext context = this.context;
        if (context != null) {
            try {
                this.routes.applyRoutes(context);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Replace the current set of route with an new one
     *
     * @param xml
     *            the new set of routes, may be {@code null}
     */
    public void setRoutes(final String xml) throws Exception {
        logger.info("Setting routes...");

        if (xml == null || xml.trim().isEmpty()) {
            clearRoutes();
        } else {
            setRoutes(new XmlRoutesProvider(xml));
        }
    }

    /**
     * Replace the current set of route with an new one
     *
     * @param routes
     *            the new set of routes, may be {@code null}
     */
    public void setRoutes(final RoutesDefinition routes) throws Exception {
        logger.info("Setting routes...");

        if (routes == null) {
            clearRoutes();
            return;
        }

        setRoutes(new SimpleRoutesProvider(routes));
    }

    /**
     * Replace the current set of route with an new one
     *
     * @param routeBuilder
     *            the new set of routes, may be {@code null}
     */
    public void setRoutes(final RouteBuilder routeBuilder) throws Exception {
        logger.info("Setting routes...");

        if (routeBuilder == null) {
            clearRoutes();
            return;
        }

        setRoutes(new BuilderRoutesProvider(routeBuilder));
    }

    static Set<String> fromRoutes(final Collection<Route> routes) {
        final Set<String> result = new HashSet<>(routes.size());

        for (final Route route : routes) {
            result.add(route.getId());
        }

        return result;
    }

    private static <T extends OptionalIdentifiedDefinition<T>> Set<String> fromDefs(final Collection<T> defs) {
        Objects.requireNonNull(defs);

        final Set<String> result = new HashSet<>(defs.size());

        for (final T def : defs) {
            final String id = def.getId();
            if (id != null) {
                result.add(def.getId());
            }
        }

        return result;
    }

    /**
     * Get the camel context
     * <p>
     * <strong>Note: </strong> This method may return {@code null} even after the {@link #start()} method was called
     * if there are unresolved dependencies for the runner.
     * </p>
     *
     * @return the camel context, if the camel context is currently not running then {@code null} is being returned
     */
    public CamelContext getCamelContext() {
        return this.context;
    }

}
