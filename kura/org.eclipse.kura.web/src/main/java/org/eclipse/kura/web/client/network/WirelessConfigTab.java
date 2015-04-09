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
package org.eclipse.kura.web.client.network;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.resources.Resources;
import org.eclipse.kura.web.client.util.Constants;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.client.util.FormUtils;
import org.eclipse.kura.web.client.util.MessageUtils;
import org.eclipse.kura.web.client.util.SwappableListStore;
import org.eclipse.kura.web.client.util.TextFieldWithButton;
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtNetIfStatus;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtWifiBgscanModule;
import org.eclipse.kura.web.shared.model.GwtWifiChannelModel;
import org.eclipse.kura.web.shared.model.GwtWifiCiphers;
import org.eclipse.kura.web.shared.model.GwtWifiConfig;
import org.eclipse.kura.web.shared.model.GwtWifiHotspotEntry;
import org.eclipse.kura.web.shared.model.GwtWifiNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtWifiRadioMode;
import org.eclipse.kura.web.shared.model.GwtWifiSecurity;
import org.eclipse.kura.web.shared.model.GwtWifiWirelessMode;
import org.eclipse.kura.web.shared.model.GwtWifiWirelessModeModel;
import org.eclipse.kura.web.shared.service.GwtDeviceService;
import org.eclipse.kura.web.shared.service.GwtDeviceServiceAsync;
import org.eclipse.kura.web.shared.service.GwtNetworkService;
import org.eclipse.kura.web.shared.service.GwtNetworkServiceAsync;

import com.allen_sauer.gwt.log.client.Log;
import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.Style.SelectionMode;
import com.extjs.gxt.ui.client.Style.SortDir;
import com.extjs.gxt.ui.client.core.El;
import com.extjs.gxt.ui.client.data.BaseListLoader;
import com.extjs.gxt.ui.client.data.ListLoadResult;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.data.ModelKeyProvider;
import com.extjs.gxt.ui.client.data.RpcProxy;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.GridEvent;
import com.extjs.gxt.ui.client.event.KeyListener;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.LoadListener;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.GroupingStore;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.ComponentPlugin;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.Slider;
import com.extjs.gxt.ui.client.widget.VerticalPanel;
import com.extjs.gxt.ui.client.widget.Window;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.ComboBox;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.form.Field;
import com.extjs.gxt.ui.client.widget.form.FieldSet;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.NumberField;
import com.extjs.gxt.ui.client.widget.form.Radio;
import com.extjs.gxt.ui.client.widget.form.RadioGroup;
import com.extjs.gxt.ui.client.widget.form.SimpleComboBox;
import com.extjs.gxt.ui.client.widget.form.SimpleComboValue;
import com.extjs.gxt.ui.client.widget.form.SliderField;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.form.Validator;
import com.extjs.gxt.ui.client.widget.grid.CheckBoxSelectionModel;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridGroupRenderer;
import com.extjs.gxt.ui.client.widget.grid.GridSelectionModel;
import com.extjs.gxt.ui.client.widget.grid.GroupColumnData;
import com.extjs.gxt.ui.client.widget.grid.GroupingView;
import com.extjs.gxt.ui.client.widget.layout.FillLayout;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.FlowLayout;
import com.extjs.gxt.ui.client.widget.layout.FormData;
import com.extjs.gxt.ui.client.widget.layout.FormLayout;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
 
public class WirelessConfigTab extends LayoutContainer 
{
	private static final Messages MSGS = GWT.create(Messages.class);
	
	private final GwtNetworkServiceAsync gwtNetworkService = GWT.create(GwtNetworkService.class);
	private final GwtDeviceServiceAsync gwtDeviceService = GWT.create(GwtDeviceService.class);

	private static final String PASSWORD_REGEX_ANY  = ".*";
	private static final String PASSWORD_REGEX_WPA  = "^[ -~]{8,63}$"; // Match all ASCII printable characters
	//private static final String PASSWORD_REGEX_WEP  = "^(?:\\w{5}|\\w{13}|\\w{16}|[a-fA-F0-9]{10}|[a-fA-F0-9]{26}|[a-fA-F0-9]{32})$";
	private static final String PASSWORD_REGEX_WEP  = "^(?:\\w{5}|\\w{13}|[a-fA-F0-9]{10}|[a-fA-F0-9]{26})$";
	private static final int	MAX_WIFI_CHANNEL	= 13;
	
	//private final TextArea toolTipField = new TextArea();
	private final ToolTipBox toolTipField = new ToolTipBox("360px");
	private final String defaultToolTip = "Mouse over enabled items on the left to see help text.";
	
    private GwtSession            m_currentSession;

    private boolean                    m_dirty;
    private boolean                    m_initialized;
	private GwtWifiNetInterfaceConfig  m_selectNetIfConfig;
	private NetInterfaceConfigTabs	   m_netInterfaceConfigTabs;
	private TcpIpConfigTab             m_tcpIpConfigTab;
	private GwtNetIfStatus             m_tcpIpStatus;
	private GwtWifiConfig			   m_activeWifiConfig;
	
	private FormPanel               m_formPanel;
	private ListStore<GwtWifiWirelessModeModel> m_wirelessModeListStore;
	private ComboBox<GwtWifiWirelessModeModel>  m_modeCombo;
	private TextFieldWithButton<String> 	m_ssidField;
	//private Button 					m_ssidPickerButton;
	private SimpleComboBox<String>  m_radioModeCombo;
	
	private SimpleComboBox<String>  m_securityCombo;
	private TextFieldWithButton<String>       m_passwordField;
	private TextField<String>       m_verifyPasswordField;
	
	private SimpleComboBox<String>  m_pairwiseCiphersCombo;
	private SimpleComboBox<String>  m_groupCiphersCombo;
	
	private SimpleComboBox<String>  m_bgscanModuleCombo;
	private NumberField				m_bgscanShortIntervalField;
	private NumberField				m_bgscanLongIntervalField;
	private Slider 					m_bgscanRssiThresholdSlider;
	private SliderField 			m_bgscanRssiThresholdSliderField;
	
	private Radio 				   	m_pingAccessPointRadioTrue;
	private Radio 				   	m_pingAccessPointRadioFalse;
	private RadioGroup 			   	m_pingAccessPointRadioGroup;
	
	private Radio 				   	m_ignoreSsidRadioTrue;
	private Radio 				   	m_ignoreSsidRadioFalse;
	private RadioGroup 			   	m_ignoreSsidRadioGroup;
	
	private ContentPanel			m_channelPanel;
	
    private ComponentPlugin         m_dirtyPlugin;
    
    private GroupingView m_channelGroupingView;
    private String m_checkedStyle = "x-grid3-group-check";
    private String m_uncheckedStyle = "x-grid3-group-uncheck";
    
    private BaseListLoader<ListLoadResult<GwtWifiHotspotEntry>> m_wifiHotspotLoader;
    private Grid<GwtWifiHotspotEntry> m_grid;
    private Window m_wifiNetworksWindow;
    
    private class MouseOverListener implements Listener<BaseEvent> {

    	private String  html;
    	
    	public MouseOverListener(String html) {
    		this.html = html;
    	}
		public void handleEvent(BaseEvent be) {
			toolTipField.setText(html);
		}
    	
    }
    
	private CheckBoxSelectionModel<GwtWifiChannelModel> m_checkboxChannelSelectionModel = new CheckBoxSelectionModel<GwtWifiChannelModel>() {
		@Override
		public void deselectAll() {
			super.deselectAll();
			NodeList<com.google.gwt.dom.client.Element> groups = m_channelGroupingView.getGroups();
			for (int i = 0; i < groups.getLength(); i++) {
				com.google.gwt.dom.client.Element group = groups.getItem(i).getFirstChildElement();
				setGroupChecked((Element) group, false);
			}
		}

		@Override
		public void selectAll() {
			super.selectAll();
			NodeList<com.google.gwt.dom.client.Element> groups = m_channelGroupingView.getGroups();
			for (int i = 0; i < groups.getLength(); i++) {
				com.google.gwt.dom.client.Element group = groups.getItem(i).getFirstChildElement();
				setGroupChecked((Element) group, true);
			}
		}

		@Override
		protected void doDeselect(List<GwtWifiChannelModel> models, boolean supressEvent) {
			super.doDeselect(models, supressEvent);
			NodeList<com.google.gwt.dom.client.Element> groups = m_channelGroupingView.getGroups();
			search: for (int i = 0; i < groups.getLength(); i++) {
				com.google.gwt.dom.client.Element group = groups.getItem(i);
				NodeList<Element> rows = El.fly(group).select(".x-grid3-row");
				for (int j = 0, len = rows.getLength(); j < len; j++) {
					Element r = rows.getItem(j);
					int idx = grid.getView().findRowIndex(r);
					GwtWifiChannelModel m = grid.getStore().getAt(idx);
					if (!isSelected(m)) {
						setGroupChecked((Element) group, false);
						continue search;
					}
				}
			}

		}

		@Override
		protected void doSelect(List<GwtWifiChannelModel> models, boolean keepExisting,
				boolean supressEvent) {
			super.doSelect(models, keepExisting, supressEvent);
			NodeList<com.google.gwt.dom.client.Element> groups = m_channelGroupingView.getGroups();
			search: for (int i = 0; i < groups.getLength(); i++) {
				com.google.gwt.dom.client.Element group = groups.getItem(i);
				NodeList<Element> rows = El.fly(group).select(".x-grid3-row");
				for (int j = 0, len = rows.getLength(); j < len; j++) {
					Element r = rows.getItem(j);
					int idx = grid.getView().findRowIndex(r);
					GwtWifiChannelModel m = grid.getStore().getAt(idx);
					if (!isSelected(m)) {
						continue search;
					}
				}
				setGroupChecked((Element) group, true);

			}
		}
	}; 
    
