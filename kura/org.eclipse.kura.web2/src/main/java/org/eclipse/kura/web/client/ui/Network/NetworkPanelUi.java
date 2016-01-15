package org.eclipse.kura.web.client.bootstrap.ui.Network;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.shared.model.GwtBSSession;
import org.gwtbootstrap3.client.ui.Container;
import org.gwtbootstrap3.client.ui.PanelBody;
import org.gwtbootstrap3.client.ui.Well;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

public class NetworkPanelUi extends Composite {

	private static NetworkPanelUiUiBinder uiBinder = GWT
			.create(NetworkPanelUiUiBinder.class);

	interface NetworkPanelUiUiBinder extends UiBinder<Widget, NetworkPanelUi> {
	}

	private static final Messages MSGS = GWT.create(Messages.class);
	
	GwtBSSession session;
	private NetworkInterfacesTableUi table;
	private NetworkButtonBarUi buttons;
	private NetworkTabsUi tabs;

	@UiField
	HTMLPanel networkIntro;

	@UiField
	Well interfacesTable;
	@UiField
	PanelBody tabsPanel;
	@UiField
	Container buttonBar;
	
	
	public NetworkPanelUi() {
		initWidget(uiBinder.createAndBindUi(this));	
		networkIntro.add(new Span("<p>"+MSGS.netIntro()+"</p>"));
		
		tabs=new NetworkTabsUi(session);
		tabsPanel.add(tabs);
		
		table=new NetworkInterfacesTableUi(session, tabs);
		interfacesTable.add(table);
		
		buttons= new NetworkButtonBarUi(session, tabs,table);
		buttonBar.add(buttons);
		
		tabs.setDirty(false);		
	}

	public boolean isDirty(){
		return tabs.isDirty();		
	}
	
	public void setSession(GwtBSSession currentSession) {
		this.session = currentSession;
	}

	public void setDirty(boolean b) {
		tabs.setDirty(b);
	}

}
