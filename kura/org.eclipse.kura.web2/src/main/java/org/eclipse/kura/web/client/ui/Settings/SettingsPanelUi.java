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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.shared.model.GwtSession;
import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.Well;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class SettingsPanelUi extends Composite {

	private static SettingsPanelUiUiBinder uiBinder = GWT.create(SettingsPanelUiUiBinder.class);
	private static final Logger logger = Logger.getLogger(SettingsPanelUi.class.getSimpleName());

	interface SettingsPanelUiUiBinder extends UiBinder<Widget, SettingsPanelUi> {
	}
	private static SnapshotsTabUi snapshotsBinder = GWT.create(SnapshotsTabUi.class);
	private static ApplicationCertsTabUi appCertBinder = GWT.create(ApplicationCertsTabUi.class);
	private static SslTabUi sslConfigBinder = GWT.create(SslTabUi.class);
	private static ServerCertsTabUi serverCertBinder = GWT.create(ServerCertsTabUi.class);
	private static DeviceCertsTabUi deviceCertBinder = GWT.create(DeviceCertsTabUi.class);
	private static SecurityTabUi securityBinder = GWT.create(SecurityTabUi.class);
	
	GwtSession Session;
	@UiField
	AnchorListItem snapshots, appCert, sslConfig, serverCert, deviceCert, security;
	@UiField
	Well content;

	public SettingsPanelUi() {
		logger.log(Level.FINER, "Initiating SettingsPanelUI...");
		initWidget(uiBinder.createAndBindUi(this));
		setSelectedActive(snapshots);
		content.clear();
		content.add(snapshotsBinder);
		
		snapshots.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent event) {
				setSelectedActive(snapshots);
				content.clear();
				content.add(snapshotsBinder);
				snapshotsBinder.refresh();
			}});
		
		appCert.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent event) {
				setSelectedActive(appCert);
				content.clear();
				content.add(appCertBinder);
				appCertBinder.refresh();
			}});
		
		sslConfig.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent event) {
				setSelectedActive(sslConfig);
				content.clear();
				content.add(sslConfigBinder);
				sslConfigBinder.refresh();
			}});
		
		serverCert.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent event) {
				setSelectedActive(serverCert);
				content.clear();
				content.add(serverCertBinder);
				serverCertBinder.refresh();
			}});
		
		deviceCert.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent event) {
				setSelectedActive(deviceCert);
				content.clear();
				content.add(deviceCertBinder);
				deviceCertBinder.refresh();
			}});
		
		security.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent event) {
				setSelectedActive(security);
				content.clear();
				content.add(securityBinder);
				securityBinder.refresh();
			}});
	}

	public void load() {
		setSelectedActive(snapshots);
		content.clear();
		content.add(snapshotsBinder);
		snapshotsBinder.refresh();
	}
	
	public void setSession(GwtSession currentSession) {
		Session = currentSession;
	}

	public void setSelectedActive(AnchorListItem item){
		snapshots.setActive(false);
		appCert.setActive(false);
		sslConfig.setActive(false);
		serverCert.setActive(false);
		deviceCert.setActive(false);
		security.setActive(false);
		item.setActive(true);	
	}
}