    public WirelessConfigTab(GwtSession currentSession,
    					     TcpIpConfigTab tcpIpConfigTab,
    					     NetInterfaceConfigTabs netInterfaceConfigTabs)
    {
    	// initialization 
        m_currentSession = currentSession;
        m_tcpIpConfigTab = tcpIpConfigTab;
        m_netInterfaceConfigTabs = netInterfaceConfigTabs;
    	m_dirty          = true;
    	m_initialized    = false;
    
	    final WirelessConfigTab theTab = this;
    	m_dirtyPlugin = new ComponentPlugin() {  
    		public void init(Component component) {  
    			component.addListener(Events.Change, new Listener<ComponentEvent>() {  
    				public void handleEvent(ComponentEvent be) {
    					FormUtils.addDirtyFieldIcon(be.getComponent());    					
    					theTab.fireEvent(Events.Change);
    				}  
    			});  
  	      	}  
  	    };
  	    
  	    m_tcpIpConfigTab.addListener(Events.Change, new Listener<BaseEvent>() {
  	    	public void handleEvent(BaseEvent be) {
  	    	    if(m_selectNetIfConfig != null) {
      	    	    // set default values for wireless mode if tcp/ip status was changed
      	    	    GwtNetIfStatus tcpIpStatus = m_tcpIpConfigTab.getStatus();
      	    	    if(!tcpIpStatus.equals(m_tcpIpStatus)) {
      	    	        if(GwtNetIfStatus.netIPv4StatusEnabledLAN.equals(tcpIpStatus)) {
      	    	            m_activeWifiConfig = m_selectNetIfConfig.getAccessPointWifiConfig();
                        } else {
                            m_activeWifiConfig = m_selectNetIfConfig.getStationWifiConfig();
                        }
      	    	        m_tcpIpStatus = tcpIpStatus;
      	    	        m_netInterfaceConfigTabs.adjustInterfaceTabs();
      	    	    }
  	    	    }
  	    	    refreshForm();
  	    	    theTab.fireEvent(Events.Change);
  	    	}
  	    });
    }

    public void setNetInterface(GwtNetInterfaceConfig netIfConfig)
    {
    	m_dirty = true;
        if(m_tcpIpStatus == null || m_selectNetIfConfig != netIfConfig) {
            m_tcpIpStatus = m_tcpIpConfigTab.getStatus();
        }
		if(netIfConfig instanceof GwtWifiNetInterfaceConfig) {
	    	m_selectNetIfConfig = (GwtWifiNetInterfaceConfig) netIfConfig;
	    	m_activeWifiConfig = m_selectNetIfConfig.getActiveWifiConfig();
		}
    }
    
    
    public void getUpdatedNetInterface(GwtNetInterfaceConfig updatedNetIf)
    {
        GwtWifiNetInterfaceConfig updatedWifiNetIf = (GwtWifiNetInterfaceConfig) updatedNetIf;

        if (m_formPanel != null) {
        	GwtWifiConfig updatedWifiConfig = getGwtWifiConfig();
        	updatedWifiNetIf.setWirelessMode(updatedWifiConfig.getWirelessMode());
        	
    		// Update the wifi config
    		updatedWifiNetIf.setWifiConfig(updatedWifiConfig);
    	} else {
    	    if(m_selectNetIfConfig != null) {
        	    Log.debug("Wireless config tab not yet rendered, using original values");
                
        	    updatedWifiNetIf.setAccessPointWifiConfig(m_selectNetIfConfig.getAccessPointWifiConfigProps());
        	    /* updatedWifiNetIf.setAdhocWifiConfig(m_selectNetIfConfig.getAdhocWifiConfigProps()); */
        	    updatedWifiNetIf.setStationWifiConfig(m_selectNetIfConfig.getStationWifiConfigProps());
    
        	    // Select the correct mode
                for (GwtWifiWirelessMode mode : GwtWifiWirelessMode.values()) {
                    if (mode.name().equals(m_selectNetIfConfig.getWirelessMode())) {
                        updatedWifiNetIf.setWirelessMode(mode.name());
                        break;
                    }
                }
    	    }
    	}
    }
        
    public boolean isValid() {
    	if (m_formPanel != null) {
    		for (Field<?> field : m_formPanel.getFields()) {
    			if (!field.isValid()) {
    				return false;
    			}
    		}
    	}
    	return true;
    }
    
    public boolean isDirty() {
    	if (m_formPanel == null) {
    		return false;
    	}
        List<Field<?>> fields = m_formPanel.getFields();
        for (int i=0; i<fields.size(); i++) {
        	if (fields.get(i) instanceof Field) {
	            Field<?> field = (Field<?>) fields.get(i);
	            if (field.isDirty()) {
	            	if((field.getOriginalValue() != null && field.getOriginalValue().equals("") && field.getValue() == null) || 
	            			(field.getValue() != null && field.getValue().equals("") && field.getOriginalValue() == null)) {
	            		continue;
	            	} else if(field.getOriginalValue() != null && field.getValue() != null && field.getOriginalValue().equals(field.getValue())) {
	            		continue;
	            	} else {
	            		if(field.getOriginalValue() instanceof SimpleComboValue) {
	            			Log.debug("field " + field.getName() + " is dirty - original value: " + ((SimpleComboValue)field.getOriginalValue()).getValue() + " with type: " + field.getOriginalValue().getClass().toString());
	            			if(field.getValue() != null) {
	            				Log.debug("\tnew value: " + ((SimpleComboValue)field.getValue()).getValue() +  " with type: " + field.getValue().getClass().toString());
	            			}
	            		} else {
		            		Log.debug("field " + field.getName() + " is dirty - original value: " + field.getOriginalValue() + " with type: " + field.getOriginalValue().getClass().toString());
		            		if(field.getValue() != null) {
		            			Log.debug("\tnew value: " + field.getValue() +  " with type: " + field.getValue().getClass().toString());
		            		}
	            		}
	            		return true;
	            	}
	            	
	            	/*
                    if(field != m_bgscanRssiThresholdSliderField)       // FIXME: m_bgscanRssiThresholdSliderField.isDirty() is always returning true
                        return true;
                        */
	            } else {
	            	Log.debug("NOT DIRTY: " + field.getName() + " value: " + field.getValue());
	            }
        	}
        }
        return false;
    }
    
