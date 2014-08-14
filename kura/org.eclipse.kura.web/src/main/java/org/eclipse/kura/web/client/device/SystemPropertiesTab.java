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
package org.eclipse.kura.web.client.device;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.service.GwtDeviceService;
import org.eclipse.kura.web.shared.service.GwtDeviceServiceAsync;

import com.extjs.gxt.ui.client.data.BaseListLoader;
import com.extjs.gxt.ui.client.data.ListLoadResult;
import com.extjs.gxt.ui.client.data.RpcProxy;
import com.extjs.gxt.ui.client.store.GroupingStore;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GroupingView;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;


public class SystemPropertiesTab extends LayoutContainer 
{
	private static final Messages MSGS = GWT.create(Messages.class);
	private final GwtDeviceServiceAsync gwtDeviceService = GWT.create(GwtDeviceService.class);

	@SuppressWarnings("unused")
    private GwtSession      m_currentSession;

    private boolean         m_initialized;
	
	private Grid<GwtGroupedNVPair> m_grid;
	private GroupingStore<GwtGroupedNVPair> m_store;	
	private BaseListLoader<ListLoadResult<GwtGroupedNVPair>> m_loader;
	
    
    public SystemPropertiesTab(GwtSession currentSession) {
        m_currentSession = currentSession;
    	m_initialized    = false;
    }
    
    
    protected void onRender(Element parent, int index) 
    {        
    	super.onRender(parent, index);         
        setLayout(new FitLayout());
        setId("device-system-properties");

        RpcProxy<ListLoadResult<GwtGroupedNVPair>> proxy = new RpcProxy<ListLoadResult<GwtGroupedNVPair>>() {  
            @Override  
            protected void load(Object loadConfig, final AsyncCallback<ListLoadResult<GwtGroupedNVPair>> callback) {
    			gwtDeviceService.findSystemProperties( new AsyncCallback<ListLoadResult<GwtGroupedNVPair>>() {    				
    				public void onSuccess(ListLoadResult<GwtGroupedNVPair> pairs) {
    					callback.onSuccess(pairs);
    				}    				
    				public void onFailure(Throwable caught) {
    					FailureHandler.handle(caught);
    				}
    			});
            }
        };

        m_loader = new BaseListLoader<ListLoadResult<GwtGroupedNVPair>>(proxy);

        m_store = new GroupingStore<GwtGroupedNVPair>(m_loader);  
        m_store.groupBy("groupLoc");  

        ColumnConfig name  = new ColumnConfig("name",  MSGS.devicePropName(), 50);  
        ColumnConfig value = new ColumnConfig("value", MSGS.devicePropValue(), 50);  

        List<ColumnConfig> config = new ArrayList<ColumnConfig>();  
        config.add(name);  
        config.add(value);  

        ColumnModel cm = new ColumnModel(config);    
        GroupingView view = new GroupingView();  
        view.setShowGroupedColumn(false);  
        view.setForceFit(true);  
      
        m_grid = new Grid<GwtGroupedNVPair>(m_store, cm);  
        m_grid.setView(view);  
        m_grid.setBorders(false);
        m_grid.setLoadMask(true);
        m_grid.setStripeRows(true);
        
        add(m_grid);
        m_initialized = true;
    }
    
    
	public void refresh() 
	{
		if (m_initialized) {
			m_loader.load();
		}
	}
}
