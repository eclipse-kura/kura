/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.kura.camel;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
import org.apache.camel.spi.Registry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CamelRunner {

    private static final Logger logger = LoggerFactory.getLogger(CamelRunner.class);

    public interface RoutesProvider {

        /**
         * Apply the desired state of camel routes to the context
         * <p>
         * <strong>Note: </strong> This method may need to stop and remove
         * routes which are no longer used
         * </p>
         *
         * @param camelContext
         *            the context the routes should by applied to
         * @throws Exception
         *             if anything goes wrong
         */
        public void applyRoutes(CamelContext camelContext) throws Exception;
    }

    public static class EmptyRoutesProvider implements RoutesProvider {

        public static final EmptyRoutesProvider INSTANCE = new EmptyRoutesProvider();

        private EmptyRoutesProvider() {
        }

        @Override
        public void applyRoutes(final CamelContext camelContext) throws Exception {
            removeRoutes(camelContext, fromRoutes(camelContext.getRoutes()));
        }
    }

    public static class BuilderRoutesProvider implements RoutesProvider {

        private final RouteBuilder builder;

        public BuilderRoutesProvider(final RouteBuilder builder) throws Exception {
            this.builder = builder;
        }

        @Override
        public void applyRoutes(CamelContext camelContext) throws Exception {
            removeMissingRoutes(camelContext, this.builder.getRouteCollection().getRoutes());
            camelContext.addRoutes(this.builder);
        }
    }

    public static abstract class AbstractRoutesProvider implements RoutesProvider {

        @Override
        public void applyRoutes(final CamelContext camelContext) throws Exception {

            final RoutesDefinition routes = getRoutes(camelContext);

            removeMissingRoutes(camelContext, routes.getRoutes());
            camelContext.addRouteDefinitions(routes.getRoutes());
        }

        protected abstract RoutesDefinition getRoutes(CamelContext camelContext) throws Exception;
    }

    public static class SimpleRoutesProvider extends AbstractRoutesProvider {

        private final RoutesDefinition routes;

        public SimpleRoutesProvider(RoutesDefinition routes) {
            Objects.requireNonNull(routes);
            this.routes = routes;
        }

        @Override
        protected RoutesDefinition getRoutes(final CamelContext camelContext) throws Exception {
            return this.routes;
        }
    }

    public static class XmlRoutesProvider extends AbstractRoutesProvider {

        private final String xml;

        public XmlRoutesProvider(final String xml) {
            Objects.requireNonNull(xml);
            this.xml = xml;
        }

        @Override
        protected RoutesDefinition getRoutes(final CamelContext camelContext) throws Exception {
            try (final InputStream in = new ByteArrayInputStream(this.xml.getBytes(StandardCharsets.UTF_8))) { // just always close it
                return camelContext.loadRoutesDefinition(in);
            }
        }
    }

    public static ContextFactory createOsgiFactory(final BundleContext bundleContext) {
        Objects.requireNonNull(bundleContext);

        return new ContextFactory() {

            @Override
            public CamelContext createContext(final Registry registry) {
                return new OsgiDefaultCamelContext(bundleContext);
            }
        };
    }

    public static RegistryFactory createOsgiRegistry(final BundleContext bundleContext) {
        Objects.requireNonNull(bundleContext);

        return new RegistryFactory() {

            @Override
            public Registry createRegistry() {
                return new OsgiServiceRegistry(bundleContext);
            }
        };
    }

    public static RegistryFactory createOsgiRegistry(final BundleContext bundleContext, final Map<String, Object> services) {
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

    public interface BeforeStart {

        public void beforeStart(CamelContext camelContext) throws Exception;
    }

    public static final class Builder {

        private final RegistryFactory registryFactory;

        private ContextFactory contextFactory;

        private final List<ServiceDependency<?, CamelContext>> dependencies;

        private final List<BeforeStart> beforeStarts;

        private boolean disableJmx = true;

        private int shutdownTimeout = 5;

        public Builder() {
            final BundleContext bundleContext = FrameworkUtil.getBundle(CamelRunner.class).getBundleContext();

            this.registryFactory = createOsgiRegistry(bundleContext);
            this.contextFactory = createOsgiFactory(bundleContext);

            this.dependencies = new LinkedList<>();
            this.beforeStarts = new LinkedList<>();
        }

        public Builder(final BundleContext bundleContext) {
            Objects.requireNonNull(bundleContext);

            this.registryFactory = createOsgiRegistry(bundleContext);
            this.contextFactory = createOsgiFactory(bundleContext);

            this.dependencies = new LinkedList<>();
            this.beforeStarts = new LinkedList<>();
        }

        public Builder(final Builder other) {
            Objects.requireNonNull(other);

            this.registryFactory = other.registryFactory;
            this.contextFactory = other.contextFactory;
            this.dependencies = new LinkedList<>(other.dependencies);
            this.beforeStarts = new LinkedList<>(other.beforeStarts);
            this.disableJmx = other.disableJmx;
            this.shutdownTimeout = other.shutdownTimeout;
        }

        public Builder disableJmx(boolean disableJmx) {
            this.disableJmx = disableJmx;
            return this;
        }

        public Builder shutdownTimeout(int shutdownTimeout) {
            this.shutdownTimeout = shutdownTimeout;
            return this;
        }

        public Builder osgiContext(final BundleContext bundleContext) {
            Objects.requireNonNull(bundleContext);

            return contextFactory(createOsgiFactory(bundleContext));
        }

        public Builder contextFactory(final ContextFactory contextFactory) {
            Objects.requireNonNull(contextFactory);

            this.contextFactory = contextFactory;
            return this;
        }

        public <T> Builder dependOn(final BundleContext bundleContext, final Filter filter, final ServiceConsumer<T, CamelContext> consumer) {
            this.dependencies.add(new DefaultServiceDependency<>(bundleContext, filter, consumer));
            return this;
        }

        public Builder addBeforeStart(final BeforeStart beforeStart) {
            Objects.requireNonNull(beforeStart);

            this.beforeStarts.add(beforeStart);

            return this;
        }

        public CamelRunner build() {
            final List<BeforeStart> beforeStarts = new ArrayList<>(this.beforeStarts);
            final List<ServiceDependency<?, CamelContext>> dependencies = new ArrayList<>(this.dependencies);

            if (this.disableJmx) {
                beforeStarts.add(new BeforeStart() {

                    @Override
                    public void beforeStart(CamelContext camelContext) {
                        camelContext.disableJMX();
                    }
                });
            }
            if (this.shutdownTimeout > 0) {
                final int shutdownTimeout = this.shutdownTimeout;
                beforeStarts.add(new BeforeStart() {

                    @Override
                    public void beforeStart(CamelContext camelContext) {
                        camelContext.getShutdownStrategy().setTimeout(shutdownTimeout);
                    }
                });
            }
            return new CamelRunner(this.registryFactory, this.contextFactory, beforeStarts, dependencies);
        }
    }

    private final RegistryFactory registryFactory;
    private final ContextFactory contextFactory;
    private final List<BeforeStart> beforeStarts;
    private final List<ServiceDependency<?, CamelContext>> dependencies;

    private CamelContext context;
    private RoutesProvider routes = EmptyRoutesProvider.INSTANCE;
    private DependencyRunner<CamelContext> dependencyRunner;

    private CamelRunner(final RegistryFactory registryFactory, final ContextFactory contextFactory, final List<BeforeStart> beforeStarts, final List<ServiceDependency<?, CamelContext>> dependencies) {
        this.registryFactory = registryFactory;
        this.contextFactory = contextFactory;
        this.beforeStarts = beforeStarts;
        this.dependencies = dependencies;
    }

    private Registry createRegistry() {
        return this.registryFactory.createRegistry();
    }

    private CamelContext createContext(final Registry registry) {
        return this.contextFactory.createContext(registry);
    }

    public void start() {
        logger.info("Starting...");
        stop();

        this.dependencyRunner = new DependencyRunner<>(this.dependencies, new DependencyRunner.Listener<CamelContext>() {

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

    public void stop() {
        logger.info("Stopping...");

        if (this.dependencyRunner != null) {
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
    }

    protected void stopCamel() throws Exception {
        if (this.context != null) {
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

    public static void removeRoutes(final CamelContext context, final Set<String> removedRouteIds) {
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

    public static void removeMissingRoutes(final CamelContext context, final Collection<RouteDefinition> routes) {
        // gather new IDs

        final Set<String> newRouteIds = fromDefs(routes);

        // eval removed

        final Set<String> removedRouteIds = new HashSet<>(fromRoutes(context.getRoutes()));
        removedRouteIds.removeAll(newRouteIds);

        // remove from running context

        removeRoutes(context, removedRouteIds);
    }

    public void clearRoutes() {
        setRoutes(EmptyRoutesProvider.INSTANCE);
    }

    public void setRoutes(final RoutesProvider routes) {
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

    public void setRoutes(final String xml) throws Exception {
        logger.info("Setting routes...");

        if (xml == null || xml.trim().isEmpty()) {
            clearRoutes();
        } else {
            setRoutes(new XmlRoutesProvider(xml));
        }
    }

    public void setRoutes(final RoutesDefinition routes) throws Exception {
        logger.info("Setting routes...");

        Objects.requireNonNull(routes);
        setRoutes(new SimpleRoutesProvider(routes));
    }

    public void setRoutes(final RouteBuilder routeBuilder) throws Exception {
        logger.info("Setting routes...");

        Objects.requireNonNull(routeBuilder);
        setRoutes(new BuilderRoutesProvider(routeBuilder));
    }

    private static Set<String> fromRoutes(final Collection<Route> routes) {
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

    public CamelContext getCamelContext() {
        return this.context;
    }

}