    protected void onRender(Element parent, int index) 
    {   
        super.onRender(parent, index);    
    	
        setLayout(new FitLayout());
        setId("network-wireless");
        
        FormData formData = new FormData();
        formData.setWidth(250);
  
        m_formPanel = new FormPanel();
        m_formPanel.setFrame(false);
        m_formPanel.setBodyBorder(false);
        m_formPanel.setHeaderVisible(false);
        m_formPanel.setLayout(new FlowLayout());
        m_formPanel.setStyleAttribute("min-width", "775px");
        m_formPanel.setStyleAttribute("padding-left", "30px");
        
        FieldSet fieldSet = new FieldSet();
        FormLayout layoutAccount = new FormLayout();
        layoutAccount.setLabelWidth(Constants.LABEL_WIDTH_FORM);
        fieldSet.setLayout(layoutAccount);
        fieldSet.setBorders(false);
        
        //
        // Tool Tip Box
        //
        toolTipField.setText(defaultToolTip);
        fieldSet.add(toolTipField);

        //
    	// Wireless Mode
        //
        m_wirelessModeListStore = new ListStore<GwtWifiWirelessModeModel>();
        /*
        m_wirelessModeListStore.add(new GwtWifiWirelessModeModel(GwtWifiWirelessMode.netWifiWirelessModeDisabled,
                MessageUtils.get("netWifiWirelessModeDisabled"),
                MSGS.netWifiToolTipWirelessModeDisabled()));
        */
        m_wirelessModeListStore.add(new GwtWifiWirelessModeModel(GwtWifiWirelessMode.netWifiWirelessModeStation,
                MessageUtils.get("netWifiWirelessModeStation"),
                MSGS.netWifiToolTipWirelessModeStation()));
        m_wirelessModeListStore.add(new GwtWifiWirelessModeModel(GwtWifiWirelessMode.netWifiWirelessModeAccessPoint,
                MessageUtils.get("netWifiWirelessModeAccessPoint"),
                MSGS.netWifiToolTipWirelessModeAccessPoint()));
        /*
        m_wirelessModeListStore.add(new GwtWifiWirelessModeModel(GwtWifiWirelessMode.netWifiWirelessModeAdHoc,
                MessageUtils.get("netWifiWirelessModeAdHoc"),
                MSGS.netWifiToolTipWirelessModeAdhoc()));
        */
        
        m_modeCombo = new ComboBox<GwtWifiWirelessModeModel>();
        m_modeCombo.setName("comboMode");
        m_modeCombo.setDisplayField(GwtWifiWirelessModeModel.NAME);
        m_modeCombo.setFieldLabel(MSGS.netWifiWirelessMode());
        m_modeCombo.setEditable(false);
        m_modeCombo.setTypeAhead(true);
        m_modeCombo.setTriggerAction(TriggerAction.ALL);
        m_modeCombo.addStyleName("kura-combobox");
        m_modeCombo.setStore(m_wirelessModeListStore);
        m_modeCombo.setTemplate(getTemplate());
        m_modeCombo.setValue(m_wirelessModeListStore.findModel(GwtWifiWirelessModeModel.MODE, GwtWifiWirelessMode.netWifiWirelessModeStation.name()));
		m_modeCombo.setValidator(new Validator() {
			
			public String validate(Field<?> field, String value) {
				if (m_tcpIpConfigTab.getStatus().equals(GwtNetIfStatus.netIPv4StatusEnabledWAN)
						&& value.equals(MessageUtils.get(GwtWifiWirelessMode.netWifiWirelessModeAccessPoint.name()))){
					return MSGS.netWifiWirelessEnabledForWANError();
				}
				
				return null;
			}
		});

        m_modeCombo.addSelectionChangedListener( new SelectionChangedListener<GwtWifiWirelessModeModel>() {         
			@Override
			public void selectionChanged(SelectionChangedEvent<GwtWifiWirelessModeModel> se) {

				// Station mode selected
			    if (GwtWifiWirelessMode.netWifiWirelessModeStation.equals(se.getSelectedItem().getMode())) {
					// Use values from station config
					m_activeWifiConfig = m_selectNetIfConfig.getStationWifiConfig();
					
			    } else {
					// Use values from access point config
					m_activeWifiConfig = m_selectNetIfConfig.getAccessPointWifiConfig();
			    }
				setPasswordValidation();
				setValues(false);
				refreshForm();
			}
		});
        m_modeCombo.addListener(Events.OnMouseOver, new MouseOverListener(MSGS.netWifiToolTipWirelessMode()));
        m_modeCombo.addPlugin(m_dirtyPlugin);
       
        
        Button ssidPickerButton = new Button("",
        		AbstractImagePrototype.create(Resources.INSTANCE.magnifier16()),
        		new SelectionListener<ButtonEvent>() {
					@Override
					public void componentSelected(ButtonEvent ce) {
						m_wifiNetworksWindow = createWifiNetworksWindow();
						m_wifiNetworksWindow.show();
						m_wifiHotspotLoader.load();
					}
        });
        
        //
        // SSID
        // 
        m_ssidField = new TextFieldWithButton<String>(ssidPickerButton, 10);
        m_ssidField.setAllowBlank(true);
        m_ssidField.setId("ssid-input");
        m_ssidField.setName("ssid");
    	m_ssidField.setFieldLabel(MSGS.netWifiNetworkName());
    	m_ssidField.setEnabled(true, false);
    	m_ssidField.addPlugin(m_dirtyPlugin);
    	m_ssidField.addListener(Events.OnMouseOver, new MouseOverListener(MSGS.netWifiToolTipNetworkName()));
    	m_ssidField.setStyleAttribute("margin-top", Constants.LABEL_MARGIN_TOP_SEPARATOR);
       
        //
        // Radio Mode
        // 
        m_radioModeCombo = new SimpleComboBox<String>();
        m_radioModeCombo.setName("radioMode");
        m_radioModeCombo.setFieldLabel(MSGS.netWifiRadioMode());
        m_radioModeCombo.setEditable(false);
        m_radioModeCombo.setTypeAhead(true);
        m_radioModeCombo.setTriggerAction(TriggerAction.ALL);
        for (GwtWifiRadioMode mode : GwtWifiRadioMode.values()) {
        	if (mode != GwtWifiRadioMode.netWifiRadioModeA) { // we don't support 802.11a yet
        		m_radioModeCombo.add(MessageUtils.get(mode.name()));
        	}
        }
        m_radioModeCombo.setSimpleValue(MessageUtils.get(GwtWifiRadioMode.netWifiRadioModeBGN.name()));
        m_radioModeCombo.addSelectionChangedListener( new SelectionChangedListener<SimpleComboValue<String>>() {			
			@Override
			public void selectionChanged(SelectionChangedEvent<SimpleComboValue<String>> se) {
				refreshForm();
			}
		});
        m_radioModeCombo.addListener(Events.OnMouseOver, new MouseOverListener(MSGS.netWifiToolTipRadioMode()));
        m_radioModeCombo.addStyleName("kura-combobox");
        m_radioModeCombo.addPlugin(m_dirtyPlugin);
       
        //
        // Wireless Security
        // 
        m_securityCombo = new SimpleComboBox<String>();
        m_securityCombo.setName("security");
        m_securityCombo.setFieldLabel(MSGS.netWifiWirelessSecurity());
        m_securityCombo.setEditable(false);
        m_securityCombo.setTypeAhead(true);
        m_securityCombo.setTriggerAction(TriggerAction.ALL);
        m_securityCombo.setStyleAttribute("margin-top", Constants.LABEL_MARGIN_TOP_SEPARATOR);
        for (GwtWifiSecurity mode : GwtWifiSecurity.values()) {
        	m_securityCombo.add(MessageUtils.get(mode.name()));
        }
        m_securityCombo.setSimpleValue(MessageUtils.get(GwtWifiSecurity.netWifiSecurityWPA2.name()));
        m_securityCombo.addSelectionChangedListener( new SelectionChangedListener<SimpleComboValue<String>>() {			
			@Override
			public void selectionChanged(SelectionChangedEvent<SimpleComboValue<String>> se) {
				setPasswordValidation();
				refreshForm();
			}
		});
        m_securityCombo.addListener(Events.OnMouseOver, new MouseOverListener(MSGS.netWifiToolTipSecurity()));
        m_securityCombo.addStyleName("kura-combobox");
        m_securityCombo.addPlugin(m_dirtyPlugin);

        Button passwordButton = new Button("",
        		AbstractImagePrototype.create(Resources.INSTANCE.connect16()),
        		new SelectionListener<ButtonEvent>() {
					@Override
					public void componentSelected(ButtonEvent ce) {
					
						m_passwordField.setIcon(AbstractImagePrototype.create(Resources.INSTANCE.hourglass16()));
						m_passwordField.setEnabled(true, false);
						GwtWifiConfig gwtWifiConfig = getGwtWifiConfig();
						gwtNetworkService.verifyWifiCredentials(m_selectNetIfConfig.getName(), gwtWifiConfig, new AsyncCallback<Boolean>() {
							public void onSuccess(Boolean result) {
							    Log.warn("verifyWifiCredentials() :: result=" + result);
							    if (!result.booleanValue()) {
							    	m_passwordField.setValue("");
							    }
							    
							    m_passwordField.setIcon(AbstractImagePrototype.create(Resources.INSTANCE.connect16()));
								m_passwordField.setEnabled(true, true);
							}
							public void onFailure(Throwable caught) {
								Log.warn("verifyWifiCredentials() :: caught: " + caught.toString());
								m_passwordField.setIcon(AbstractImagePrototype.create(Resources.INSTANCE.connect16()));
								m_passwordField.setEnabled(true, true);
							}
						});
					}
        });
             
        //
        // Password
        // 
        m_passwordField = new TextFieldWithButton<String>(passwordButton, 10);
        m_passwordField.setId("wifi-password");
        m_passwordField.setName("password");
        m_passwordField.setFieldLabel(MSGS.netWifiWirelessPassword());
        m_passwordField.setPassword(true);
        m_passwordField.setRegex(PASSWORD_REGEX_ANY);
        m_passwordField.getMessages().setRegexText(MSGS.netWifiWirelessInvalidWPAPassword());
        m_passwordField.addPlugin(m_dirtyPlugin);
        m_passwordField.setAutoValidate(true);
        m_passwordField.setAllowBlank(false);
        m_passwordField.addKeyListener(new KeyListener() {
            @Override
            public void componentKeyUp(ComponentEvent event) {
                super.componentKeyUp(event);
                m_verifyPasswordField.validate();
            }
        });
        
        m_passwordField.addListener(Events.OnMouseOver, new MouseOverListener(MSGS.netWifiToolTipPassword()));
        
        m_passwordField.addListener(Events.OnChange, new Listener<BaseEvent>() {
  	    	public void handleEvent(BaseEvent be) {
  	    		setPasswordValidation();
  	    	    refreshForm();
  	    	}
  	    });
        
        //
        // Verify Password
        // 
        m_verifyPasswordField = new TextField<String>();
        m_verifyPasswordField.setName("verifyPassword");
        m_verifyPasswordField.setFieldLabel(MSGS.netWifiWirelessVerifyPassword());
        m_verifyPasswordField.setPassword(true);
        m_verifyPasswordField.addPlugin(m_dirtyPlugin);
        m_verifyPasswordField.getMessages().setInvalidText("Invalid text 123");  //TODO:
        m_verifyPasswordField.setAutoValidate(true);
        m_verifyPasswordField.setAllowBlank(false);
        m_verifyPasswordField.setValidator( new Validator() {
            public String validate(Field<?> field, String value) {
                if (m_modeCombo != null && GwtWifiWirelessMode.netWifiWirelessModeAccessPoint.equals(m_modeCombo.getValue().getMode())) {
                    // Check that the verify password field matches
                    if(m_passwordField == null || !value.equals(m_passwordField.getValue())) {
                        return MSGS.netWifiWirelessPasswordDoesNotMatch();
                    }
                }
                return null;
            }
        });
        m_verifyPasswordField.addListener(Events.OnMouseOver, new MouseOverListener(MSGS.netWifiToolTipPassword()));
        m_verifyPasswordField.addStyleName("kura-textfield");
                
        // pairwise ciphers
        m_pairwiseCiphersCombo = new SimpleComboBox<String>();
        m_pairwiseCiphersCombo.setName("pairwiseCiphers");
        m_pairwiseCiphersCombo.setFieldLabel(MSGS.netWifiWirelessPairwiseCiphers());
        m_pairwiseCiphersCombo.setEditable(false);
        m_pairwiseCiphersCombo.setTypeAhead(true);
        m_pairwiseCiphersCombo.setTriggerAction(TriggerAction.ALL);
        //m_pairwiseCiphersCombo.setStyleAttribute("margin-top", Constants.LABEL_MARGIN_TOP_SEPARATOR);
        for (GwtWifiCiphers ciphers : GwtWifiCiphers.values()) {
        	m_pairwiseCiphersCombo.add(MessageUtils.get(ciphers.name()));
        }
        m_pairwiseCiphersCombo.setSimpleValue(MessageUtils.get(GwtWifiCiphers.netWifiCiphers_CCMP_TKIP.name()));
		m_pairwiseCiphersCombo.addSelectionChangedListener(new SelectionChangedListener<SimpleComboValue<String>>() {
					@Override
					public void selectionChanged(
							SelectionChangedEvent<SimpleComboValue<String>> se) {
						refreshForm();
					}
				});
		m_pairwiseCiphersCombo.addListener(Events.OnMouseOver, new MouseOverListener(MSGS.netWifiToolTipCiphers()));
		m_pairwiseCiphersCombo.addStyleName("kura-combobox");
		m_pairwiseCiphersCombo.addPlugin(m_dirtyPlugin);
        
        // group ciphers
        m_groupCiphersCombo = new SimpleComboBox<String>();
        m_groupCiphersCombo.setName("groupCiphers");
        m_groupCiphersCombo.setFieldLabel(MSGS.netWifiWirelessGroupCiphers());
        m_groupCiphersCombo.setEditable(false);
        m_groupCiphersCombo.setTypeAhead(true);
        m_groupCiphersCombo.setTriggerAction(TriggerAction.ALL);
        //m_groupCiphersCombo.setStyleAttribute("margin-top", Constants.LABEL_MARGIN_TOP_SEPARATOR);
        for (GwtWifiCiphers ciphers : GwtWifiCiphers.values()) {
        	m_groupCiphersCombo.add(MessageUtils.get(ciphers.name()));
        }
        m_groupCiphersCombo.setSimpleValue(MessageUtils.get(GwtWifiCiphers.netWifiCiphers_CCMP_TKIP.name()));
        m_groupCiphersCombo.addSelectionChangedListener(new SelectionChangedListener<SimpleComboValue<String>>() {
			@Override
			public void selectionChanged(
					SelectionChangedEvent<SimpleComboValue<String>> se) {
				refreshForm();
			}
		});
        m_groupCiphersCombo.addListener(Events.OnMouseOver, new MouseOverListener(MSGS.netWifiToolTipCiphers()));
        m_groupCiphersCombo.addStyleName("kura-combobox");
        m_groupCiphersCombo.addPlugin(m_dirtyPlugin);
        
        // bgscan module
        m_bgscanModuleCombo = new SimpleComboBox<String>();
        m_bgscanModuleCombo.setName("groupCiphers");
        m_bgscanModuleCombo.setFieldLabel(MSGS.netWifiWirelessBgscanModule());
        m_bgscanModuleCombo.setEditable(false);
        m_bgscanModuleCombo.setTypeAhead(true);
        m_bgscanModuleCombo.setTriggerAction(TriggerAction.ALL);
        m_bgscanModuleCombo.setStyleAttribute("margin-top", Constants.LABEL_MARGIN_TOP_SEPARATOR);
        for (GwtWifiBgscanModule module : GwtWifiBgscanModule.values()) {
        	m_bgscanModuleCombo.add(MessageUtils.get(module.name()));
        }
        m_bgscanModuleCombo.setSimpleValue(MessageUtils.get(GwtWifiBgscanModule.netWifiBgscanMode_NONE.name()));
        m_bgscanModuleCombo.addSelectionChangedListener(new SelectionChangedListener<SimpleComboValue<String>>() {
			@Override
			public void selectionChanged(
					SelectionChangedEvent<SimpleComboValue<String>> se) {
				refreshForm();
			}
		});
        m_bgscanModuleCombo.addListener(Events.OnMouseOver, new MouseOverListener(MSGS.netWifiToolTipBgScan()));
        m_bgscanModuleCombo.addStyleName("kura-combobox");
        m_bgscanModuleCombo.addPlugin(m_dirtyPlugin);
        
        // bgscan RSSI Threshold
        m_bgscanRssiThresholdSlider = new Slider();  
        
        m_bgscanRssiThresholdSlider.setWidth(200);  
        m_bgscanRssiThresholdSlider.setIncrement(1);
        m_bgscanRssiThresholdSlider.setMinValue(-90);
        m_bgscanRssiThresholdSlider.setMaxValue(-20);  
        m_bgscanRssiThresholdSlider.addListener(Events.OnMouseOver, new MouseOverListener(MSGS.netWifiToolTipBgScanStrength()));
        m_bgscanRssiThresholdSlider.setClickToChange(false);  
        m_bgscanRssiThresholdSliderField = new SliderField(m_bgscanRssiThresholdSlider);
        m_bgscanRssiThresholdSliderField.setFieldLabel(MSGS.netWifiWirelessBgscanSignalStrengthThreshold());
        
        // bgscan short interval
        m_bgscanShortIntervalField = new NumberField();
        m_bgscanShortIntervalField.setPropertyEditorType(Integer.class);
        m_bgscanShortIntervalField.setAllowDecimals(false);
        m_bgscanShortIntervalField.setAllowNegative(false);
        m_bgscanShortIntervalField.setAllowBlank(true);
        m_bgscanShortIntervalField.setName("bgscanShortInterval");
        m_bgscanShortIntervalField.setFieldLabel(MSGS.netWifiWirelessBgscanShortInterval());
        m_bgscanShortIntervalField.addListener(Events.OnMouseOver, new MouseOverListener(MSGS.netWifiToolTipBgScanShortInterval()));
        m_bgscanShortIntervalField.addPlugin(m_dirtyPlugin);
        //m_bgscanShortInterval.setStyleAttribute("margin-top", Constants.LABEL_MARGIN_TOP_SEPARATOR);
        
        // bgscan long interval
        m_bgscanLongIntervalField = new NumberField();
        m_bgscanLongIntervalField.setPropertyEditorType(Integer.class);
        m_bgscanLongIntervalField.setAllowDecimals(false);
        m_bgscanLongIntervalField.setAllowNegative(false);
        m_bgscanLongIntervalField.setAllowBlank(true);
        m_bgscanLongIntervalField.setName("bgscanLongInterval");
        m_bgscanLongIntervalField.setFieldLabel(MSGS.netWifiWirelessBgscanLongInterval());
        m_bgscanLongIntervalField.addListener(Events.OnMouseOver, new MouseOverListener(MSGS.netWifiToolTipBgScanLongInterval()));
        m_bgscanLongIntervalField.addPlugin(m_dirtyPlugin);
        
        m_pingAccessPointRadioTrue = new Radio();  
        m_pingAccessPointRadioTrue.setBoxLabel(MSGS.trueLabel());
        m_pingAccessPointRadioTrue.setItemId("true");
        m_pingAccessPointRadioTrue.addListener(Events.OnMouseOver, new MouseOverListener(MSGS.netWifiToolTipPingAccessPoint()));
        
        m_pingAccessPointRadioFalse = new Radio();  
        m_pingAccessPointRadioFalse.setBoxLabel(MSGS.falseLabel());  
        m_pingAccessPointRadioFalse.setItemId("false");
        m_pingAccessPointRadioFalse.addListener(Events.OnMouseOver, new MouseOverListener(MSGS.netWifiToolTipPingAccessPoint()));
        
        m_pingAccessPointRadioGroup = new RadioGroup();
        m_pingAccessPointRadioGroup.setName("pingAccessPoint");
        m_pingAccessPointRadioGroup.setFieldLabel(MSGS.netWifiWirelessPingAccessPoint()); 
        m_pingAccessPointRadioGroup.add(m_pingAccessPointRadioTrue);  
        m_pingAccessPointRadioGroup.add(m_pingAccessPointRadioFalse);
        m_pingAccessPointRadioGroup.addPlugin(m_dirtyPlugin);
        m_pingAccessPointRadioGroup.setStyleAttribute("margin-top", Constants.LABEL_MARGIN_TOP_SEPARATOR);
        
        m_ignoreSsidRadioTrue = new Radio();  
        m_ignoreSsidRadioTrue.setBoxLabel(MSGS.trueLabel());
        m_ignoreSsidRadioTrue.setItemId("true");
        m_ignoreSsidRadioTrue.addListener(Events.OnMouseOver, new MouseOverListener(MSGS.netWifiToolTipIgnoreSSID()));
        
        m_ignoreSsidRadioFalse = new Radio();  
        m_ignoreSsidRadioFalse.setBoxLabel(MSGS.falseLabel());  
        m_ignoreSsidRadioFalse.setItemId("false");
        m_ignoreSsidRadioFalse.addListener(Events.OnMouseOver, new MouseOverListener(MSGS.netWifiToolTipIgnoreSSID()));
        
        m_ignoreSsidRadioGroup = new RadioGroup();
        m_ignoreSsidRadioGroup.setName("ignoreSSID");
        m_ignoreSsidRadioGroup.setFieldLabel(MSGS.netWifiWirelessIgnoreSSID()); 
        m_ignoreSsidRadioGroup.add(m_ignoreSsidRadioTrue);  
        m_ignoreSsidRadioGroup.add(m_ignoreSsidRadioFalse);
        m_ignoreSsidRadioGroup.addPlugin(m_dirtyPlugin);  
        m_ignoreSsidRadioGroup.setStyleAttribute("margin-top", Constants.LABEL_MARGIN_TOP_SEPARATOR);
                
        //
        // Channel
        // 
        GroupingStore<GwtWifiChannelModel> grtoupingStore = new GroupingStore<GwtWifiChannelModel>();  
        grtoupingStore.setMonitorChanges(true);  
        grtoupingStore.add(GwtWifiChannelModel.getChannels());
        grtoupingStore.groupBy("band");
        
        ColumnConfig channel = new ColumnConfig("name", "All Available Channels", 20);  
        ColumnConfig frequency = new ColumnConfig("frequency", "Frequency (MHz)", 10);  
        ColumnConfig band = new ColumnConfig("band", "Spectrum Band", 20);  
           
        List<ColumnConfig> channelColumnConfig = new ArrayList<ColumnConfig>();  
        channelColumnConfig.add(m_checkboxChannelSelectionModel.getColumn()); 
        channelColumnConfig.add(channel);  
        channelColumnConfig.add(frequency);  
        channelColumnConfig.add(band);  
      
        final ColumnModel columnModel = new ColumnModel(channelColumnConfig);  
        m_channelGroupingView = new GroupingView() {  
              
            @Override  
            protected void onMouseDown(GridEvent<ModelData> ge) {  
              El hd = ge.getTarget(".x-grid-group-hd", 10);  
              El target = ge.getTargetEl();  
              if (hd != null && target.hasStyleName(m_uncheckedStyle) || target.hasStyleName(m_checkedStyle)) {  
                boolean checked = !ge.getTargetEl().hasStyleName(m_uncheckedStyle);  
                checked = !checked;  
                if (checked) {  
                  ge.getTargetEl().replaceStyleName(m_uncheckedStyle, m_checkedStyle);  
                } else {  
                  ge.getTargetEl().replaceStyleName(m_checkedStyle, m_uncheckedStyle);  
                }  
        
                Element group = (Element) findGroup(ge.getTarget());  
                if (group != null) {  
                  NodeList<Element> rows = El.fly(group).select(".x-grid3-row");  
                  List<ModelData> temp = new ArrayList<ModelData>();  
                  for (int i = 0; i < rows.getLength(); i++) {  
                    Element r = rows.getItem(i);  
                    int idx = findRowIndex(r);  
                    ModelData m = grid.getStore().getAt(idx);  
                    temp.add(m);  
                  }  
                  if (checked) {  
                    grid.getSelectionModel().select(temp, true);  
                  } else {  
                    grid.getSelectionModel().deselect(temp);  
                  }  
                }  
                return;  
              }  
              super.onMouseDown(ge);  
            }  
        
          };  
         
        m_channelGroupingView.setShowGroupedColumn(false);  
        m_channelGroupingView.setForceFit(true);  
        m_channelGroupingView.setShowDirtyCells(true);
        m_channelGroupingView.setGroupRenderer(new GridGroupRenderer() {  
            public String render(GroupColumnData data) {  
              String f = columnModel.getColumnById(data.field).getHeader();  
              String l = data.models.size() == 1 ? "Item" : "Items";  
              return "<div class='x-grid3-group-checker'><div class='" + m_uncheckedStyle + "'> </div></div> " + f  
                  + ": " + data.group + " (" + data.models.size() + " " + l + ")";  
            }  
          });  
          
        final Grid<GwtWifiChannelModel> channelGrid = new Grid<GwtWifiChannelModel>(grtoupingStore, columnModel);
        channelGrid.setView(m_channelGroupingView);
        channelGrid.setBorders(true);
        channelGrid.addPlugin(m_checkboxChannelSelectionModel);
        channelGrid.setSelectionModel(m_checkboxChannelSelectionModel);
        channelGrid.addPlugin(m_dirtyPlugin);
        
        m_channelPanel = new ContentPanel();
        m_channelPanel.setHeading("Select Channel(s)");
        m_channelPanel.setCollapsible(true);
        m_channelPanel.setFrame(true);
        m_channelPanel.setSize(430, 200);
        m_channelPanel.setLayout(new FitLayout());
        m_channelPanel.setStyleAttribute("margin-top", Constants.LABEL_MARGIN_TOP_SEPARATOR);
        m_channelPanel.add(channelGrid);
        m_channelPanel.addListener(Events.OnMouseOver, new MouseOverListener(MSGS.netWifiToolTipChannels()));
        
        m_checkboxChannelSelectionModel.addSelectionChangedListener(new SelectionChangedListener<GwtWifiChannelModel>() {
            @Override
            public void selectionChanged(
                    SelectionChangedEvent<GwtWifiChannelModel> se) {
                
                updateSelectedChannels(se.getSelection());
            }
        });
        
        // Adjust the number of channels shown based on defined property
        gwtDeviceService.findDeviceConfiguration(new AsyncCallback<ListLoadResult<GwtGroupedNVPair>>() {
			public void onSuccess(ListLoadResult<GwtGroupedNVPair> results) {
				if (results != null) {
					List<GwtGroupedNVPair> pairs = results.getData();
					if (pairs != null) {
						for (GwtGroupedNVPair pair : pairs) {
							String name = pair.getName();
							if (name != null && name.equals("devLastWifiChannel")) {
								int topChannel = Integer.parseInt(pair.getValue());
								// Remove channels 12 and 13
								if (topChannel < MAX_WIFI_CHANNEL) {
									channelGrid.getStore().remove(MAX_WIFI_CHANNEL - 1);
									channelGrid.getStore().remove(MAX_WIFI_CHANNEL - 2);
								}
							}
						}
					}
				}
			}
			
			public void onFailure(Throwable caught) {
				FailureHandler.handle(caught);
			}
        });
 
        fieldSet.add(m_modeCombo, formData);
        fieldSet.add(m_ssidField, formData);
        fieldSet.add(m_radioModeCombo, formData);
        fieldSet.add(m_securityCombo, formData);
        fieldSet.add(m_passwordField, formData);
        fieldSet.add(m_verifyPasswordField, formData); 
    	fieldSet.add(m_pairwiseCiphersCombo, formData);
    	fieldSet.add(m_groupCiphersCombo, formData);
    	fieldSet.add(m_bgscanModuleCombo, formData);
    	fieldSet.add(m_bgscanRssiThresholdSliderField, formData);
    	fieldSet.add(m_bgscanShortIntervalField, formData);
    	fieldSet.add(m_bgscanLongIntervalField, formData);
    	fieldSet.add(m_pingAccessPointRadioGroup, formData);
    	fieldSet.add(m_ignoreSsidRadioGroup, formData);
    	fieldSet.add(m_channelPanel, formData);
    	 
        if ((m_securityCombo.getSimpleValue().equals(MessageUtils.get(GwtWifiSecurity.netWifiSecurityWPA2.name())))
				|| (m_securityCombo.getSimpleValue().equals(MessageUtils.get(GwtWifiSecurity.netWifiSecurityWPA.name())))
				|| (m_securityCombo.getSimpleValue().equals(MessageUtils.get(GwtWifiSecurity.netWifiSecurityWPA_WPA2.name())))) {
        	
            if (GwtWifiWirelessMode.netWifiWirelessModeStation.equals(m_modeCombo.getValue().getMode())) {
        		m_pairwiseCiphersCombo.setEnabled(true);
        		m_groupCiphersCombo.setEnabled(true);
        	} else {
        		m_pairwiseCiphersCombo.setEnabled(false);
        		m_groupCiphersCombo.setEnabled(false);
        	}
        } else {
        	m_pairwiseCiphersCombo.setEnabled(false);
        	m_groupCiphersCombo.setEnabled(false);
        }
        	 
	    m_formPanel.add(fieldSet);
	    m_formPanel.setScrollMode(Scroll.AUTO);
	    add(m_formPanel);
	    setScrollMode(Scroll.AUTOX);
	    m_initialized = true;
	}
    
