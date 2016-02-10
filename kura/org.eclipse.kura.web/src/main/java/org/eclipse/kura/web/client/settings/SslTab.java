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
package org.eclipse.kura.web.client.settings;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.resources.Resources;
import org.eclipse.kura.web.client.util.Constants;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.client.util.UserAgentUtils;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtSslConfig;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSslService;
import org.eclipse.kura.web.shared.service.GwtSslServiceAsync;

import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.core.El;
import com.extjs.gxt.ui.client.core.XDOM;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.ComponentPlugin;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Info;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.Field;
import com.extjs.gxt.ui.client.widget.form.FieldSet;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.Radio;
import com.extjs.gxt.ui.client.widget.form.RadioGroup;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.ColumnLayout;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.FlowLayout;
import com.extjs.gxt.ui.client.widget.layout.FormData;
import com.extjs.gxt.ui.client.widget.layout.FormLayout;
import com.extjs.gxt.ui.client.widget.toolbar.SeparatorToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Label;

public class SslTab extends LayoutContainer {

	private static final Messages MSGS = GWT.create(Messages.class);

	private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
	private final GwtSslServiceAsync gwtSslService= GWT.create(GwtSslService.class);

	@SuppressWarnings("unused")
	private GwtSession			m_currentSession;
	private TextField<String>   m_protocol;
	private Radio 				m_trueRadio;
	private Radio 				m_falseRadio;
	private RadioGroup          m_hostnameVerification;
	private TextField<String>   m_keystorePath;
	private TextField<String>   m_keystorePassword;
	private TextField<String>   m_cipherSuites;

	private ToolBar             m_toolBar;
	private Button              m_apply;
	private Button              m_reset;

	private FieldSet            m_actionFieldSet;
	private FormPanel           m_actionFormPanel;

	private ComponentPlugin     m_infoPlugin;
    private ComponentPlugin     m_dirtyPlugin;
	
	private boolean    		    m_dirty;
	private boolean             m_initialized;
	
	private LayoutContainer     m_tabContent;


	public SslTab(GwtSession currentSession) 
	{
		m_currentSession = currentSession;
		m_dirty           = false;
		m_initialized     = false;  

		m_infoPlugin = new ComponentPlugin() {  
			public void init(Component component) {  
				component.addListener(Events.Render, new Listener<ComponentEvent>() {  
					public void handleEvent(ComponentEvent be) {  
						El elem = be.getComponent().el().findParent(".x-form-element", 3);
						
						if (elem != null) {
							// should style in external CSS  rather than directly  
							elem.appendChild(XDOM.create("<div style='color: #615f5f; padding: 1px 25px 5px 0px;'>" + be.getComponent().getData("text") + "</div>"));
						}
					}  
				});  
			}
		};  
		
		final SslTab thePanel = this;
		m_dirtyPlugin = new ComponentPlugin() {  
    		public void init(Component component) {  
    			component.addListener(Events.Change, new Listener<ComponentEvent>() {  
    				public void handleEvent(ComponentEvent be) {  
    					El elem = be.getComponent().el().findParent(".x-form-element", 7);
    					@SuppressWarnings("unchecked")
						Field<Object> fe=(Field<Object>) be.getSource();
    					
    					if(fe.isDirty()){
	    					El dirtyIcon= elem.createChild("");
	    					dirtyIcon.setStyleName("x-grid3-dirty-cell");
	    					dirtyIcon.setStyleAttribute("top", "0");
	    					dirtyIcon.setStyleAttribute("position", "absolute");
	    					dirtyIcon.setSize(10, 10);
	    					dirtyIcon.show();
    					} else {
    						Element child= elem.getChildElement(2);
    						if (child != null){
    							elem.removeChild(child);
    						}
    					}
    					
    					thePanel.fireEvent(Events.Change);
    				}  
    			});  
  	      	}  
  	    };
	}


