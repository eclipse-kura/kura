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

import org.eclipse.kura.web.client.configuration.ServiceTree;
import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.resources.Resources;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.service.GwtSecurityService;
import org.eclipse.kura.web.shared.service.GwtSecurityServiceAsync;

import com.allen_sauer.gwt.log.client.Log;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.TabItem;
import com.extjs.gxt.ui.client.widget.TabPanel;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.server.rpc.XsrfProtect;

@XsrfProtect
public class SettingsTabs extends LayoutContainer 
{
	private static final Messages MSGS = GWT.create(Messages.class);

	private final GwtSecurityServiceAsync gwtSecurityService = GWT.create(GwtSecurityService.class);

	private GwtSession              m_currentSession;
	private ServiceTree             m_servicesTree;

	private TabPanel                m_tabsPanel;
	private TabItem                 m_snapshotsConfig;
	private TabItem                 m_applicationCertsConfig;
	private TabItem                 m_sslConfig;
	private TabItem                 m_serverSSLConfig;
	private TabItem                 m_deviceSSLConfig;
	private TabItem                 m_securityConfig;

	private SnapshotsTab	        m_snapshotsTab;
	private ApplicationCertsTab		m_bundleCertsTab;

	private SslTab          		m_sslTab;
	private ServerCertsTab          m_serverSSLTab;
	private DeviceCertsTab          m_deviceSSLTab;

	private SecurityTab				m_securityTab;

	public SettingsTabs(GwtSession currentSession,
			ServiceTree serviceTree) 
	{    
		m_currentSession = currentSession;
		m_servicesTree   = serviceTree;

		Log.debug("about to get the firewall configuration");
		initTabs();

	}


	private void initTabs()
	{
		m_snapshotsTab = new SnapshotsTab(m_currentSession, m_servicesTree);
		if (m_snapshotsConfig != null) {
			m_snapshotsConfig.add(m_snapshotsTab);
			m_snapshotsConfig.layout();
		}

		m_bundleCertsTab = new ApplicationCertsTab(m_currentSession);
		if (m_applicationCertsConfig != null) {
			m_applicationCertsConfig.add(m_bundleCertsTab);
		}

		m_sslTab = new SslTab(m_currentSession);
		if (m_sslTab != null) {
			m_sslTab.add(m_sslTab);
			m_sslTab.layout();
		}
		
		m_serverSSLTab = new ServerCertsTab(m_currentSession);
		if (m_serverSSLConfig != null) {
			m_serverSSLConfig.add(m_serverSSLTab);
			m_serverSSLConfig.layout();
		}

		m_deviceSSLTab = new DeviceCertsTab(m_currentSession);
		if (m_deviceSSLConfig != null) {
			m_deviceSSLConfig.add(m_deviceSSLTab);
			m_deviceSSLConfig.layout();
		}

		m_securityTab = new SecurityTab(m_currentSession);
		if (m_securityConfig != null) {
			m_securityConfig.add(m_securityTab);
		}
	}

	public boolean isDirty() {
		return false;
	}

	protected void onRender(Element parent, int index) 
	{        
		super.onRender(parent, index);

		setId("settings-tabs-wrapper");
		setLayout(new FitLayout());

		m_tabsPanel = new TabPanel();
		m_tabsPanel.setPlain(true);
		m_tabsPanel.setBorders(false);
		m_tabsPanel.setBodyBorder(false);
		m_tabsPanel.setStyleAttribute("padding-top", "5px");

		m_snapshotsConfig = new TabItem(MSGS.settingsSnapshots());
		m_snapshotsConfig.setIcon(AbstractImagePrototype.create(Resources.INSTANCE.snapshots()));
		m_snapshotsConfig.setBorders(true);
		m_snapshotsConfig.setLayout(new FitLayout());
		m_snapshotsConfig.add(m_snapshotsTab);

		m_applicationCertsConfig = new TabItem(MSGS.settingsAddBundleCerts());
		m_applicationCertsConfig.setBorders(true);
		m_applicationCertsConfig.setLayout(new FitLayout());
		m_applicationCertsConfig.add(m_bundleCertsTab);
		
		m_sslConfig = new TabItem(MSGS.settingsSSLConfiguration());
		m_sslConfig.setBorders(true);
		m_sslConfig.setLayout(new FitLayout());
		m_sslConfig.add(m_sslTab);

		m_serverSSLConfig = new TabItem(MSGS.settingsAddCertificates());
		m_serverSSLConfig.setBorders(true);
		m_serverSSLConfig.setLayout(new FitLayout());
		m_serverSSLConfig.add(m_serverSSLTab);

		m_deviceSSLConfig = new TabItem(MSGS.settingsAddMAuthCertificates());
		m_deviceSSLConfig.setBorders(true);
		m_deviceSSLConfig.setLayout(new FitLayout());
		m_deviceSSLConfig.add(m_deviceSSLTab);

		m_securityConfig = new TabItem(MSGS.settingsSecurityOptions());
		m_securityConfig.setBorders(true);
		m_securityConfig.setLayout(new FitLayout());
		m_securityConfig.add(m_securityTab);



		AsyncCallback<Boolean> callback = new AsyncCallback<Boolean>() {
			public void onFailure(Throwable caught) {
				m_tabsPanel.add(m_snapshotsConfig);
				m_tabsPanel.add(m_sslConfig);
				m_tabsPanel.add(m_serverSSLConfig);
				m_tabsPanel.add(m_deviceSSLConfig);
			}

			public void onSuccess(Boolean result) {
				m_tabsPanel.add(m_snapshotsConfig);
				if(result){
					m_tabsPanel.add(m_applicationCertsConfig);
				}
				m_tabsPanel.add(m_sslConfig);
				m_tabsPanel.add(m_serverSSLConfig);
				m_tabsPanel.add(m_deviceSSLConfig);
				if(result){
					m_tabsPanel.add(m_securityConfig);
				}
			}
		};
		gwtSecurityService.isSecurityServiceAvailable(callback);

		add(m_tabsPanel);
	}
}