    public GwtWifiWirelessMode getWirelessMode() {
    	Log.warn("[+] WirelessConfigTab :: getWirelessMode()");
		if (m_modeCombo != null) {
		    return m_modeCombo.getValue().getMode();
		} 
		else if(m_activeWifiConfig != null) {
			return GwtWifiWirelessMode.valueOf(m_activeWifiConfig.getWirelessMode());
		} 
		/*
		else if(m_selectNetIfConfig != null) {
			return GwtWifiWirelessMode.valueOf(m_selectNetIfConfig.getWirelessMode());
		}
		*/
		
		Log.warn("[- null] WirelessConfigTab :: getWirelessMode()");
    	return null;
    }
	    
	public void refresh() {
		if (m_dirty && m_initialized) {			
	        m_dirty = false;
			if (m_selectNetIfConfig == null) {
				reset();
	        }
			else {
				update();				
			}		
		}
	}	
	
	private void update() {
		for (Field<?> field : m_formPanel.getFields()) {
			FormUtils.removeDirtyFieldIcon(field);
		}
		setValues(true);
		
		for (Field<?> field : m_formPanel.getFields()) {
			FormUtils.removeDirtyFieldIcon(field);
		}
		refreshForm();
	}
	
	private void updateSelectedChannels(List<GwtWifiChannelModel> channels) {
		
	    /*
        if (GwtWifiWirelessMode.netWifiWirelessModeAdHoc.equals(m_modeCombo.getValue().getMode()) 
                || GwtWifiWirelessMode.netWifiWirelessModeAccessPoint.equals(m_modeCombo.getValue().getMode())) {
        */
	    if(GwtWifiWirelessMode.netWifiWirelessModeAccessPoint.equals(m_modeCombo.getValue().getMode())) {
		
			if (channels != null) {
				int ind = 0;
				for (GwtWifiChannelModel channel : channels) {
					if (ind > 0) {
						m_checkboxChannelSelectionModel.deselect(channel.getChannel()-1);
					}
					ind++;
				}
			}
		}
	}
		
