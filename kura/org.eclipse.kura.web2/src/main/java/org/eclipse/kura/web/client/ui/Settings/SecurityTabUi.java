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
package org.eclipse.kura.web.client.ui.Settings;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.ui.Tab;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtSecurityService;
import org.eclipse.kura.web.shared.service.GwtSecurityServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Anchor;
import org.gwtbootstrap3.client.ui.AnchorButton;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.PanelCollapse;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

public class SecurityTabUi extends Composite implements Tab {

	private static SecurityTabUiUiBinder uiBinder = GWT.create(SecurityTabUiUiBinder.class);

	interface SecurityTabUiUiBinder extends UiBinder<Widget, SecurityTabUi> {
	}
	
	private static final Messages MSGS = GWT.create(Messages.class);
	
	private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
	private final GwtSecurityServiceAsync gwtSecurityService = GWT.create(GwtSecurityService.class);

	@UiField
	HTMLPanel description;
	@UiField
	Anchor collapseOneAnchor;
	@UiField
	Anchor collapseTwoAnchor;
	@UiField
	PanelCollapse collapseOne;
	@UiField
	PanelCollapse collapseTwo;
	@UiField
	FormLabel securityPolicyLabel;
	@UiField
	FormLabel commandLineLabel;
	@UiField
	AnchorButton reloadPolicyFingerprint;
	@UiField
	AnchorButton reloadCommandLineFingerprint;
	
	public SecurityTabUi() {
		initWidget(uiBinder.createAndBindUi(this));
		initTab();
	}

	@Override
	public void setDirty(boolean flag) {
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public void refresh() {
		if (isDirty()) {
			setDirty(false);
			reset();
		}
	}	
	
	private void initTab() {
		StringBuilder title= new StringBuilder();
		title.append("<p>");
		title.append(MSGS.settingsSecurityDescription());
		title.append("</p>");
		description.add(new Span(title.toString()));
		
		collapseOneAnchor.setText(MSGS.settingsSecurityReloadPolicyTitle());
		collapseTwoAnchor.setText(MSGS.settingsReloadStartupFingerprintTitle());
		
		securityPolicyLabel.setText(MSGS.settingsSecurityReloadPolicyDescription());
		commandLineLabel.setText(MSGS.settingsReloadStartupFingerprintDescription());
		
		reloadPolicyFingerprint.setText(MSGS.settingsSecurityReloadPolicy());
		reloadPolicyFingerprint.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent event) {
				EntryClassUi.showWaitModal();
				
				gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {
					@Override
					public void onFailure(Throwable ex) {
						FailureHandler.handle(ex);
						EntryClassUi.hideWaitModal();
					}

					@Override
					public void onSuccess(GwtXSRFToken token) {	
						gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {
							@Override
							public void onFailure(Throwable ex) {
								FailureHandler.handle(ex);
								EntryClassUi.hideWaitModal();
							}

							@Override
							public void onSuccess(GwtXSRFToken token) {
								AsyncCallback<Void> callback = new AsyncCallback<Void>() {
									public void onFailure(Throwable caught) {
										FailureHandler.handle(caught);
										EntryClassUi.hideWaitModal();
									}

									public void onSuccess(Void result) {
										EntryClassUi.hideWaitModal();
									}
								};
								gwtSecurityService.reloadSecurityPolicyFingerprint(token, callback);
							}
						});

					}});
			}
		});
		
		reloadCommandLineFingerprint.setText(MSGS.settingsSecurityReloadPolicy());
		reloadCommandLineFingerprint.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent event) {
				EntryClassUi.showWaitModal();
				
				gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {
					@Override
					public void onFailure(Throwable ex) {
						FailureHandler.handle(ex);
						EntryClassUi.hideWaitModal();
					}

					@Override
					public void onSuccess(GwtXSRFToken token) {	
						gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {
							@Override
							public void onFailure(Throwable ex) {
								FailureHandler.handle(ex);
								EntryClassUi.hideWaitModal();
							}

							@Override
							public void onSuccess(GwtXSRFToken token) {
								AsyncCallback<Void> callback = new AsyncCallback<Void>() {
									public void onFailure(Throwable caught) {
										FailureHandler.handle(caught);
										EntryClassUi.hideWaitModal();
									}

									public void onSuccess(Void result) {
										EntryClassUi.hideWaitModal();
									}
								};
								gwtSecurityService.reloadCommandLineFingerprint(token, callback);
							}
						});

					}});
			}
		});
	}
	
	private void reset() {
	}
}
