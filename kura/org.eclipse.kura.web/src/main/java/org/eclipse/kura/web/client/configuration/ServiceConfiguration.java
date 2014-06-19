/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.web.client.configuration;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.resources.Resources;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.service.GwtComponentService;
import org.eclipse.kura.web.shared.service.GwtComponentServiceAsync;

import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.MessageBoxEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.Info;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.toolbar.SeparatorToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

public class ServiceConfiguration extends LayoutContainer 
{

	private static final Messages MSGS = GWT.create(Messages.class);

	private final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);
	
	@SuppressWarnings("unused")
	private GwtSession           m_currentSession;
	private GwtConfigComponent   m_configComponent;
	
    private boolean    		     m_dirty;
    private boolean              m_initialized;

    private ServiceTree          m_serviceTree;
    private DeviceConfigPanel    m_devConfPanel;

    private ToolBar              m_toolBar;
    private Button               m_apply;
    private Button               m_reset;
        
    
    public ServiceConfiguration(GwtSession currentSession,
    						    GwtConfigComponent configComponent,
    						    ServiceTree serviceTree)
    {
    	m_serviceTree     = serviceTree;
    	m_currentSession  = currentSession;
    	m_configComponent = configComponent;
        m_dirty           = false;
    	m_initialized     = false;    	
    }
    

    public GwtConfigComponent getGwtConfigComponent()
    {
    	return m_configComponent;
    }
    

    protected void onRender(Element parent, int index) 
    {        
        super.onRender(parent, index);        
        setLayout(new FitLayout());
        setBorders(false);
        setId("services-config-panel-wrapper");
        
        // init components
        initToolBar();
        initConfigPanel();

		ContentPanel devicesConfigurationPanel = new ContentPanel();
		devicesConfigurationPanel.setBorders(false);
		devicesConfigurationPanel.setBodyBorder(false);
		devicesConfigurationPanel.setHeaderVisible(false);
		devicesConfigurationPanel.setLayout( new FitLayout());
		devicesConfigurationPanel.setScrollMode(Scroll.AUTO);
        devicesConfigurationPanel.setTopComponent(m_toolBar);
		devicesConfigurationPanel.add(m_devConfPanel);

        add(devicesConfigurationPanel);
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
        m_toolBar.setId("services-config-toolbar");
        
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
    
    

	private void initConfigPanel() 
    {    
		if (m_configComponent != null) {
			
			m_devConfPanel = new DeviceConfigPanel(m_configComponent);
			m_devConfPanel.addListener(Events.Change, new Listener<BaseEvent>() {
				public void handleEvent(BaseEvent be) {
			        m_apply.setEnabled(true);
			        m_reset.setEnabled(true);
			        m_dirty = true;
				}
			});
		}    	
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

	
    public void refresh() {

		if (m_dirty && m_initialized) {
			
			// clear the tree and disable the toolbar
	        m_apply.setEnabled(false);
	        m_reset.setEnabled(false);
		}
	}

        
    public void apply() 
    {
    	if (!m_devConfPanel.isValid()) {
    		MessageBox mb = new MessageBox();
    		mb.setIcon(MessageBox.ERROR);
    		mb.setMessage(MSGS.deviceConfigError());
    		mb.show();
    		return;
    	}
    	
        // ask for confirmation            
    	String componentName = m_devConfPanel.getConfiguration().getComponentName();
    	String message = MSGS.deviceConfigConfirmation(componentName);
    	final boolean isCloudUpdate = "CloudService".equals(componentName);
    	if (isCloudUpdate) {
    		message = MSGS.deviceCloudConfigConfirmation(componentName);
    	}    	
        MessageBox.confirm(MSGS.confirm(), 
        	message, 
            new Listener<MessageBoxEvent>() {  
                public void handleEvent(MessageBoxEvent ce) {
                    
                    // if confirmed, push the update
                    // if confirmed, delete
                    Dialog  dialog = ce.getDialog(); 
                    if (dialog.yesText.equals(ce.getButtonClicked().getText())) {
                    	
                    	// start the configuration update
                    	m_dirty = false;
                    	m_devConfPanel.mask(MSGS.applying());
                    	m_serviceTree.mask(MSGS.applying());
                		final GwtConfigComponent configComponent = m_devConfPanel.getUpdatedConfiguration();
						gwtComponentService.updateComponentConfiguration(configComponent, new AsyncCallback<Void>() {
							public void onFailure(Throwable caught) {
								m_devConfPanel.unmask();
								m_serviceTree.unmask();
								FailureHandler.handle(caught);  			                    
								m_serviceTree.refreshServicePanel();
							}            
							public void onSuccess(Void arg0) {
								Info.display(MSGS.info(), MSGS.deviceConfigApplied());
								m_serviceTree.refreshServicePanel();
								m_devConfPanel.unmask();
								m_serviceTree.unmask();
							}
						});
                    }
                }
        	});
    }

    
    public void reset() {
    	
    	// refresh the list
    	// and reselect the item
    	m_serviceTree.refreshServicePanel();
    }
}