	private void refreshForm() {
		if (m_formPanel != null) {
			
			GwtNetIfStatus tcpIpStatus = m_tcpIpConfigTab.getStatus();
			//GwtWifiWirelessMode wifiMode = m_modeCombo.getValue().getMode();
			
			// tcp/ip disabled
	  		if (tcpIpStatus.equals(GwtNetIfStatus.netIPv4StatusDisabled)) {
	  			m_channelPanel.setEnabled(false);
				for (Field<?> field : m_formPanel.getFields()) {
					field.setEnabled(false);
				}
				
			// wireless disabled - leave mode selectable
			/*
            } else if (GwtWifiWirelessMode.netWifiWirelessModeDisabled.equals(wifiMode)) {
	  			m_channelPanel.setEnabled(false);
				for (Field<?> field : m_formPanel.getFields()) {		
					field.setEnabled(false);
				}
				m_modeCombo.setEnabled(true);
			*/
				
	  		} else {
	  			m_channelPanel.setEnabled(true);
				for (Field<?> field : m_formPanel.getFields()) {
					field.setEnabled(true);
					field.clearInvalid();
					field.validate();
				}
				
				// Station mode
				if(GwtWifiWirelessMode.netWifiWirelessModeStation.equals(m_modeCombo.getValue().getMode())) {
					for (Field<?> field : m_formPanel.getFields()) {			
						if (field != m_modeCombo
								&& field != m_ssidField
								&& field != m_securityCombo
								&& field != m_pairwiseCiphersCombo
								&& field != m_groupCiphersCombo
								&& field != m_passwordField
								&& field != m_bgscanModuleCombo
								&& field != m_bgscanRssiThresholdSliderField
								&& field != m_bgscanShortIntervalField
								&& field != m_bgscanLongIntervalField
								&& field != m_pingAccessPointRadioTrue
								&& field != m_pingAccessPointRadioFalse
								&& field != m_pingAccessPointRadioGroup
								&& field != m_ignoreSsidRadioGroup) {
							
							field.setEnabled(false);
						}
					}
					m_verifyPasswordField.setAllowBlank(true);

				/*
                // Ad-hoc mode
				} else if(GwtWifiWirelessMode.netWifiWirelessModeAdHoc.equals(m_modeCombo.getValue().getMode())) {
					for (Field<?> field : m_formPanel.getFields()) {			
						if (field != m_modeCombo
								&& field != m_ssidField
								&& field != m_securityCombo
								&& field != m_passwordField) {
							
							field.setEnabled(false);
						}
					}
				*/

                // Access Point mode
				} else if(GwtWifiWirelessMode.netWifiWirelessModeAccessPoint.equals(m_modeCombo.getValue().getMode())) {
					// Disable Access Point mode when TCP/IP is set to WAN
					if (tcpIpStatus.equals(GwtNetIfStatus.netIPv4StatusEnabledWAN)) {
						for (Field<?> field : m_formPanel.getFields()) {			
							if (field != m_modeCombo) {
								field.setEnabled(false);
							}
						}
						m_verifyPasswordField.setAllowBlank(false);
					}
				}
				
				// disable password fields if security is None
				if (m_securityCombo.getSimpleValue().equals(MessageUtils.get(GwtWifiSecurity.netWifiSecurityNONE.name()))) {
					m_passwordField.setEnabled(false, false);
					m_verifyPasswordField.setEnabled(false);
					
					m_passwordField.clearInvalid();
					m_verifyPasswordField.clearInvalid();
				}
				
				if (GwtWifiWirelessMode.netWifiWirelessModeStation.equals(m_modeCombo.getValue().getMode())) {
					m_ssidField.setEnabled(true, true);
					if (!m_securityCombo.getSimpleValue().equals(MessageUtils.get(GwtWifiSecurity.netWifiSecurityNONE.name()))) {
						if ((m_passwordField.getValue() != null) && (m_passwordField.getValue().length() > 0)) {
							m_passwordField.setEnabled(true, true);
						} else {
							m_passwordField.setEnabled(true, false);
						}
					}
					
					for (Field<?> field : m_formPanel.getFields()) {
						
						if (field.equals(m_bgscanModuleCombo)) {
							field.setEnabled(true);
						} else if (field.equals(m_bgscanRssiThresholdSliderField) 
								|| field.equals(m_bgscanShortIntervalField)
								|| field.equals(m_bgscanLongIntervalField)) {
				
							if ((m_bgscanModuleCombo.getSimpleValue().equals(MessageUtils.get(GwtWifiBgscanModule.netWifiBgscanMode_SIMPLE.name())))
									|| (m_bgscanModuleCombo.getSimpleValue().equals(MessageUtils.get(GwtWifiBgscanModule.netWifiBgscanMode_LEARN.name())))) {
								field.setEnabled(true);
							} else {
								field.setEnabled(false);
							}
						}
					}
				} else {
					m_ssidField.setEnabled(true, false);
					if (!m_securityCombo.getSimpleValue().equals(MessageUtils.get(GwtWifiSecurity.netWifiSecurityNONE.name()))) {
						m_passwordField.setEnabled(true, false);
					}
					for (Field<?> field : m_formPanel.getFields()) {
						if (field.equals(m_bgscanModuleCombo)
								|| field.equals(m_bgscanRssiThresholdSliderField) 
								|| field.equals(m_bgscanShortIntervalField)
								|| field.equals(m_bgscanLongIntervalField)
								|| field.equals(m_pingAccessPointRadioTrue)
								|| field.equals(m_pingAccessPointRadioFalse)
								|| field.equals(m_pingAccessPointRadioGroup)) {
							field.setEnabled(false);
						}
					}
				}
				
				if ((m_securityCombo.getSimpleValue().equals(MessageUtils.get(GwtWifiSecurity.netWifiSecurityWPA2.name())))
					|| (m_securityCombo.getSimpleValue().equals(MessageUtils.get(GwtWifiSecurity.netWifiSecurityWPA.name())))
					|| (m_securityCombo.getSimpleValue().equals(MessageUtils.get(GwtWifiSecurity.netWifiSecurityWPA_WPA2.name())))) {
					
					for (Field<?> field : m_formPanel.getFields()) {
						if (field.equals(m_pairwiseCiphersCombo) 
								|| field.equals(m_groupCiphersCombo)) {
							
						    if (GwtWifiWirelessMode.netWifiWirelessModeStation.equals(m_modeCombo.getValue().getMode())) {
								field.setEnabled(true);
							} else {
								field.setEnabled(false);
							}
						}
					}
				} else {
					for (Field<?> field : m_formPanel.getFields()) {			
						if (field.equals(m_pairwiseCiphersCombo) 
								|| field.equals(m_groupCiphersCombo)) {

							field.setEnabled(false);
						}
					}
				}
	  		}
	  		
	  		m_netInterfaceConfigTabs.adjustInterfaceTabs();
		}
	}
	
	
	private void reset()
	{
		m_modeCombo.setValue(m_wirelessModeListStore.findModel(GwtWifiWirelessModeModel.MODE, GwtWifiWirelessMode.netWifiWirelessModeStation.name()));
		m_modeCombo.setOriginalValue(m_modeCombo.getValue());
		
		m_ssidField.setValue("");
		m_ssidField.setOriginalValue("");

		m_radioModeCombo.setSimpleValue(MessageUtils.get(GwtWifiRadioMode.netWifiRadioModeBGN.name()));
		m_radioModeCombo.setOriginalValue(m_radioModeCombo.getValue());

		m_securityCombo.setSimpleValue(MessageUtils.get(GwtWifiSecurity.netWifiSecurityWPA2.name()));
		m_securityCombo.setOriginalValue(m_securityCombo.getValue());
		
		m_pairwiseCiphersCombo.setSimpleValue(MessageUtils.get(GwtWifiCiphers.netWifiCiphers_CCMP_TKIP.name()));
		m_pairwiseCiphersCombo.setOriginalValue(m_pairwiseCiphersCombo.getValue());
		
		m_groupCiphersCombo.setSimpleValue(MessageUtils.get(GwtWifiCiphers.netWifiCiphers_CCMP_TKIP.name()));
		m_groupCiphersCombo.setOriginalValue(m_groupCiphersCombo.getValue());
		
		m_bgscanModuleCombo.setSimpleValue(MessageUtils.get(GwtWifiBgscanModule.netWifiBgscanMode_NONE.name()));
		m_bgscanModuleCombo.setOriginalValue(m_bgscanModuleCombo.getValue());
		
		m_bgscanRssiThresholdSlider.setValue(0);
		m_bgscanRssiThresholdSliderField.setValue(0);
		m_bgscanRssiThresholdSliderField.setOriginalValue(0);
		
		m_bgscanShortIntervalField.setValue(0);
		m_bgscanShortIntervalField.setOriginalValue(m_bgscanShortIntervalField.getValue());
		
		m_bgscanLongIntervalField.setValue(0);
		m_bgscanLongIntervalField.setOriginalValue(m_bgscanLongIntervalField.getValue());
		
		m_passwordField.setValue("");
		m_passwordField.setOriginalValue("");
		
		m_verifyPasswordField.setValue("");
		m_verifyPasswordField.setOriginalValue("");
		
		m_pingAccessPointRadioGroup.setValue(m_pingAccessPointRadioFalse);
		m_pingAccessPointRadioGroup.setOriginalValue(m_pingAccessPointRadioGroup.getValue());
		
		m_ignoreSsidRadioGroup.setValue(m_ignoreSsidRadioFalse);
		m_ignoreSsidRadioGroup.setOriginalValue(m_ignoreSsidRadioGroup.getValue());
		
		update();
	}
	
