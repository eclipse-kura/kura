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
package org.eclipse.kura.web.client.status;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.resources.Resources;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.service.GwtStatusService;
import org.eclipse.kura.web.shared.service.GwtStatusServiceAsync;

import com.extjs.gxt.ui.client.data.BaseListLoader;
import com.extjs.gxt.ui.client.data.ListLoadResult;
import com.extjs.gxt.ui.client.data.RpcProxy;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.GroupingStore;
import com.extjs.gxt.ui.client.store.StoreSorter;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GroupingView;
import com.extjs.gxt.ui.client.widget.layout.FillLayout;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.toolbar.SeparatorToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

public class StatusPanel extends LayoutContainer {
	
	private static final Messages MSGS = GWT.create(Messages.class);
	
	private final GwtStatusServiceAsync gwtStatusService = GWT.create(GwtStatusService.class);
	
	@SuppressWarnings("unused")
	private GwtSession           m_currentSession;
    
    private boolean				m_initialized;
    
    private ToolBar				m_toolBar;
    private Button				m_refreshButton;
    
    private Grid<GwtGroupedNVPair> m_grid;
    private GroupingStore<GwtGroupedNVPair> m_store;
    private BaseListLoader<ListLoadResult<GwtGroupedNVPair>> m_loader;
    
    public StatusPanel(GwtSession gwtSession) {
    	m_currentSession = gwtSession;
    	m_initialized = false;
    }
    
    protected void onRender(Element parent, int index) {
    	super.onRender(parent, index);        
        setLayout(new FitLayout());
        setBorders(false);
        setId("status-panel-wrapper");
        
        initToolBar();
        initStatusPanel();
        
        ContentPanel statusWrapperPanel = new ContentPanel();
        statusWrapperPanel.setBorders(false);
        statusWrapperPanel.setBodyBorder(false);
        statusWrapperPanel.setHeaderVisible(false);
        statusWrapperPanel.setLayout(new FillLayout());
        statusWrapperPanel.setTopComponent(m_toolBar);
        statusWrapperPanel.add(m_grid);
        
        add(statusWrapperPanel);
        m_initialized = true;
        refresh();
        
    }
    
    private void initToolBar() {
    	m_toolBar = new ToolBar();
        m_toolBar.setBorders(true);
        m_toolBar.setId("status-toolbar");
 
        m_refreshButton = new Button(MSGS.refreshButton(), 
        		AbstractImagePrototype.create(Resources.INSTANCE.refresh()),
                new SelectionListener<ButtonEvent>() {
            @Override
            public void componentSelected(ButtonEvent ce) {
                refresh();
            }
        });
        
        m_toolBar.add(m_refreshButton);
        m_toolBar.add(new SeparatorToolItem());
    }
    
    private void initStatusPanel() {
    	
    	RpcProxy<ListLoadResult<GwtGroupedNVPair>> proxy = new RpcProxy<ListLoadResult<GwtGroupedNVPair>>() {
			@Override
			protected void load(Object loadConfig, final AsyncCallback<ListLoadResult<GwtGroupedNVPair>> callback) {
				mask(MSGS.loading());
				gwtStatusService.getDeviceConfig(m_currentSession.isNetAdminAvailable(), new AsyncCallback<ListLoadResult<GwtGroupedNVPair>>() {
					public void onFailure(Throwable caught) {
						unmask();
						FailureHandler.handle(caught);
					}
					public void onSuccess(ListLoadResult<GwtGroupedNVPair> pairs) {
						unmask();
						callback.onSuccess(pairs);
					}
		    	});
			}
    	};
    	
    	m_loader = new BaseListLoader<ListLoadResult<GwtGroupedNVPair>>(proxy);
    	
    	m_store = new GroupingStore<GwtGroupedNVPair>(m_loader);
    	m_store.groupBy("groupLoc");
    	m_store.setStoreSorter(new StoreSorter<GwtGroupedNVPair>(new Comparator<Object>() {
			public int compare(Object o1, Object o2) {
				if (o1 == null) o1 = new Integer(-1);
				else o1 = getIntFromString((String)o1);
				
				if (o2 == null) o2 = new Integer(-1);
				else o2 = getIntFromString((String)o2);
				
				return (Integer)o1 - (Integer)o2;
			}
			
			private Integer getIntFromString(String value) {
				if ("Cloud and Data Service".equals(value)) return new Integer(0);
				else if ("Ethernet Settings".equals(value)) return new Integer(1);
				else if ("Wireless Settings".equals(value)) return new Integer(2);
				else if ("Cellular Settings".equals(value)) return new Integer(3);
				else if ("Position Status".equals(value)) return new Integer(4);
				else return new Integer(100);
			}
    		
    	}));
    	
    	ColumnConfig name  = new ColumnConfig("name", MSGS.devicePropName(), 50);
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
    	m_grid.setHideHeaders(true);
    }
    
    private void refresh() {
    	if (m_initialized) {
    		m_loader.load();
    	}
    }

}