	protected void onRender(Element parent, int index) {
		super.onRender(parent, index);         
		setLayout(new FitLayout());
		setId("settings-ssl");

		initToolBar();
		initTabContent();

		// Main Panel
		ContentPanel deviceCommandPanel = new ContentPanel();
		deviceCommandPanel.setBorders(false);
		deviceCommandPanel.setBodyBorder(false);
		deviceCommandPanel.setHeaderVisible(false);
		deviceCommandPanel.setScrollMode(Scroll.AUTO);
		deviceCommandPanel.setLayout(new FitLayout());
		deviceCommandPanel.add(m_tabContent);
		deviceCommandPanel.setTopComponent(m_toolBar);

		add(deviceCommandPanel);
		m_initialized = true;
	}
	
	protected void onDetach()
	{
		m_dirty = false;
	}

	private void initToolBar() 
	{	
		m_toolBar = new ToolBar();
		m_toolBar.setBorders(true);
		m_toolBar.setId("settings-ssl-toolbar");

		m_apply = new Button(MSGS.apply(), 
				AbstractImagePrototype.create(Resources.INSTANCE.accept()),
				new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				apply();
			}
		});

		m_reset = new Button(MSGS.reset(), 
				AbstractImagePrototype.create(Resources.INSTANCE.cancel()),
				new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				reset();
			}
		});

		m_apply.setEnabled(false);
		m_reset.setEnabled(false);

		m_toolBar.add(m_apply);
		m_toolBar.add(new SeparatorToolItem());
		m_toolBar.add(m_reset);
	}

	// --------------------------------------------------------------------------------------
	//
	//    Device Configuration Management
	//
	// --------------------------------------------------------------------------------------    

	public boolean isDirty()
	{
		return m_dirty;
	}

	public void setDirty(boolean dirty)
	{
		m_dirty = dirty;
	}

	public void reset() {
		loadData();
		refresh();
	}

	public void apply(){
		m_tabContent.mask();
		gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {
			@Override
			public void onFailure(Throwable ex) {
				FailureHandler.handle(ex);
			}

			@Override
			public void onSuccess(GwtXSRFToken token) {	
				GwtSslConfig sslConfig= new GwtSslConfig();
				sslConfig.setProtocol(m_protocol.getValue());
				if(m_hostnameVerification.getValue() == m_trueRadio){
					sslConfig.setHostnameVerification(true);
				} else {
					sslConfig.setHostnameVerification(false);
				}
				sslConfig.setKeyStore(m_keystorePath.getValue());
				sslConfig.setKeystorePassword(m_keystorePassword.getValue());
				sslConfig.setCiphers(m_cipherSuites.getValue());

				gwtSslService.updateSslConfiguration(token, sslConfig, new AsyncCallback<Void>() {
					public void onFailure(Throwable caught) {
						if(caught.getLocalizedMessage().equals(GwtKuraErrorCode.ILLEGAL_ARGUMENT.toString())){
							Info.display(MSGS.error(), "Error while updating service configuration.");
						}else{
							Info.display(MSGS.error(), caught.getLocalizedMessage());
						}
						m_tabContent.unmask();
					}

					public void onSuccess(Void result) {
						Info.display(MSGS.info(), "Service updated");
						refresh();
						m_tabContent.unmask();
					}
				});
			}});
	}

	public void refresh() {

		if (m_dirty && m_initialized) {

			// clear the tree and disable the toolbar
			m_apply.setEnabled(false);
			m_reset.setEnabled(false);
			m_dirty = false;
		}
	}
	
	private void initTabContent(){
		LayoutContainer lcAction = new LayoutContainer();
		lcAction.setLayout(new BorderLayout());
		lcAction.setBorders(true);
		lcAction.setSize(475, -1);


		// center panel: action form
		BorderLayoutData centerData = new BorderLayoutData(LayoutRegion.CENTER, .75F);
		centerData.setSplit(false);  
		centerData.setMargins(new Margins(0, 0, 0, 0));

		FormData formData = new FormData("-20");
		formData.setMargins(new Margins(0, 10, 0, 0));

		if (!UserAgentUtils.isIE()) {
			m_actionFormPanel = new FormPanel();
			m_actionFormPanel.setId("config-panel-id");
			m_actionFormPanel.setFrame(false);
			m_actionFormPanel.setBodyBorder(false);
			m_actionFormPanel.setHeaderVisible(false);
			m_actionFormPanel.setLabelWidth(Constants.LABEL_WIDTH_CONFIG_FORM);
			m_actionFormPanel.setStyleAttribute("padding", "0px");
			m_actionFormPanel.setScrollMode(Scroll.AUTO);
			m_actionFormPanel.setLayout(new FlowLayout());
			m_actionFormPanel.addListener(Events.Render, new Listener<BaseEvent>() {
				public void handleEvent(BaseEvent be) {
					NodeList<com.google.gwt.dom.client.Element> nl = m_actionFormPanel.getElement().getElementsByTagName("form");
					if (nl.getLength() > 0) {
						com.google.gwt.dom.client.Element elemForm = nl.getItem(0);
						elemForm.setAttribute("autocomplete", "off");

					}
				}
			});
			m_actionFormPanel.getElement().setAttribute("autocomplete", "off");
		}

		m_actionFieldSet = new FieldSet();
		m_actionFieldSet.setId("configuration-form");
		m_actionFieldSet.setBorders(false);
		m_actionFieldSet.setStyleAttribute("padding", "0px");
		m_actionFieldSet.setScrollMode(Scroll.AUTO);

		this.addListener(Events.Change, new Listener<BaseEvent>() {
			public void handleEvent(BaseEvent be) {
				m_apply.setEnabled(true);
				m_reset.setEnabled(true);
				m_dirty = true;
			}
		});


		FormLayout layout = new FormLayout();
		layout.setLabelWidth(Constants.LABEL_WIDTH_CONFIG_FORM);
		m_actionFieldSet.setLayout(layout);

		
		loadData();
		
		//
		// Initial description
		// 
		LayoutContainer description = new LayoutContainer();
		description.setBorders(false);
		description.setLayout(new ColumnLayout());

		Label descriptionLabel = new Label(MSGS.settingsSSLConfigurationDescription());

		description.add(descriptionLabel);
		description.setStyleAttribute("padding-bottom", "10px");
		m_actionFieldSet.add(description);

		//
		// SSL protocol
		//
		m_protocol= new TextField<String>();
		m_protocol.setName(MSGS.settingsSSLConfigurationProtocol());
		m_protocol.setPassword(false);
		m_protocol.setAllowBlank(false);
		m_protocol.setFieldLabel(MSGS.settingsSSLConfigurationProtocol());
		m_protocol.addPlugin(m_dirtyPlugin);
		m_protocol.addPlugin(m_infoPlugin);
		m_protocol.setData("text", MSGS.settingsSSLConfigurationProtocolDescr()); 
		m_actionFieldSet.add(m_protocol, formData);

		//
		// SSL hostname verification
		//
		m_trueRadio = new Radio();  
		m_trueRadio.setBoxLabel(MSGS.trueLabel());
		m_trueRadio.setItemId("true");
		
		m_falseRadio = new Radio();  
		m_falseRadio.setBoxLabel(MSGS.falseLabel());
		m_falseRadio.setValueAttribute("false");

		m_hostnameVerification = new RadioGroup();  
		m_hostnameVerification.setName("Hostname Verification");  
		m_hostnameVerification.setItemId("Hostname Verification");
		m_hostnameVerification.setFieldLabel(MSGS.settingsSSLConfigurationHostnameVerification());  
		m_hostnameVerification.add(m_trueRadio);  
		m_hostnameVerification.add(m_falseRadio);
		m_hostnameVerification.addPlugin(m_infoPlugin);
		m_hostnameVerification.addPlugin(m_dirtyPlugin);
		m_hostnameVerification.setData("text", MSGS.settingsSSLConfigurationHostnameVerificationDescr());  
		m_actionFieldSet.add(m_hostnameVerification, formData);

		//
		// SSL keystore
		//
		m_keystorePath = new TextField<String>();
		m_keystorePath.setName(MSGS.settingsSSLConfigurationKeystorePath());
		m_keystorePath.setPassword(false);
		m_keystorePath.setAllowBlank(false);
		m_keystorePath.setFieldLabel(MSGS.settingsSSLConfigurationKeystorePath());
		m_keystorePath.addPlugin(m_dirtyPlugin);
		m_keystorePath.addPlugin(m_infoPlugin);
		m_keystorePath.setData("text", MSGS.settingsSSLConfigurationKeystorePathDescr());  
		m_actionFieldSet.add(m_keystorePath, formData);

		//
		// SSL keystore password
		//
		m_keystorePassword = new TextField<String>();
		m_keystorePassword.setName(MSGS.settingsSSLConfigurationKeystorePassword());
		m_keystorePassword.setPassword(true);
		m_keystorePassword.setAllowBlank(false);
		m_keystorePassword.setFieldLabel(MSGS.settingsSSLConfigurationKeystorePassword());
		m_keystorePassword.addPlugin(m_dirtyPlugin);
		m_keystorePassword.addPlugin(m_infoPlugin);
		m_keystorePassword.setData("text", MSGS.settingsSSLConfigurationKeystorePasswordDescr());  
		m_actionFieldSet.add(m_keystorePassword, formData);

		//
		// SSL cipher suites
		//
		m_cipherSuites = new TextField<String>();
		m_cipherSuites.setName(MSGS.settingsSSLConfigurationCipherSuites());
		m_cipherSuites.setPassword(false);
		m_cipherSuites.setAllowBlank(true);
		m_cipherSuites.setFieldLabel(MSGS.settingsSSLConfigurationCipherSuites());
		m_cipherSuites.addPlugin(m_dirtyPlugin);
		m_cipherSuites.addPlugin(m_infoPlugin);
		m_cipherSuites.setData("text", MSGS.settingsSSLConfigurationCipherSuitesDescr());  
		m_actionFieldSet.add(m_cipherSuites, formData);


		if (!UserAgentUtils.isIE()) {
			m_actionFormPanel.add(m_actionFieldSet, formData);
			lcAction.add(m_actionFormPanel, centerData);
		}
		else {
			lcAction.add(m_actionFieldSet, centerData);
		}
		m_tabContent= lcAction;
	}
	
	private void loadData(){
		//
		// Retrieve content
		//
		gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {
			@Override
			public void onFailure(Throwable ex) {
				FailureHandler.handle(ex);
			}

			@Override
			public void onSuccess(GwtXSRFToken token) {	
				gwtSslService.getSslConfiguration(token, new AsyncCallback<GwtSslConfig>() {
					public void onFailure(Throwable caught) {
						if(caught.getLocalizedMessage().equals(GwtKuraErrorCode.ILLEGAL_ARGUMENT.toString())){
							Info.display(MSGS.error(), "Error while retrieving data");
						}else{
							Info.display(MSGS.error(), caught.getLocalizedMessage());
						}
					}

					public void onSuccess(GwtSslConfig sslConfig) {
						m_protocol.setValue(sslConfig.getProtocol());
						m_protocol.setOriginalValue(sslConfig.getProtocol());
						if(sslConfig.isHostnameVerification()){
							m_hostnameVerification.setOriginalValue(m_trueRadio);
							m_hostnameVerification.setValue(m_trueRadio);
	
						} else {
							m_hostnameVerification.setOriginalValue(m_falseRadio);
							m_hostnameVerification.setValue(m_falseRadio);
						}
						
						m_keystorePath.setValue(sslConfig.getKeyStore());
						m_keystorePath.setOriginalValue(sslConfig.getKeyStore());
						m_keystorePassword.setValue(sslConfig.getKeystorePassword());
						m_keystorePassword.setOriginalValue(sslConfig.getKeystorePassword());
						m_cipherSuites.setValue(sslConfig.getCiphers());
						m_cipherSuites.setOriginalValue(sslConfig.getCiphers());
					}
				});
			}});
	}
}