	private void setValues(boolean setOriginal) {
		if (m_activeWifiConfig != null) {
		    m_modeCombo.setValue(m_wirelessModeListStore.findModel(GwtWifiWirelessModeModel.MODE, GwtWifiWirelessMode.valueOf(m_activeWifiConfig.getWirelessMode()).name()));
		    if(setOriginal) m_modeCombo.setOriginalValue(m_modeCombo.getValue());
		    
			m_ssidField.setValue(m_activeWifiConfig.getWirelessSsid());
			if(setOriginal) m_ssidField.setOriginalValue(m_ssidField.getValue());

			m_radioModeCombo.setSimpleValue(MessageUtils.get(m_activeWifiConfig.getRadioMode()));
			if(setOriginal) m_radioModeCombo.setOriginalValue(m_radioModeCombo.getValue());
			
			ArrayList<Integer> alChannels =  m_activeWifiConfig.getChannels();
			if ((alChannels != null) && (alChannels.size() > 0)) {
				// deselect all channels
				for (int channel = 1; channel <= MAX_WIFI_CHANNEL; channel++) {
					m_checkboxChannelSelectionModel.deselect(channel-1);
				}
				// select proper channels
				for (int channel : alChannels) {
					m_checkboxChannelSelectionModel.select(channel-1, true);
				}
			} else {
				Log.warn("No channels specified, selecting all ...");
				for (int channel = 1; channel <= MAX_WIFI_CHANNEL; channel++) {
					m_checkboxChannelSelectionModel.select(channel-1, true);
				}
			}
				
			m_securityCombo.setSimpleValue(MessageUtils.get(m_activeWifiConfig.getSecurity()));
			if(setOriginal) m_securityCombo.setOriginalValue(m_securityCombo.getValue());
			
			String sPairwiseCiphers = m_activeWifiConfig.getPairwiseCiphers();
			if (sPairwiseCiphers != null) {
				m_pairwiseCiphersCombo.setSimpleValue(MessageUtils.get(sPairwiseCiphers));
				if(setOriginal) m_pairwiseCiphersCombo.setOriginalValue(m_pairwiseCiphersCombo.getValue());
			}
			
			String sGroupCiphers = m_activeWifiConfig.getPairwiseCiphers();
			if (sGroupCiphers != null) {
				m_groupCiphersCombo.setSimpleValue(MessageUtils.get(sGroupCiphers));
				if(setOriginal) m_groupCiphersCombo.setOriginalValue(m_groupCiphersCombo.getValue());
			}
			
			String sBgscanModule = m_activeWifiConfig.getBgscanModule();
			if (sBgscanModule != null) {
				m_bgscanModuleCombo.setSimpleValue(MessageUtils.get(sBgscanModule));
				if(setOriginal) m_bgscanModuleCombo.setOriginalValue(m_bgscanModuleCombo.getValue());
			}
			
			m_bgscanRssiThresholdSlider.setValue(m_activeWifiConfig.getBgscanRssiThreshold());
			m_bgscanRssiThresholdSliderField.setValue(m_activeWifiConfig.getBgscanRssiThreshold());
			if(setOriginal) m_bgscanRssiThresholdSliderField.setOriginalValue(m_activeWifiConfig.getBgscanRssiThreshold());
			
			m_bgscanShortIntervalField.setValue(m_activeWifiConfig.getBgscanShortInterval());
			if(setOriginal) m_bgscanShortIntervalField.setOriginalValue(m_bgscanShortIntervalField.getValue());
			
			m_bgscanLongIntervalField.setValue(m_activeWifiConfig.getBgscanLongInterval());
			if(setOriginal) m_bgscanLongIntervalField.setOriginalValue(m_bgscanLongIntervalField.getValue());
			
			m_passwordField.setValue(m_activeWifiConfig.getPassword());
			if(setOriginal) m_passwordField.setOriginalValue(m_passwordField.getValue());
			
			m_verifyPasswordField.setValue(m_activeWifiConfig.getPassword());
			if(setOriginal) m_verifyPasswordField.setOriginalValue(m_verifyPasswordField.getValue());
		
			if (m_activeWifiConfig.pingAccessPoint()) {
				m_pingAccessPointRadioTrue.setValue(true);
				m_pingAccessPointRadioTrue.setOriginalValue(m_pingAccessPointRadioTrue.getValue());
				
				m_pingAccessPointRadioFalse.setValue(false);
				m_pingAccessPointRadioFalse.setOriginalValue(m_pingAccessPointRadioFalse.getValue());
				
				m_pingAccessPointRadioGroup.setOriginalValue(m_pingAccessPointRadioTrue);
				m_pingAccessPointRadioGroup.setValue(m_pingAccessPointRadioGroup.getValue());
			} else {
				m_pingAccessPointRadioTrue.setValue(false);
				m_pingAccessPointRadioTrue.setOriginalValue(m_pingAccessPointRadioTrue.getValue());

				m_pingAccessPointRadioFalse.setValue(true);
				m_pingAccessPointRadioFalse.setOriginalValue(m_pingAccessPointRadioFalse.getValue());

				m_pingAccessPointRadioGroup.setOriginalValue(m_pingAccessPointRadioFalse);
				m_pingAccessPointRadioGroup.setValue(m_pingAccessPointRadioGroup.getValue());
			}
			
			if (m_activeWifiConfig.ignoreSSID()) {
				m_ignoreSsidRadioTrue.setValue(true);
				m_ignoreSsidRadioTrue.setOriginalValue(m_ignoreSsidRadioTrue.getValue());
				
				m_ignoreSsidRadioFalse.setValue(false);
				m_ignoreSsidRadioFalse.setOriginalValue(m_ignoreSsidRadioFalse.getValue());
				
				m_ignoreSsidRadioGroup.setOriginalValue(m_ignoreSsidRadioTrue);
				m_ignoreSsidRadioGroup.setValue(m_ignoreSsidRadioGroup.getValue());
			} else {
				m_ignoreSsidRadioTrue.setValue(false);
				m_ignoreSsidRadioTrue.setOriginalValue(m_ignoreSsidRadioTrue.getValue());

				m_ignoreSsidRadioFalse.setValue(true);
				m_ignoreSsidRadioFalse.setOriginalValue(m_ignoreSsidRadioFalse.getValue());

				m_ignoreSsidRadioGroup.setOriginalValue(m_ignoreSsidRadioFalse);
				m_ignoreSsidRadioGroup.setValue(m_ignoreSsidRadioGroup.getValue());
			}
		}		
	}
	
