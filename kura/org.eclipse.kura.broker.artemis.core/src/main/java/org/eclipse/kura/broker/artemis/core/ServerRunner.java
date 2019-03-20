/*******************************************************************************
 * Copyright (c) 2017, 2019 Red Hat Inc
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *  Red Hat
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.broker.artemis.core;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.activemq.artemis.core.config.FileDeploymentManager;
import org.apache.activemq.artemis.core.config.impl.FileConfiguration;
import org.apache.activemq.artemis.core.config.impl.SecurityConfiguration;
import org.apache.activemq.artemis.core.server.ActiveMQComponent;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.jms.server.config.impl.FileJMSConfiguration;
import org.apache.activemq.artemis.spi.core.protocol.ProtocolManagerFactory;
import org.apache.activemq.artemis.spi.core.security.ActiveMQJAASSecurityManager;
import org.apache.activemq.artemis.spi.core.security.ActiveMQSecurityManager;
import org.apache.activemq.artemis.spi.core.security.jaas.InVMLoginModule;
import org.eclipse.kura.broker.artemis.core.UserAuthentication.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerRunner {

    private static final Logger logger = LoggerFactory.getLogger(ServerRunner.class);

    private final ServerConfiguration configuration;

    private Map<String, ActiveMQComponent> components;

    private final Collection<ProtocolManagerFactory<?>> protocols;

    public ServerRunner(final ServerConfiguration configuration,
            final Collection<ProtocolManagerFactory<?>> protocols) {
        this.configuration = configuration;
        this.protocols = protocols;
    }

    public void start() throws Exception {
        final Path file = Files.createTempFile("broker-", ".xml");
        try {
            try (final Writer writer = new PrintWriter(
                    new OutputStreamWriter(Files.newOutputStream(file), StandardCharsets.UTF_8), true)) {
                writer.write(this.configuration.getBrokerXml());
            }
            createArtemis(file);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private void createArtemis(final Path brokerXmlFile) throws Exception {

        final ActiveMQSecurityManager security = createSecurityManager();

        final FileConfiguration configuration = new FileConfiguration();

        final FileJMSConfiguration jmsConfiguration = new FileJMSConfiguration();

        final FileDeploymentManager fileDeploymentManager = new FileDeploymentManager(brokerXmlFile.toUri().toString());
        fileDeploymentManager.addDeployable(configuration);
        fileDeploymentManager.addDeployable(jmsConfiguration);
        fileDeploymentManager.readConfiguration();

        // load components

        this.components = fileDeploymentManager.buildService(security, ManagementFactory.getPlatformMBeanServer());

        logger.info("Loaded components: {}", this.components.size());
        for (final Map.Entry<String, ActiveMQComponent> entry : this.components.entrySet()) {
            logger.info("\t{} -> {}", entry.getKey(), entry.getValue());
        }

        // register all protocols

        final ActiveMQServer server = (ActiveMQServer) this.components.get("core");
        if (server != null) {
            for (final ProtocolManagerFactory<?> protocol : this.protocols) {
                logger.debug("Registering protocol: {}", protocol);
                server.addProtocolManagerFactory(protocol);
            }
        }

        // start components

        startComponents();
    }

    private ActiveMQSecurityManager createSecurityManager() {
        return createSecurityManager(this.configuration.getUserAuthentication());
    }

    private static ActiveMQSecurityManager createSecurityManager(final UserAuthentication source) {
        final SecurityConfiguration configuration = new SecurityConfiguration();

        configuration.setDefaultUser(source.getDefaultUser());
        for (final User user : source.getUsers()) {
            configuration.addUser(user.getName(), user.getPassword());
            for (final String role : user.getRoles()) {
                configuration.addRole(user.getName(), role);
            }
        }

        return new ActiveMQJAASSecurityManager(InVMLoginModule.class.getName(), configuration);
    }

    public void stop() throws Exception {
        stopComponents();
    }

    private List<ActiveMQComponent> getComponents() {
        if (this.components == null) {
            return Collections.emptyList();
        }

        final List<ActiveMQComponent> result = new ArrayList<>(2);
        final ActiveMQComponent jms = this.components.get("jms");
        if (jms != null) {
            result.add(jms);
        }

        final ActiveMQComponent core = this.components.get("core");
        if (core == null) {
            return Collections.emptyList();
        }
        result.add(core);

        return result;
    }

    private void startComponents() throws Exception {
        for (final ActiveMQComponent component : getComponents()) {
            component.start();
        }
    }

    private void stopComponents() throws Exception {
        final List<ActiveMQComponent> currentComponents = getComponents();
        final ListIterator<ActiveMQComponent> iter = currentComponents.listIterator(currentComponents.size());

        while (iter.hasPrevious()) {
            iter.previous().stop();
        }
    }
}
