package org.eclipse.kura.example.camel.quickstart;

import org.eclipse.kura.camel.router.AbstractXmlCamelComponent;

/**
 * Example of the Kura Camel application.
 */
public class GatewayRouterXml extends AbstractXmlCamelComponent {
	public GatewayRouterXml() {
		super("camel.route.xml");
	}
}