	private void setPasswordValidation() {
		
		m_passwordField.clearInvalid();
		
		// Access Point mode
			// change password validation criteria
			if(m_securityCombo.getSimpleValue() == MessageUtils.get(GwtWifiSecurity.netWifiSecurityWPA.name())) {
		        m_passwordField.setRegex(PASSWORD_REGEX_WPA);
		        m_passwordField.getMessages().setRegexText(MSGS.netWifiWirelessInvalidWPAPassword());
			} else if(m_securityCombo.getSimpleValue() == MessageUtils.get(GwtWifiSecurity.netWifiSecurityWPA2.name())) {
				m_passwordField.setRegex(PASSWORD_REGEX_WPA);
				m_passwordField.getMessages().setRegexText(MSGS.netWifiWirelessInvalidWPAPassword());
			} else if(m_securityCombo.getSimpleValue() == MessageUtils.get(GwtWifiSecurity.netWifiSecurityWPA_WPA2.name())) {
				m_passwordField.setRegex(PASSWORD_REGEX_WPA);
				m_passwordField.getMessages().setRegexText(MSGS.netWifiWirelessInvalidWPAPassword());
			} else if(m_securityCombo.getSimpleValue() == MessageUtils.get(GwtWifiSecurity.netWifiSecurityWEP.name())) {
				m_passwordField.setRegex(PASSWORD_REGEX_WEP);
				m_passwordField.getMessages().setRegexText(MSGS.netWifiWirelessInvalidWEPPassword());
			} else {
				m_passwordField.setRegex(PASSWORD_REGEX_ANY);
			}
		
		m_passwordField.validate();
	}
	

	private El findCheck(Element group) {
		return El.fly(group).selectNode(".x-grid3-group-checker").firstChild();
	}

	private void setGroupChecked(Element group, boolean checked) {
		findCheck(group).replaceStyleName(
				checked ? m_uncheckedStyle : m_checkedStyle,
				checked ? m_checkedStyle : m_uncheckedStyle);
	}
	
	
	// Combo box item template
    private native String getTemplate() /*-{
        return  [
	    '<tpl for=".">',
	    '<div class="x-combo-list-item" qtitle="{name}">{name}</div>',
	    '</tpl>'
	    ].join("");
    }-*/;
    
