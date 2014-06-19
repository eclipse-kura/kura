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
package org.eclipse.kura.web.client.packages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.kura.web.client.configuration.ServiceTree;
import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.resources.Resources;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.client.widget.PackageInstallDialog;
import org.eclipse.kura.web.shared.model.GwtBundleInfo;
import org.eclipse.kura.web.shared.model.GwtDeploymentPackage;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.service.GwtPackageService;
import org.eclipse.kura.web.shared.service.GwtPackageServiceAsync;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.MessageBoxEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.TreeStore;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.HiddenField;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.toolbar.SeparatorToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.extjs.gxt.ui.client.widget.treegrid.TreeGrid;
import com.extjs.gxt.ui.client.widget.treegrid.TreeGridCellRenderer;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

public class PackagesPanel extends LayoutContainer 
{
	private static final Messages MSGS = GWT.create(Messages.class);

	private final GwtPackageServiceAsync gwtPackageService = GWT.create(GwtPackageService.class);
	
	private final static String SERVLET_URL = "/" + GWT.getModuleName() + "/file/deploy";
	
	@SuppressWarnings("unused")
	private GwtSession           m_currentSession;
    private ServiceTree          m_servicesTree;

    private boolean    		     m_dirty;
    private boolean              m_initialized;
    
    private ToolBar              m_toolBar;
    private ContentPanel         m_packagesPanel;
    private Button               m_refreshButton;
    private Button               m_installButton;
    private Button               m_uninstallButton;
    
    private TreeGrid<ModelData>  m_treeGrid;
    private TreeStore<ModelData> m_treeStore = new TreeStore<ModelData>();
    private PackageInstallDialog m_fileUpload;

    
    public PackagesPanel(GwtSession currentSession,
					     ServiceTree serviceTree) 
    {
        m_currentSession = currentSession;
    	m_servicesTree   = serviceTree;
        m_dirty          = true;
    	m_initialized    = false;
    }
    
    
    protected void onRender(Element parent, int index) {
        
        super.onRender(parent, index);        
        setLayout(new FitLayout());
        setBorders(false);
        setId("packages-panel-wrapper");
        
        // init components
        initToolBar();
        initPackages();

		ContentPanel devicesConfigurationPanel = new ContentPanel();
		devicesConfigurationPanel.setBorders(false);
		devicesConfigurationPanel.setBodyBorder(false);
		devicesConfigurationPanel.setHeaderVisible(false);
		devicesConfigurationPanel.setLayout( new FitLayout());
		//devicesConfigurationPanel.setLayout( new FlowLayout());
		devicesConfigurationPanel.setScrollMode(Scroll.AUTO);
        devicesConfigurationPanel.setTopComponent(m_toolBar);
		devicesConfigurationPanel.add(m_packagesPanel);

        add(devicesConfigurationPanel);
        m_initialized = true;
        
        refresh();
    }
    
    
    private void initToolBar() {  
    	
        m_toolBar = new ToolBar();
        m_toolBar.setBorders(true);
        m_toolBar.setId("packages-toolbar");
 
        m_refreshButton = new Button(MSGS.refreshButton(), 
        		AbstractImagePrototype.create(Resources.INSTANCE.refresh()),
                new SelectionListener<ButtonEvent>() {
            @Override
            public void componentSelected(ButtonEvent ce) {
            	m_dirty = true;
		    	// refresh the list
		    	// and reselect the item
		    	m_servicesTree.refreshServicePanel();
                refresh();
            }
        });

        m_installButton = new Button(MSGS.packageAddButton(),
        		AbstractImagePrototype.create(Resources.INSTANCE.packageAdd()),
                new SelectionListener<ButtonEvent>() {
            @Override
            public void componentSelected(ButtonEvent ce) {
                upload();
            }
        });

        m_uninstallButton = new Button(MSGS.packageDeleteButton(),
        		AbstractImagePrototype.create(Resources.INSTANCE.packageDelete()),
                new SelectionListener<ButtonEvent>() {
            @Override
            public void componentSelected(ButtonEvent ce) {
        		ModelData selectedItem = m_treeGrid.getSelectionModel().getSelectedItem();

        		if (selectedItem != null) {
        			if (selectedItem instanceof GwtDeploymentPackage) {
        				GwtDeploymentPackage pkg = (GwtDeploymentPackage) selectedItem;

        				MessageBox.confirm(MSGS.confirm(), 
        						           MSGS.deviceUninstallPackage(pkg.getName()),
        						           new Listener<MessageBoxEvent>() {  
        					public void handleEvent(MessageBoxEvent ce) {
        						// if confirmed, uninstall
        						Dialog  dialog = ce.getDialog();
        						if (dialog.yesText.equals(ce.getButtonClicked().getText())) {
        							uninstall();
        						}
        					}
        				});
        			}
        		}
            }
        });

        m_toolBar.add(m_refreshButton);
        m_toolBar.add(new SeparatorToolItem());
        m_toolBar.add(m_installButton);
        m_toolBar.add(new SeparatorToolItem());
        m_toolBar.add(m_uninstallButton);
        
        m_toolBar.disable();
    }
    
