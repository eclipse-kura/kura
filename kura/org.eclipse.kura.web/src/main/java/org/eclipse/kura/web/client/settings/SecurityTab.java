/**
 * Copyright (c) 2011, 2015 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.web.client.settings;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.service.GwtSecurityService;
import org.eclipse.kura.web.shared.service.GwtSecurityServiceAsync;

import com.extjs.gxt.ui.client.widget.Info;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.layout.ColumnLayout;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DecoratorPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

public class SecurityTab extends LayoutContainer {

	private static final Messages MSGS = GWT.create(Messages.class);

	private final GwtSecurityServiceAsync gwtSecurityService = GWT.create(GwtSecurityService.class);


	private GwtSession			m_currentSession;
	private LayoutContainer 	m_commandInput;


	public SecurityTab(GwtSession currentSession) 
	{
		m_currentSession = currentSession;
	}


	protected void onRender(Element parent, int index) {
		super.onRender(parent, index);         
		//setLayout(new FitLayout());
		setId("device-command");

		ClickHandler clickHandler1 = new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				AsyncCallback<Void> callback = new AsyncCallback<Void>() {
					public void onFailure(Throwable caught) {
						Info.display(MSGS.error(), "Error reloading security policy!");
					}

					public void onSuccess(Void result) {
						Info.display(MSGS.info(), "Security policy successfully reloaded!");
					}
				};
				gwtSecurityService.reloadSecurityPolicy(callback);
			}
		};


		VerticalPanel vPanel = new VerticalPanel();
		vPanel.setSpacing(5);

		LayoutContainer description = new LayoutContainer();
		description.setBorders(false);
		description.setLayout(new ColumnLayout());

		Label descriptionLabel = new Label(MSGS.settingsSecurityDescription());
		description.add(descriptionLabel);
		description.setStyleAttribute("padding-bottom", "10px");
		vPanel.add(description);

		Button securityPolicyButton= new Button(MSGS.settingsSecurityReloadPolicy());
		securityPolicyButton.addDomHandler(clickHandler1, ClickEvent.getType());

		FlexTable layout = new FlexTable();
		layout.setCellSpacing(6);
		FlexCellFormatter cellFormatter = layout.getFlexCellFormatter();

		// Add a title
		layout.setHTML(0, 0, MSGS.settingsSecurityReloadPolicyTitle());
		cellFormatter.setColSpan(0, 0, 2);
		cellFormatter.setHorizontalAlignment(0, 0, HasHorizontalAlignment.ALIGN_CENTER);

		// Add description and reload button
		layout.setHTML(1, 0, MSGS.settingsSecurityReloadPolicyDescription());
		layout.setWidget(2, 0, securityPolicyButton);
		cellFormatter.setColSpan(2, 0, 2);
		cellFormatter.setHorizontalAlignment(2, 0, HasHorizontalAlignment.ALIGN_CENTER);

		// Wrap the content in a DecoratorPanel
		DecoratorPanel decPanel = new DecoratorPanel();
		decPanel.setWidget(layout);
		vPanel.add(decPanel);
		
		vPanel.ensureDebugId("cwVerticalPanel");


		add(vPanel);
	}

	public void refresh() {
		m_commandInput.unmask();
	}
}