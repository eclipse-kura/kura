/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.broker.artemis.simple.mqtt;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.eclipse.kura.broker.artemis.core.ServerConfiguration;
import org.eclipse.kura.broker.artemis.core.ServerManager;
import org.eclipse.kura.broker.artemis.core.UserAuthentication;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.crypto.CryptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.google.common.io.Resources;

public class ServiceComponent implements ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(ServiceComponent.class);

    private ServerConfiguration configuration;
    private ServerManager server;
    private CryptoService cryptoService;

    public void setCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public void activate(final Map<String, Object> properties) throws Exception {
        final ServerConfiguration cfg = parse(properties);
        if (cfg != null) {
            start(cfg);
        }
    }

    public void modified(final Map<String, Object> properties) throws Exception {
        final ServerConfiguration cfg = parse(properties);

        if (this.configuration == cfg) {
            logger.debug("Configuration identical ... skipping update");
            return;
        }

        if (this.configuration != null && this.configuration.equals(cfg)) {
            logger.debug("Configuration equal ... skipping update");
            return;
        }

        stop();
        if (cfg != null) {
            start(cfg);
        }
    }

    public void deactivate() throws Exception {
        stop();
    }

    private void start(final ServerConfiguration configuration) throws Exception {
        logger.info("Starting Artemis");

        this.server = new ServerManager(configuration);
        this.server.start();

        this.configuration = configuration;
    }

    private void stop() throws Exception {
        logger.info("Stopping Artemis");

        if (this.server != null) {
            this.server.stop();
            this.server = null;
        }

        this.configuration = null;
    }

    private ServerConfiguration parse(final Map<String, Object> properties) throws Exception {

        // is enabled?

        if (!Boolean.TRUE.equals(properties.get("enabled"))) {
            return null;
        }

        // create single user security configuration

        final UserAuthentication.Builder auth = new UserAuthentication.Builder();

        String user = (String) properties.get("user");
        String password = (String) properties.get("password");

        if (user == null || user.isEmpty()) {
            user = "mqtt";
        }

        if (password == null || password.isEmpty()) {
            auth.defaultUser(user);
            password = "";
        } else {
            password = String.valueOf(cryptoService.decryptAes(password.toCharArray()));
        }

        auth.addUser(user, password, Collections.singleton("amq"));

        // create result

        final ServerConfiguration cfg = new ServerConfiguration();
        cfg.setBrokerXml(createBrokerXml(properties));
        cfg.setRequiredProtocols(Collections.singleton("MQTT"));
        cfg.setUserAuthentication(auth.build());
        return cfg;
    }

    private String createBrokerXml(final Map<String, Object> properties) throws Exception {

        try (final InputStream input = Resources.getResource(ServiceComponent.class, "broker.xml").openStream()) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);
            
            final Document document = dbf.newDocumentBuilder().parse(input);

            customizeDocument(document, properties);

            final TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            
            final Transformer transformer = factory.newTransformer();
            final StringWriter sw = new StringWriter();
            final StreamResult result = new StreamResult(sw);
            transformer.transform(new DOMSource(document), result);
            sw.close();

            return sw.toString();
        }
    }

    private void customizeDocument(final Document document, final Map<String, Object> properties) throws Exception {

        Objects.requireNonNull(document);
        Objects.requireNonNull(properties);

        // get bind address

        String address = (String) properties.get("address");
        if (address == null || address.isEmpty()) {
            address = "localhost";
        }

        // get bind port

        Integer port = (Integer) properties.get("port");
        if (port == null) {
            port = 1883;
        }

        // create XPath processor

        final XPath xpath = XPathFactory.newInstance().newXPath();

        // set name

        final Node nameNode = (Node) xpath.evaluate("/configuration/core/name", document, XPathConstants.NODE);
        nameNode.setTextContent("simple-mqtt-broker");

        // set MQTT acceptor

        final Node acceptorNode = (Node) xpath.evaluate("//acceptor[@name='mqtt']", document, XPathConstants.NODE);

        final String mqttAcceptor = String.format(
                "tcp://%s:%s?tcpSendBufferSize=1048576;tcpReceiveBufferSize=1048576;protocols=MQTT", address, port);

        acceptorNode.setTextContent(mqttAcceptor);
    }

}
