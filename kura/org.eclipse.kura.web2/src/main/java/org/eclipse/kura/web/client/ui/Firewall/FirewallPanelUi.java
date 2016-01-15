package org.eclipse.kura.web.client.bootstrap.ui.Firewall;

import org.eclipse.kura.web.client.messages.Messages;
import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.Well;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

public class FirewallPanelUi extends Composite {

	private static FirewallPanelUiUiBinder uiBinder = GWT
			.create(FirewallPanelUiUiBinder.class);

	interface FirewallPanelUiUiBinder extends UiBinder<Widget, FirewallPanelUi> {
	}
	
	private static OpenPortsTabUi openPortsBinder = GWT.create(OpenPortsTabUi.class);
	private static PortForwardingTabUi portForwardingBinder = GWT.create(PortForwardingTabUi.class);
	private static NatTabUi natBinder = GWT.create(NatTabUi.class);
	
	private static final Messages MSGS = GWT.create(Messages.class);
	@UiField
	HTMLPanel firewallIntro;
	@UiField
	AnchorListItem openPorts, portForwarding, ipForwarding; 
	@UiField
	Well content;
	
	public FirewallPanelUi() {
		initWidget(uiBinder.createAndBindUi(this));
		firewallIntro.add(new Span("<p>"+MSGS.firewallIntro()+"</p>"));
		openPorts.setText(MSGS.firewallOpenPorts());
		portForwarding.setText(MSGS.firewallPortForwarding());
		ipForwarding.setText(MSGS.firewallNat());
		
		//set open ports selected by default
		setSelectedActive(openPorts);
		content.clear();
		content.add(openPortsBinder);
		
		openPorts.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent event) {
				setSelectedActive(openPorts);
				content.clear();
				content.add(openPortsBinder);
			}});
		
		portForwarding.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent event) {
				setSelectedActive(portForwarding);
				content.clear();
				content.add(portForwardingBinder);
			}});
		
		ipForwarding.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent event) {
				setSelectedActive(ipForwarding);
				content.clear();
				content.add(natBinder);
			}});
	}
	
	
	public void setSelectedActive(AnchorListItem item){
		openPorts.setActive(false);
		portForwarding.setActive(false);
		ipForwarding.setActive(false);
		item.setActive(true);
	}

	
}
