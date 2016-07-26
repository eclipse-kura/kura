package org.eclipse.kura.web.client.network;

import java.util.List;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.Constants;
import org.eclipse.kura.web.client.util.FormUtils;
import org.eclipse.kura.web.client.util.MessageUtils;
import org.eclipse.kura.web.shared.model.GwtModemAuthType;
import org.eclipse.kura.web.shared.model.GwtModemInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtSession;

import com.allen_sauer.gwt.log.client.Log;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.ComponentPlugin;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.form.Field;
import com.extjs.gxt.ui.client.widget.form.FieldSet;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.Radio;
import com.extjs.gxt.ui.client.widget.form.RadioGroup;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.FlowLayout;
import com.extjs.gxt.ui.client.widget.layout.FormData;
import com.extjs.gxt.ui.client.widget.layout.FormLayout;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Element;

public class ModemGpsConfigTab extends LayoutContainer {
	
	private static final Messages MSGS = GWT.create(Messages.class);
	
	private final ToolTipBox toolTipField = new ToolTipBox("275px", "66px");
	private final String defaultToolTip = "Mouse over enabled items on the left to see help text.";
	
	@SuppressWarnings("unused")
    private GwtSession            m_currentSession;

    private boolean               		m_dirty;
    private boolean               		m_initialized;
    private GwtModemInterfaceConfig  	m_selectedModemInterfaceConfig;
    private FormPanel             		m_formPanel;
    
    private Radio 				   	 	m_enableGpsRadioTrue;
	private Radio 				   	 	m_enableGpsRadioFalse;
	private RadioGroup 			   	 	m_enableGpsRadioGroup;
	
    private ComponentPlugin          	m_dirtyPlugin;
    
    private class MouseOverListener implements Listener<BaseEvent> {

    	private String  html;
    	
    	public MouseOverListener(String html) {
    		this.html = html;
    	}
		public void handleEvent(BaseEvent be) {
			toolTipField.setText(html);
		}
    }

    public ModemGpsConfigTab(GwtSession currentSession) {
        m_currentSession = currentSession;
    	m_dirty          = true;
    	m_initialized    = false;
    	final ModemGpsConfigTab theTab = this;
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
    }
    
    public void setNetInterface(GwtNetInterfaceConfig netIfConfig) {
    	m_dirty = true;
    	if(netIfConfig instanceof GwtModemInterfaceConfig) {
    		m_selectedModemInterfaceConfig = (GwtModemInterfaceConfig) netIfConfig;
    	}
    }
    