    private Window createWifiNetworksWindow() {
    	
		final Window window = new Window();
		window.setSize(700, 400);
		window.setPlain(true);
		window.setModal(true);
		window.setBlinkModal(true);
		window.setHeading("Wireless Networks");

		// Create a table to layout the content
		VerticalPanel dialogContents = new VerticalPanel();
		dialogContents.setSpacing(4);
               
        window.add(dialogContents);
        
        List<ColumnConfig> configs = new ArrayList<ColumnConfig>();  
        ColumnConfig column = null;  
        
        column = new ColumnConfig("ssid", "SSID", 100);
        column.setAlignment(HorizontalAlignment.LEFT);
        configs.add(column);
        
        column = new ColumnConfig("macAddress", "MAC Address", 100);  
        column.setAlignment(HorizontalAlignment.CENTER);
        configs.add(column);  
        
        column = new ColumnConfig("signalStrength", "Signal (dBm)", 100);  
        column.setAlignment(HorizontalAlignment.CENTER);
        configs.add(column); 
        
        column = new ColumnConfig("channel", "Channel", 100);
        column.setAlignment(HorizontalAlignment.CENTER);
        configs.add(column);
        
        column = new ColumnConfig("frequency", "Frequency", 100);    
        column.setAlignment(HorizontalAlignment.CENTER);
        configs.add(column);  
        
        column = new ColumnConfig("security", "Security", 100); 
        column.setAlignment(HorizontalAlignment.LEFT);
        configs.add(column); 
        
        /*
        CheckColumnConfig checkColumn = new CheckColumnConfig("selectAP", "Select", 55);
        CellEditor checkBoxEditor = new CellEditor(new CheckBox());  
        checkColumn.setEditor(checkBoxEditor);
        configs.add(checkColumn); 
        */
        
     // rpc data proxy  
        RpcProxy<ListLoadResult<GwtWifiHotspotEntry>> proxy = new RpcProxy<ListLoadResult<GwtWifiHotspotEntry>>() {  
          @Override  
          protected void load(Object loadConfig, AsyncCallback<ListLoadResult<GwtWifiHotspotEntry>> callback) {
        	  gwtNetworkService.findWifiHotspots(m_selectNetIfConfig.getName(), callback);
          }
        };
        
        m_wifiHotspotLoader = new BaseListLoader<ListLoadResult<GwtWifiHotspotEntry>>(proxy);
        m_wifiHotspotLoader.setSortDir(SortDir.ASC);  
        m_wifiHotspotLoader.setSortField("signalStrength");
        //m_wifiHotspotLoader.setRemoteSort(true);  
        
        SwappableListStore<GwtWifiHotspotEntry> store = new SwappableListStore<GwtWifiHotspotEntry>(m_wifiHotspotLoader);
        store.setKeyProvider( new ModelKeyProvider<GwtWifiHotspotEntry>() {            
            public String getKey(GwtWifiHotspotEntry wifiHotspotEntry) {
                return wifiHotspotEntry.getSignalStrength().toString();
            }
        });
        
   
        m_grid = new Grid<GwtWifiHotspotEntry>(store, new ColumnModel(configs));
        m_grid.setBorders(false);
        m_grid.setStateful(false);
        m_grid.setLoadMask(true);
        m_grid.setStripeRows(true);
        m_grid.setColumnLines(true);
        m_grid.setColumnReordering(true);
        m_grid.setAutoExpandColumn("ssid");
        m_grid.getView().setAutoFill(true); 
        //m_grid.addPlugin(checkColumn);
         
        m_wifiHotspotLoader.addLoadListener(new DataLoadListener(m_grid));
        
        GridSelectionModel<GwtWifiHotspotEntry> selectionModel = new GridSelectionModel<GwtWifiHotspotEntry>();
        selectionModel.setSelectionMode(SelectionMode.SINGLE);
        
        m_grid.setSelectionModel(selectionModel);
        //m_grid.addPlugin(selectionModel);
        
        m_grid.getSelectionModel().addSelectionChangedListener(new SelectionChangedListener<GwtWifiHotspotEntry>() {
            @Override
            public void selectionChanged(SelectionChangedEvent<GwtWifiHotspotEntry> se) {
	            if (se != null) {
	            	List<GwtWifiHotspotEntry> list = se.getSelection();
	            	if ((list != null) && (list.size() > 0)) {
	            		GwtWifiHotspotEntry wifiHotspotEntry = list.get(0);
	            		if (wifiHotspotEntry != null) {
	            			m_ssidField.setValue(wifiHotspotEntry.getSSID());
	            			String security = wifiHotspotEntry.getSecurity();
	            			if (security.equals("None")) {
	            				m_securityCombo.setSimpleValue(MessageUtils.get(GwtWifiSecurity.netWifiSecurityNONE.name()));
	            			} else if (security.equals("WEP")) {
	            				m_securityCombo.setSimpleValue(MessageUtils.get(GwtWifiSecurity.netWifiSecurityWEP.name()));
	            			} else if (security.equals("WPA")) {
	            				m_securityCombo.setSimpleValue(MessageUtils.get(GwtWifiSecurity.netWifiSecurityWPA.name()));
	            			} else if (security.equals("WPA2") || security.equals("WPA/WPA2")) {
	            				m_securityCombo.setSimpleValue(MessageUtils.get(GwtWifiSecurity.netWifiSecurityWPA2.name()));
	            			} else {
	            				m_securityCombo.setSimpleValue(MessageUtils.get(GwtWifiSecurity.netWifiSecurityNONE.name()));
	            			}
	            			
	            			// deselect all channels
	        				for (int channel = 1; channel <= MAX_WIFI_CHANNEL; channel++) {
	        					m_checkboxChannelSelectionModel.deselect(channel-1);
	        				}
	        				// select proper channels
	        				m_checkboxChannelSelectionModel.select(wifiHotspotEntry.getChannel()-1, true);
	        				
	            			window.hide();
	            		}
	             	}
	            }
            }
        });
        
        ContentPanel cp = new ContentPanel();  
        cp.setHeading("Wireless Networks in Range");  
        cp.setFrame(true);  
        
        cp.setSize(680, 365);  
        
        FillLayout layout = new FillLayout();
        layout.setAdjustForScroll(true);
        cp.setLayout(layout);  
        
        cp.add(m_grid);  
        dialogContents.add(cp);
        window.add(dialogContents);
        
        return window;
    }
    
 private GwtWifiConfig getGwtWifiConfig() {
    	
    	GwtWifiConfig gwtWifiConfig = new GwtWifiConfig();
    	
		// mode
    	GwtWifiWirelessMode wifiMode = m_modeCombo.getValue().getMode(); 
    	gwtWifiConfig.setWirelessMode(wifiMode.name());
		
        // ssid
    	gwtWifiConfig.setWirelessSsid(m_ssidField.getValue());

        // driver
        String driver = "";
        if(GwtWifiWirelessMode.netWifiWirelessModeAccessPoint.equals(wifiMode)) {
        	driver = m_selectNetIfConfig.getAccessPointWifiConfig().getDriver();
        } else if(GwtWifiWirelessMode.netWifiWirelessModeAdHoc.equals(wifiMode)) {
        	driver = m_selectNetIfConfig.getAdhocWifiConfig().getDriver();
        } else if(GwtWifiWirelessMode.netWifiWirelessModeStation.equals(wifiMode)) {
        	driver = m_selectNetIfConfig.getStationWifiConfig().getDriver();
        }
        gwtWifiConfig.setDriver(driver);  // use previous value
		
		// radio mode
		String radioValue = m_radioModeCombo.getValue().getValue();
		for (GwtWifiRadioMode mode : GwtWifiRadioMode.values()) {
			if (MessageUtils.get(mode.name()).equals(radioValue)) {
				gwtWifiConfig.setRadioMode(mode.name());
			}
		} 
		
		// channels
		List<GwtWifiChannelModel> lSelectedChannels =  m_checkboxChannelSelectionModel.getSelectedItems();
		ArrayList<Integer> alChannels = new ArrayList<Integer>();
		for (GwtWifiChannelModel item : lSelectedChannels) {
			alChannels.add(new Integer(item.getChannel()));
		}
		
		gwtWifiConfig.setChannels(alChannels);
		
		// security
		String secValue = m_securityCombo.getValue().getValue();
		for (GwtWifiSecurity sec : GwtWifiSecurity.values()) {
			if (MessageUtils.get(sec.name()).equals(secValue)) {
				gwtWifiConfig.setSecurity(sec.name());
			}
		}
		
		String pairwiseCiphersValue = m_pairwiseCiphersCombo.getValue().getValue();
		for (GwtWifiCiphers ciphers : GwtWifiCiphers.values()) {
			if (MessageUtils.get(ciphers.name()).equals(pairwiseCiphersValue)) {
				gwtWifiConfig.setPairwiseCiphers(ciphers.name());
			}
		}

		String groupCiphersValue = m_groupCiphersCombo.getValue().getValue();
		for (GwtWifiCiphers ciphers : GwtWifiCiphers.values()) {
			if (MessageUtils.get(ciphers.name()).equals(groupCiphersValue)) {
				gwtWifiConfig.setGroupCiphers(ciphers.name());
			}
		}
		
		// bgscan
		String bgscanModuleValue = m_bgscanModuleCombo.getValue().getValue();
		for (GwtWifiBgscanModule module : GwtWifiBgscanModule.values()) {
			if (MessageUtils.get(module.name()).equals(bgscanModuleValue)) {
				gwtWifiConfig.setBgscanModule(module.name());
			}
		}
		
		gwtWifiConfig.setBgscanRssiThreshold(m_bgscanRssiThresholdSlider.getValue());
		gwtWifiConfig.setBgscanShortInterval(m_bgscanShortIntervalField.getValue().intValue());
		gwtWifiConfig.setBgscanLongInterval(m_bgscanLongIntervalField.getValue().intValue());
		
		// password
		gwtWifiConfig.setPassword(m_passwordField.getValue());
		
		// ping access point
		gwtWifiConfig.setPingAccessPoint(m_pingAccessPointRadioTrue.getValue().booleanValue());
		
		// ignore SSID
		gwtWifiConfig.setIgnoreSSID(m_ignoreSsidRadioTrue.getValue().booleanValue());
		
		return gwtWifiConfig;
    }
    
    private class DataLoadListener extends LoadListener
    {
        private Grid<GwtWifiHotspotEntry> m_grid;
        private GwtWifiHotspotEntry       m_selectedEntry;

        public DataLoadListener(Grid<GwtWifiHotspotEntry> grid) {
            m_grid 			 = grid;
            m_selectedEntry = null;
        }
        
        public void loaderBeforeLoad(LoadEvent le) {
        	m_selectedEntry = m_grid.getSelectionModel().getSelectedItem();
        }
        
        public void loaderLoad(LoadEvent le) {
        	if (le.exception != null) {
                FailureHandler.handle(le.exception);
            }
        	        	
            if (m_selectedEntry != null) {
                ListStore<GwtWifiHotspotEntry> store = m_grid.getStore();
                GwtWifiHotspotEntry modelEntry = store.findModel(m_selectedEntry.getSignalStrength().toString());
                if (modelEntry != null) {
                    m_grid.getSelectionModel().select(modelEntry, false);
                    m_grid.getView().focusRow(store.indexOf(modelEntry));
                }
            }
            m_grid.repaint();
        }
    }
}