    private void upload() {
    	m_toolBar.disable();
    	    	
    	List<HiddenField<?>> hiddenFields = new ArrayList<HiddenField<?>>();
    	
    	//m_fileUpload = new FileUploadDialog(SERVLET_URL, hiddenFields);
    	m_fileUpload = new PackageInstallDialog(SERVLET_URL, hiddenFields);
    	
    	m_fileUpload.addListener(Events.Hide, new Listener<BaseEvent>() {

    		public void handleEvent(BaseEvent be) {
				m_toolBar.enable();
				m_uninstallButton.disable();
				m_dirty = true;

		    	// refresh the list
		    	// and reselect the item
		    	m_servicesTree.refreshServicePanel();
				refreshWithDelay();
    		}
    	});

    	m_fileUpload.setHeading(MSGS.deviceInstallNewPackage());
    	m_fileUpload.show();
    }
    
    private void uninstall() {
    	m_toolBar.disable();
    	
    	ModelData selectedItem = m_treeGrid.getSelectionModel().getSelectedItem();
    	
    	if (selectedItem != null) {
			if (selectedItem instanceof GwtDeploymentPackage) {
				GwtDeploymentPackage pkg = (GwtDeploymentPackage) selectedItem;
								
				gwtPackageService.uninstallDeploymentPackage(pkg.getName(), new AsyncCallback<Void>() {					
					public void onSuccess(Void arg0) {
						m_toolBar.enable();
						m_uninstallButton.disable();
						m_dirty = true;

        		    	// refresh the list
        		    	// and reselect the item
        		    	m_servicesTree.refreshServicePanel();
						refreshWithDelay();
					}
					
					public void onFailure(Throwable caught) {
						m_toolBar.enable();
						m_uninstallButton.disable();
						m_dirty = true;
						FailureHandler.handle(caught);

        		    	// refresh the list
        		    	// and reselect the item
        		    	m_servicesTree.refreshServicePanel();
						refreshWithDelay();
					}
				});	
			}
    	}
    }
    
    private void initPackages() {      
        ColumnConfig name = new ColumnConfig("name", "Name", 100);
        name.setRenderer(new TreeGridCellRenderer<ModelData>());
        ColumnConfig version = new ColumnConfig("version", "Version", 150);
        version.setSortable(false);
        ColumnModel cm = new ColumnModel(Arrays.asList(name, version));
      
        m_packagesPanel = new ContentPanel();
        m_packagesPanel.setBodyBorder(false);
        m_packagesPanel.setButtonAlign(HorizontalAlignment.CENTER);  
        m_packagesPanel.setLayout(new FitLayout());
        m_packagesPanel.setFrame(false);
        m_packagesPanel.setHeaderVisible(false);
        m_packagesPanel.setId("packages-content-wrapper");
        
        m_treeGrid = new TreeGrid<ModelData>(m_treeStore, cm);
        m_treeGrid.setBorders(true);
        m_treeGrid.getStyle().setLeafIcon(AbstractImagePrototype.create(Resources.INSTANCE.plugin()));  
        m_treeGrid.setAutoExpandColumn("name");
        m_treeGrid.setTrackMouseOver(false);  
        m_treeGrid.getAriaSupport().setLabelledBy(m_packagesPanel.getHeader().getId() + "-label");
        m_treeGrid.getView().setAutoFill(true);
        m_treeGrid.getView().setEmptyText(MSGS.deviceNoDeviceSelected());
        
        m_treeGrid.getSelectionModel().addSelectionChangedListener(new SelectionChangedListener<ModelData>() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent<ModelData> se) {
				ModelData selectedItem = se.getSelectedItem();
				
				// Check if it's a package or a bundle
				if (selectedItem instanceof GwtDeploymentPackage) {
					m_uninstallButton.enable();
				} else {
					m_uninstallButton.disable();
				}
			}
		});
        
        m_packagesPanel.add(m_treeGrid);
    }

    
    // --------------------------------------------------------------------------------------
    //
    //    Device Configuration Management
    //
    // --------------------------------------------------------------------------------------

    public void refreshWithDelay() {
    	refresh(2500);
    }
    
    public void refresh() {
    	refresh(100);
    }
    
    public void refresh(int delayMillis) {

    	if (m_dirty && m_initialized) {

    		m_dirty = false;
    		m_toolBar.enable();
    		m_uninstallButton.disable();
    		m_treeGrid.getView().setEmptyText(MSGS.devicePackagesNone());
    		m_treeStore.removeAll();
        	m_treeGrid.mask(MSGS.loading());
        	
        	Timer timer = new Timer() {
        	    public void run() {
        	       
            		gwtPackageService.findDeviceDeploymentPackages(new AsyncCallback<List<GwtDeploymentPackage>>() {					
            			public void onSuccess(List<GwtDeploymentPackage> packages) {
            				if (packages != null) {
            					for (GwtDeploymentPackage pkg : packages) {
            						m_treeStore.add(pkg, false);

            						if (pkg.getBundleInfos() != null) {
            							for (GwtBundleInfo bundle : pkg.getBundleInfos()) {
            								m_treeStore.add(pkg, bundle, false);
            							}
            						}
            					}
            				}
            		    	m_treeGrid.unmask();
            			}

            			public void onFailure(Throwable caught) {
            				FailureHandler.handle(caught);
            				m_treeGrid.unmask();
            			}
            		});
        	    } 
        	};
        	timer.schedule(delayMillis);
    	}
    }
}