    public void getUpdatedNetInterface(GwtNetInterfaceConfig updatedNetIf) {
        GwtModemInterfaceConfig updatedModemNetIf = (GwtModemInterfaceConfig) updatedNetIf;
        if (m_formPanel != null) {
        	updatedModemNetIf.setGpsEnabled(m_enableGpsRadioTrue.getValue().booleanValue());
        } else {
        	if(m_selectedModemInterfaceConfig != null) {
    	        Log.debug("Modem config tab not yet rendered, using original values");
    	        updatedModemNetIf.setGpsEnabled(m_selectedModemInterfaceConfig.isGpsEnabled());
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
	                return true;
	            }
        	}
        }
        return false;
    }
    
    protected void onRender(Element parent, int index) { 
    	Log.debug("ModemGpsConfigTab - onRender()");
    	super.onRender(parent, index);         
        setLayout(new FitLayout());
        setId("network-modem-gps");
        
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
        
        m_enableGpsRadioTrue = new Radio();  
        m_enableGpsRadioTrue.setBoxLabel(MSGS.trueLabel());
        m_enableGpsRadioTrue.setItemId("true");
        m_enableGpsRadioTrue.addListener(Events.OnMouseOver, new MouseOverListener(MSGS.netModemToolTipEnableGps()));
        
        m_enableGpsRadioFalse = new Radio();  
        m_enableGpsRadioFalse.setBoxLabel(MSGS.falseLabel());  
        m_enableGpsRadioFalse.setItemId("false");
        m_enableGpsRadioFalse.addListener(Events.OnMouseOver, new MouseOverListener(MSGS.netModemToolTipEnableGps()));
        
        m_enableGpsRadioGroup = new RadioGroup();
        m_enableGpsRadioGroup.setName("modemEnableGps");
        m_enableGpsRadioGroup.setFieldLabel(MSGS.netModemEnableGps()); 
        m_enableGpsRadioGroup.add(m_enableGpsRadioTrue);  
        m_enableGpsRadioGroup.add(m_enableGpsRadioFalse);
        m_enableGpsRadioGroup.addPlugin(m_dirtyPlugin);  
        //m_enableGpsRadioGroup.setStyleAttribute("margin-top", Constants.LABEL_MARGIN_TOP_SEPARATOR);
        fieldSet.add(m_enableGpsRadioGroup, formData);
        
	    m_formPanel.add(fieldSet);
	    m_formPanel.setScrollMode(Scroll.AUTO);
	    add(m_formPanel);
	    setScrollMode(Scroll.AUTOX);
	    m_initialized = true;
    }
    
    public void refresh() {
		Log.debug("ModemGpsConfigTab.refresh()");
		if (m_dirty && m_initialized) {			
	        m_dirty = false;
			if (m_selectedModemInterfaceConfig == null) {
				Log.debug("ModemGpsConfigTab.refresh() - resetting"); 
				reset();
	        } else {
	        	Log.debug("ModemGpsConfigTab.refresh() - updating");
				update();				
			}		
		}
	}
        
    private void update() {
		Log.debug("ModemGpsConfigTab - update()");
		for (Field<?> field : m_formPanel.getFields()) {
			FormUtils.removeDirtyFieldIcon(field);
		}

		if (m_selectedModemInterfaceConfig != null) {
			if (m_selectedModemInterfaceConfig.isGpsEnabled()) {
				m_enableGpsRadioTrue.setValue(true);
				m_enableGpsRadioTrue.setOriginalValue(m_enableGpsRadioTrue.getValue());
				
				m_enableGpsRadioFalse.setValue(false);
				m_enableGpsRadioFalse.setOriginalValue(m_enableGpsRadioFalse.getValue());
				
				m_enableGpsRadioGroup.setOriginalValue(m_enableGpsRadioTrue);
				m_enableGpsRadioGroup.setValue(m_enableGpsRadioGroup.getValue());
			} else {
				m_enableGpsRadioTrue.setValue(false);
				m_enableGpsRadioTrue.setOriginalValue(m_enableGpsRadioTrue.getValue());

				m_enableGpsRadioFalse.setValue(true);
				m_enableGpsRadioFalse.setOriginalValue(m_enableGpsRadioFalse.getValue());

				m_enableGpsRadioGroup.setOriginalValue(m_enableGpsRadioFalse);
				m_enableGpsRadioGroup.setValue(m_enableGpsRadioGroup.getValue());
			}
		} else {
			Log.debug("selected Modem Interface Config is null");
		}
		
		for (Field<?> field : m_formPanel.getFields()) {
			FormUtils.removeDirtyFieldIcon(field);
		}
		
		refreshForm();
	}
    
    private void refreshForm() {
		if (m_formPanel != null) {
			for (Field<?> field : m_formPanel.getFields()) {
				field.setEnabled(true);
			}
            if (m_selectedModemInterfaceConfig.isGpsSupported()) {
            	m_enableGpsRadioTrue.setEnabled(true);
                m_enableGpsRadioFalse.setEnabled(true);
                m_enableGpsRadioGroup.setEnabled(true);
            } else {
            	m_enableGpsRadioTrue.setEnabled(false);
                m_enableGpsRadioFalse.setEnabled(false);
                m_enableGpsRadioGroup.setEnabled(false);
            }
		}
	}
    
    private void reset() {
		Log.debug("ModemConfigTab: reset()");
		m_enableGpsRadioGroup.setValue(m_enableGpsRadioFalse);
		m_enableGpsRadioGroup.setOriginalValue(m_enableGpsRadioGroup.getValue());
		update();
	}
}
