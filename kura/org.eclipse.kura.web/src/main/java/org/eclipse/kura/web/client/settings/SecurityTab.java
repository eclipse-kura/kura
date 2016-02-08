/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.settings;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtSecurityService;
import org.eclipse.kura.web.shared.service.GwtSecurityServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;

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
import com.google.gwt.user.server.rpc.XsrfProtect;

@XsrfProtect
public class SecurityTab extends LayoutContainer {

	private static final Messages MSGS = GWT.create(Messages.class);

	private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
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
				gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {
					@Override
					public void onFailure(Throwable ex) {
						FailureHandler.handle(ex);
					}

					@Override
					public void onSuccess(GwtXSRFToken token) {	
						gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {
							@Override
							public void onFailure(Throwable ex) {
								FailureHandler.handle(ex);
							}

							@Override
							public void onSuccess(GwtXSRFToken token) {
								AsyncCallback<Void> callback = new AsyncCallback<Void>() {
									public void onFailure(Throwable caught) {
										Info.display(MSGS.error(), "Error reloading security policy fingerprint!");
									}

									public void onSuccess(Void result) {
										Info.display(MSGS.info(), "Fingerprint successfully reloaded!");
									}
								};
								gwtSecurityService.reloadSecurityPolicyFingerprint(token, callback);
							}
						});

					}});

			}
		};

		ClickHandler clickHandler2 = new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {
					@Override
					public void onFailure(Throwable ex) {
						FailureHandler.handle(ex);
					}

					@Override
					public void onSuccess(GwtXSRFToken token) {	
						gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {
							@Override
							public void onFailure(Throwable ex) {
								FailureHandler.handle(ex);
							}

							@Override
							public void onSuccess(GwtXSRFToken token) {
								AsyncCallback<Void> callback = new AsyncCallback<Void>() {
									public void onFailure(Throwable caught) {
										Info.display(MSGS.error(), "Error reloading command line fingerprint!");
									}

									public void onSuccess(Void result) {
										Info.display(MSGS.info(), "Fingerprint successfully reloaded!");
									}
								};
								gwtSecurityService.reloadCommandLineFingerprint(token, callback);
							}
						});

					}});
			}
		};

		
		VerticalPanel vPanel = new VerticalPanel();
		vPanel.setSpacing(5);
		vPanel.setWidth("100%");

		LayoutContainer description = new LayoutContainer();
		description.setBorders(false);
		description.setLayout(new ColumnLayout());

		Label descriptionLabel = new Label(MSGS.settingsSecurityDescription());
		description.add(descriptionLabel);
		description.setStyleAttribute("padding-bottom", "10px");
		vPanel.add(description);

		
		VerticalPanel buttonsVPanel = new VerticalPanel();
		buttonsVPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		buttonsVPanel.setWidth("100%");
		buttonsVPanel.setHeight("100%");
		buttonsVPanel.setSpacing(5);
		
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
		decPanel.setWidth("100%");
		decPanel.setHeight("100%");
		buttonsVPanel.add(decPanel);
		
		
		//Button to reload start script fingerprint
		Button startScriptReloadButton= new Button(MSGS.settingsReloadStartupFingerprint());
		startScriptReloadButton.addDomHandler(clickHandler2, ClickEvent.getType());

		FlexTable startScriptLayout = new FlexTable();
		startScriptLayout.setCellSpacing(6);
		FlexCellFormatter startScriptCellFormatter = startScriptLayout.getFlexCellFormatter();

		// Add a title
		startScriptLayout.setHTML(0, 0, MSGS.settingsReloadStartupFingerprintTitle());
		startScriptCellFormatter.setColSpan(0, 0, 2);
		startScriptCellFormatter.setHorizontalAlignment(0, 0, HasHorizontalAlignment.ALIGN_CENTER);

		// Add description and reload button
		startScriptLayout.setHTML(1, 0, MSGS.settingsReloadStartupFingerprintDescription());
		startScriptLayout.setWidget(2, 0, startScriptReloadButton);
		startScriptCellFormatter.setColSpan(2, 0, 2);
		startScriptCellFormatter.setHorizontalAlignment(2, 0, HasHorizontalAlignment.ALIGN_CENTER);

		// Wrap the content in a DecoratorPanel
		DecoratorPanel startScriptDecPanel = new DecoratorPanel();
		startScriptDecPanel.setWidth("100%");
		startScriptDecPanel.setHeight("100%");
		startScriptDecPanel.setWidget(startScriptLayout);

		buttonsVPanel.add(startScriptDecPanel);
		
		
		vPanel.add(buttonsVPanel);
		vPanel.ensureDebugId("cwVerticalPanel");


		add(vPanel);
	}

	public void refresh() {
		m_commandInput.unmask();
	}
}