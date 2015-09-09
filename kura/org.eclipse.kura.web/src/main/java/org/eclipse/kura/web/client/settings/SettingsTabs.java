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

import org.eclipse.kura.web.client.configuration.ServiceTree;
import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.resources.Resources;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtSecurityService;
import org.eclipse.kura.web.shared.service.GwtSecurityServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;

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

	private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
	private final GwtSecurityServiceAsync gwtSecurityService = GWT.create(GwtSecurityService.class);

	private GwtSession              m_currentSession;
	private ServiceTree             m_servicesTree;

	private TabPanel                m_tabsPanel;
	private TabItem                 m_certificatesConfig;
	private TabItem                 m_mutualAuthConfig;
	private TabItem                 m_snapshotsConfig;
	private TabItem                 m_securityConfig;

	private SnapshotsTab	        m_snapshotsTab;

	private CertificatesTab         m_certificatesTab;
	private MutualAuthenticationTab m_mutualAuthenticationTab;

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
		m_certificatesTab = new CertificatesTab(m_currentSession);
		if (m_certificatesConfig != null) {
			m_certificatesConfig.add(m_certificatesTab);
			m_certificatesConfig.layout();
		}

		m_mutualAuthenticationTab = new MutualAuthenticationTab(m_currentSession);
		if (m_mutualAuthConfig != null) {
			m_mutualAuthConfig.add(m_mutualAuthenticationTab);
			m_mutualAuthConfig.layout();
		}

		m_snapshotsTab = new SnapshotsTab(m_currentSession, m_servicesTree);
		if (m_snapshotsConfig != null) {
			m_snapshotsConfig.add(m_snapshotsTab);
			m_snapshotsConfig.layout();
		}


		Log.info("security service available inittabs");
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
		m_tabsPanel.add(m_snapshotsConfig);

		m_certificatesConfig = new TabItem(MSGS.settingsAddCertificates());
		m_certificatesConfig.setBorders(true);
		m_certificatesConfig.setLayout(new FitLayout());
		m_certificatesConfig.add(m_certificatesTab);
		m_tabsPanel.add(m_certificatesConfig);

		m_mutualAuthConfig = new TabItem(MSGS.settingsAddMAuthCertificates());
		m_mutualAuthConfig.setBorders(true);
		m_mutualAuthConfig.setLayout(new FitLayout());
		m_mutualAuthConfig.add(m_mutualAuthenticationTab);
		m_tabsPanel.add(m_mutualAuthConfig);

		m_securityConfig = new TabItem(MSGS.settingsSecurityOptions());
		m_securityConfig.setBorders(true);
		m_securityConfig.setLayout(new FitLayout());
		m_securityConfig.add(m_securityTab);


		AsyncCallback<Boolean> callback = new AsyncCallback<Boolean>() {
			public void onFailure(Throwable caught) {
			}

			public void onSuccess(Boolean result) {
				if(result){
					m_tabsPanel.add(m_securityConfig);
				}
			}
		};
		gwtSecurityService.isSecurityServiceAvailable(callback);



		add(m_tabsPanel);
	}
}
