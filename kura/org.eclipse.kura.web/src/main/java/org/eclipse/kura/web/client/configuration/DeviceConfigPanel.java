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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.Constants;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.client.util.MessageUtils;
import org.eclipse.kura.web.client.util.UserAgentUtils;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;

import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.Style.Orientation;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.core.El;
import com.extjs.gxt.ui.client.core.XDOM;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.ComponentPlugin;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.form.Field;
import com.extjs.gxt.ui.client.widget.form.FieldSet;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.MultiField;
import com.extjs.gxt.ui.client.widget.form.NumberField;
import com.extjs.gxt.ui.client.widget.form.NumberPropertyEditor;
import com.extjs.gxt.ui.client.widget.form.Radio;
import com.extjs.gxt.ui.client.widget.form.RadioGroup;
import com.extjs.gxt.ui.client.widget.form.SimpleComboBox;
import com.extjs.gxt.ui.client.widget.form.SimpleComboValue;
import com.extjs.gxt.ui.client.widget.form.TextArea;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.form.Validator;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.FlowLayout;
import com.extjs.gxt.ui.client.widget.layout.FormData;
import com.extjs.gxt.ui.client.widget.layout.FormLayout;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;

public class DeviceConfigPanel extends LayoutContainer 
{
	private static final Messages MSGS = GWT.create(Messages.class);
 
    private GwtConfigComponent m_configComponent;
    private FormPanel          m_actionFormPanel;
    private FieldSet           m_actionFieldSet;

    private ComponentPlugin    m_infoPlugin;
    private ComponentPlugin    m_dirtyPlugin;
    
    public DeviceConfigPanel(GwtConfigComponent configComponent) {
        
    	super( new FitLayout());            
        setScrollMode(Scroll.AUTO);
        setBorders(false);
    	setId("services-config-content-wrapper");
        
    	m_configComponent = configComponent;
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
	    
	    final DeviceConfigPanel thePanel = this;
    	m_dirtyPlugin = new ComponentPlugin() {  
    		public void init(Component component) {  
    			component.addListener(Events.Change, new Listener<ComponentEvent>() {  
    				public void handleEvent(ComponentEvent be) {  
    					El elem = be.getComponent().el().findParent(".x-form-element", 7);
    					if (elem != null) {
	    					El dirtyIcon= elem.createChild("");
	    					dirtyIcon.setStyleName("x-grid3-dirty-cell");
	    					dirtyIcon.setStyleAttribute("top", "0");
	    					dirtyIcon.setStyleAttribute("position", "absolute");
	    					dirtyIcon.setSize(10, 10);
	    					dirtyIcon.show();
    					}    					
    					thePanel.fireEvent(Events.Change);
    				}  
    			});  
  	      	}  
  	    };            

	    paintConfig();
    }
    
    
    public boolean isValid() {
        List<Component> fields = m_actionFieldSet.getItems();
        for (int i=0; i<fields.size(); i++) {
        	if (fields.get(i) instanceof Field) {
        		Field<?> field = (Field<?>) fields.get(i);
        		if (!field.isValid()) {
        			return false;
        		}
        	}
        }
        return true;
    }


    public boolean isDirty() {
        List<Component> fields = m_actionFieldSet.getItems();
        for (int i=0; i<fields.size(); i++) {
        	if (fields.get(i) instanceof MultiField) {
	            MultiField<?> multiField = (MultiField<?>) fields.get(i);
	            for (Field<?> field : multiField.getAll()) {
		            if (field.isDirty()) {
		                return true;
		            }
	            }        		
        	}
        	else if (fields.get(i) instanceof Field) {
	            Field<?> field = (Field<?>) fields.get(i);
	            if (field.isDirty()) {
	                return true;
	            }
        	}
        }
        return false;
    }

    
    public GwtConfigComponent getConfiguration() {
    	return m_configComponent;
    }
    
    
    public GwtConfigComponent getUpdatedConfiguration() {
        
        List<Component> fields = m_actionFieldSet.getItems();
        for (int i=0; i<fields.size(); i++) {
        	if (fields.get(i) instanceof Field<?>) {
	            
        		Field<?> field = (Field<?>) fields.get(i);
        		String fieldName = field.getItemId();
        		GwtConfigParameter param = m_configComponent.getParameter(fieldName);
        		if (param == null) {
        			System.err.println(field);
        		}
        		if (!(field instanceof MultiField) || (field instanceof RadioGroup)) {
        			// get the updated values for the single field
        			String value = getUpdatedFieldConfiguration(param, field);
        			param.setValue(value);
        		}
        		else {
        			
        			// iterate over the subfields and extract each value
        			List<String> multiFieldValues = new ArrayList<String>();
        			MultiField<?>   multiField = (MultiField<?>) field;
        			List<Field<?>> childFields = multiField.getAll(); 
        			for (int j=0; j<childFields.size(); j++) {
        				
                		Field<?> childField = (Field<?>) childFields.get(j);
        				String value = getUpdatedFieldConfiguration(param, childField);
        				if (value != null) {
        					multiFieldValues.add(value);
        				}
        			}
        			param.setValues(multiFieldValues.toArray( new String[]{}));        			
        		}
        	}
        }
        return m_configComponent;
    }


	private String getUpdatedFieldConfiguration(GwtConfigParameter param, Field<?> field) 
	{
		Map<String, String> options = param.getOptions();   	
		if (options != null && options.size() > 0) {	        		
		    @SuppressWarnings("unchecked")
		    SimpleComboValue<String> scv = (SimpleComboValue<String>) field.getValue();
		    return options.get(scv.getValue());
		}
		else {
		    switch (param.getType()) {
		    case LONG:
		    	NumberField longField = (NumberField) field;
		        Number longNumber = longField.getValue();
		        if(longNumber != null) {
		        	return String.valueOf(longNumber.longValue());
		        } else {
		        	return null;
		        }
		        
		    case DOUBLE:
		    	NumberField doubleField = (NumberField) field;
		        Number doubleNumber = doubleField.getValue();
		        if(doubleNumber != null) {
		        	return String.valueOf(doubleNumber.doubleValue());
		        } else {
		        	return null;
		        }
		        
		    case FLOAT:
		    	NumberField floatField = (NumberField) field;
		        Number floatNumber = floatField.getValue();
		        if(floatNumber != null) {
		        	return String.valueOf(floatNumber.floatValue());
		        } else {
		        	return null;
		        }
		        
		    case INTEGER:
		    	NumberField integerField = (NumberField) field;
		        Number integerNumber = integerField.getValue();
		        if(integerNumber != null) {
		        	return String.valueOf(integerNumber.intValue());
		        } else {
		        	return null;
		        }
		        
		    case SHORT:
		    	NumberField shortField = (NumberField) field;
		        Number shortNumber = shortField.getValue();
		        if(shortNumber != null) {
		        	return String.valueOf(shortNumber.shortValue());
		        } else {
		        	return null;
		        }
		        
		    case BYTE:
		        NumberField byteField = (NumberField) field;
		        Number byteNumber = byteField.getValue();
		        if(byteNumber != null) {
		        	return String.valueOf(byteNumber.byteValue());
		        } else {
		        	return null;
		        }
		        	
		    case BOOLEAN:
		    	RadioGroup radioGroup = (RadioGroup) field;
		    	Radio radio = radioGroup.getValue();
		    	String booleanValue = radio.getItemId(); 
		    	return booleanValue;
		    	
		    case PASSWORD:
			case CHAR:						
		    case STRING:
		    	return (String) field.getValue();
		        
		    default:
		        break;
		    }
		}
		return null;
	}

    
    private void paintConfig() {
        
        LayoutContainer lcAction = new LayoutContainer();
        lcAction.setLayout(new BorderLayout());
        lcAction.setBorders(true);
        lcAction.setSize(475, -1);
        add(lcAction);
        
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
    				NodeList<Element> nl = m_actionFormPanel.getElement().getElementsByTagName("form");
    				if (nl.getLength() > 0) {
    					Element elemForm = nl.getItem(0);
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
        if (m_configComponent.getComponentDescription() != null &&
            m_configComponent.getComponentDescription().trim().length() > 0) {
        	m_actionFieldSet.addText(m_configComponent.getComponentDescription());
        }
        
        FormLayout layout = new FormLayout();
        layout.setLabelWidth(Constants.LABEL_WIDTH_CONFIG_FORM);
        m_actionFieldSet.setLayout(layout);

        Field<?> field = null;
        for (GwtConfigParameter param : m_configComponent.getParameters()) {
        	
        	if (param.getCardinality() == 0 || param.getCardinality() == 1 || param.getCardinality() == -1) {
        		field = paintConfigParameter(param);
        	}
        	else {
        		field = paintMultiFieldConfigParameter(param);
        	}
            m_actionFieldSet.add(field, formData);
        }
        
        if (!UserAgentUtils.isIE()) {
            m_actionFormPanel.add(m_actionFieldSet, formData);
            lcAction.add(m_actionFormPanel, centerData);
        }
        else {
            lcAction.add(m_actionFieldSet, centerData);
        }
    }
    
    
    private Field<?> paintMultiFieldConfigParameter(GwtConfigParameter param)
    {
    	@SuppressWarnings("rawtypes")
		MultiField<?> multiField = new MultiField();
        multiField.setName(param.getId());
        multiField.setItemId(param.getId());
        multiField.setFieldLabel(param.getName());
        multiField.addPlugin(m_dirtyPlugin);
        multiField.setOrientation(Orientation.VERTICAL);
        if (param.isRequired()) {
            multiField.setFieldLabel("* "+param.getName());
        }
        if (param.getDescription() != null && param.getDescription().length() > 0) {
        	multiField.addPlugin(m_infoPlugin);
        	multiField.setData("text", param.getDescription());  
        }        
        if (param.getMin() != null && param.getMin().equals(param.getMax())) {
        	multiField.setReadOnly(true);
        	multiField.setEnabled(false);
        }
        
        Field<?>  field = null;
        String    value = null;
        String[] values = param.getValues();
        for (int i=0; i<Math.min(param.getCardinality(), 10); i++) {

        	// temporary set the param value to the current one in the array
        	// use a value from the one passed in if we have it.
        	value = null;
        	if (values != null && i < values.length) {
        		value = values[i];
        	}        	
        	param.setValue(value);
        	field = paintConfigParameter(param);
        	multiField.add(field);
        }

        // restore a null current value
    	param.setValue(null);
    	
        return multiField;
    }
    
    
    private Field<?> paintConfigParameter(GwtConfigParameter param) 
    {
    	Field<?> field = null;
    	Map<String, String> options = param.getOptions(); 
    	if (options != null && options.size() > 0) {
    		field = paintChoiceActionParameter(param);
    	}
    	else {
    		String minValue = param.getMin();
    		String maxValue = param.getMax();
	    	switch (param.getType()) {
	            case LONG:
	            	field = paintNumberConfigParameter(param, new LongValidator(minValue, maxValue));
	            	break;
	            	
	            case DOUBLE:
	            	field = paintNumberConfigParameter(param, new DoubleValidator(minValue, maxValue));
	            	break;
	            	
	            case FLOAT:
	            	field = paintNumberConfigParameter(param, new FloatValidator(minValue, maxValue));
	            	break;
	            	
	            case INTEGER:
	            	field = paintNumberConfigParameter(param, new IntegerValidator(minValue, maxValue));
	            	break;
	                	
				case SHORT:
					field = paintNumberConfigParameter(param, new ShortValidator(minValue, maxValue));
	            	break;

				case BYTE:
					field = paintNumberConfigParameter(param, new ByteValidator(minValue, maxValue));
	            	break;

				case BOOLEAN:
					field = paintBooleanConfigParameter(param);
	            	break;
								
	            case PASSWORD:
	            	field = paintPasswordConfigParameter(param);
	            	break;

				case CHAR:
					field = paintTextConfigParameter(param, new CharValidator(minValue, maxValue));
	            	break;

				default:
				case STRING:
					field = paintTextConfigParameter(param, new StringValidator(minValue, maxValue));
	            	break;
	    	}
    	}
    	
    	field.setName(param.getId());
    	field.setItemId(param.getId());
    	field.getElement().setAttribute("autocomplete", "off");
    	return field;
    }


    private Field<?> paintTextConfigParameter(GwtConfigParameter param, Validator validator) {    
    	
    	// FIXME: in some case we might need a TextArea for multi-line String values.
    	// The trick is to put a tag in the parameter descriptor which we use
    	// to render the TextField widget.
    	// The tag is a sequence of at least five Zero Width Space characters
    	// which are usually invisible.
    	String description = param.getDescription();
    	TextField<String> field;
    	//if (description.endsWith("\u200B\u200B\u200B\u200B\u200B")) {
    	if (description.contains("\u200B\u200B\u200B\u200B\u200B")) {
    		field = new TextArea();
    		field.setHeight(120);
    	} else {
    		field = new TextField<String>();
    	}
    	
        //TextField<String> field = new TextField<String>();
        field.setName(param.getId());
        field.setValue((String) param.getValue());
        field.setAllowBlank(true);
        field.setFieldLabel(param.getName());
        field.addPlugin(m_dirtyPlugin);
        if (param.isRequired()) {
            field.setAllowBlank(false);
            field.setFieldLabel("* "+param.getName());
        }        
        if (param.getDescription() != null && param.getDescription().length() > 0) {
        	field.addPlugin(m_infoPlugin);
        	field.setData("text", param.getDescription());  
        }        
//        if (param.getDescription() != null) {
//            field.setToolTip(param.getDescription());
//        }
        if (param.getMin() != null && param.getMin().equals(param.getMax())) {
        	field.setReadOnly(true);
        	field.setEnabled(false);
        }
        
        if (param.getValue() != null) {
            field.setValue((String) param.getValue());
            field.setOriginalValue((String) param.getValue());
        }        
        if (validator != null && validator instanceof CharValidator) {
        	field.setMaxLength(1);
        	field.setValidator((CharValidator)validator);
        }   
        if (validator != null && validator instanceof StringValidator) {
        	field.setValidator((StringValidator)validator);
        }   
        return field;
    }
    
    
    private Field<?> paintPasswordConfigParameter(GwtConfigParameter param) {           
        TextField<String> field = new TextField<String>();
        field.setName(param.getId());
        field.setValue((String) param.getValue());
        field.setAllowBlank(true);
        field.setPassword(true);
        field.setFieldLabel(param.getName());
        field.addPlugin(m_dirtyPlugin);
        if (param.isRequired()) {
            field.setAllowBlank(false);
            field.setFieldLabel("* "+param.getName());
        }
        if (param.getDescription() != null && param.getDescription().length() > 0) {
        	field.addPlugin(m_infoPlugin);
        	field.setData("text", param.getDescription());  
        }        
//        if (param.getDescription() != null) {
//            field.setToolTip(param.getDescription());
//        }
        if (param.getMin() != null && param.getMin().equals(param.getMax())) {
        	field.setReadOnly(true);
        	field.setEnabled(false);
        }
        
        if (param.getValue() != null) {
            field.setValue((String) param.getValue());
            field.setOriginalValue((String) param.getValue());
        }
        return field;
    }

    
    private Field<?> paintNumberConfigParameter(GwtConfigParameter param, Validator validator) {
        NumberField field = new NumberField();
        field.setName(param.getId());
        field.setAllowBlank(true);
        field.setFieldLabel(param.getName());
        field.addPlugin(m_dirtyPlugin);
        if (validator != null) {
        	field.setValidator(validator);
        }
        if (param.isRequired()) {
            field.setAllowBlank(false);
            field.setFieldLabel("* "+param.getName());
        }
        if (param.getDescription() != null && param.getDescription().length() > 0) {
        	field.addPlugin(m_infoPlugin);
        	field.setData("text", param.getDescription());  
        }        
//        if (param.getDescription() != null) {
//            field.setToolTip(param.getDescription());
//        }
        if (param.getMin() != null && param.getMin().equals(param.getMax())) {
        	field.setReadOnly(true);
        	field.setEnabled(false);
        }

        switch (param.getType()) {
        case LONG:
            field.setPropertyEditorType(Long.class);
            if (param.getValue() != null) {
                field.setValue(Long.parseLong(param.getValue()));
                field.setOriginalValue(Long.parseLong(param.getValue()));
            }
            break;
        case DOUBLE:
            field.setPropertyEditorType(Double.class);
            if (param.getValue() != null) {
                field.setValue(Double.parseDouble(param.getValue()));
                field.setOriginalValue(Double.parseDouble(param.getValue()));
            }
            break;
        case FLOAT:
            field.setPropertyEditorType(Float.class);
            if (param.getValue() != null) {
                field.setValue(Float.parseFloat(param.getValue()));
                field.setOriginalValue(Float.parseFloat(param.getValue()));
            }
            break;
        case SHORT:
            field.setPropertyEditorType(Short.class);
            if (param.getValue() != null) {
                field.setValue(Short.parseShort(param.getValue()));
                field.setOriginalValue(Short.parseShort(param.getValue()));
            }
            break;
        case BYTE:
            field.setPropertyEditor( new BytePropertyEditor());
            if (param.getValue() != null) {
                field.setValue(Byte.parseByte(param.getValue()));
                field.setOriginalValue(Byte.parseByte(param.getValue()));
            }
            break;
		default:
        case INTEGER:
            field.setPropertyEditorType(Integer.class);
            if (param.getValue() != null) {
                field.setValue(Integer.parseInt(param.getValue()));
                field.setOriginalValue(Integer.parseInt(param.getValue()));
            }
            break;
        }
        return field;
    }

    
    private Field<?> paintChoiceActionParameter(GwtConfigParameter param) {
        SimpleComboBox<String> field = new SimpleComboBox<String>();
        field.setName(param.getId());
        field.setEditable(false);
        field.setAllowBlank(true);
        field.setTriggerAction(TriggerAction.ALL);
        field.setFieldLabel(param.getName());
        field.setEditable(false);
        field.addPlugin(m_dirtyPlugin);
        if (param.isRequired()) {
            field.setAllowBlank(false);
            field.setFieldLabel("* "+param.getName());
        }
        if (param.getDescription() != null && param.getDescription().length() > 0) {
        	field.addPlugin(m_infoPlugin);
        	field.setData("text", param.getDescription());  
        }        

        if (param.getMin() != null && param.getMin().equals(param.getMax())) {
        	field.setReadOnly(true);
        	field.setEnabled(false);
        }
        
        Map<String, String> oMap = param.getOptions();
        Iterator<String> it = oMap.keySet().iterator();
        while (it.hasNext()) {
        	field.add(it.next());
        }

        if (param.getDefault() != null) {
            //field.setSimpleValue((String) param.getDefault());
        	field.setSimpleValue(getKeyFromValue(oMap, (String)param.getDefault()));
        }
        if (param.getValue() != null) {
            //field.setSimpleValue((String) param.getValue());
        	field.setSimpleValue(getKeyFromValue(oMap, (String)param.getValue()));
        }
        return field;
    }
    
    private String getKeyFromValue (Map<String, String> m, String value) {
    	String key = "";
    	Iterator<Map.Entry<String, String>> it = m.entrySet().iterator();
    	
    	while (it.hasNext()) {
    		Map.Entry<String, String> es = it.next();
    		if (es.getValue().equals(value)) {
    			key = es.getKey();
    		}
    	}
    	return key;
    }
    
    
    private Field<?> paintBooleanConfigParameter(GwtConfigParameter param) {    

    	Radio radioTrue = new Radio();  
        radioTrue.setBoxLabel(MSGS.trueLabel());
        radioTrue.setItemId("true");
      
        Radio radioFalse = new Radio();  
        radioFalse.setBoxLabel(MSGS.falseLabel());  
        radioFalse.setItemId("false");
      
        RadioGroup radioGroup = new RadioGroup();  
        radioGroup.setName(param.getId());  
        radioGroup.setItemId(param.getId());
        radioGroup.setFieldLabel(param.getName());  
        radioGroup.add(radioTrue);  
        radioGroup.add(radioFalse);
        if (param.isRequired()) {
            radioGroup.setFieldLabel("* "+param.getName());
        }
        
        radioGroup.addPlugin(m_dirtyPlugin);
        if (param.getDescription() != null && param.getDescription().length() > 0) {
        	radioGroup.addPlugin(m_infoPlugin);
        	radioGroup.setData("text", param.getDescription());  
        }        

        boolean bool = Boolean.parseBoolean(param.getValue());
        if (bool) {
        	radioTrue.setValue(true);
        	radioGroup.setOriginalValue(radioTrue);
        }
        else {
        	radioFalse.setValue(true);
        	radioGroup.setOriginalValue(radioFalse);
        }
        
        return radioGroup;
    }

    
    private static class IntegerValidator implements Validator
    {
    	private Integer m_minValue;
    	private Integer m_maxValue;
    	
    	public IntegerValidator(String minValue, String maxValue)
    	{
			m_minValue = null;
			if (minValue != null) {
	    		try {
	    			m_minValue = Integer.valueOf(minValue);
	    		}
	    		catch (NumberFormatException nfe) {
	    			FailureHandler.handle(nfe);
	    		}
			}			
			m_maxValue = null;
			if (maxValue != null) {
	    		try {
	    			m_maxValue = Integer.valueOf(maxValue);
	    		}
	    		catch (NumberFormatException nfe) {
	    			FailureHandler.handle(nfe);
	    		}
			}
    	}
    	
		public String validate(Field<?> field, String value) {
			Integer intValue = null;
			try {
				intValue = Integer.valueOf(value);
			}
    		catch (NumberFormatException nfe) {
    			return nfe.getMessage();
    		}
			if (intValue != null) {				
				if (m_minValue != null) {
					if (intValue.intValue() < m_minValue.intValue()) {
						return MessageUtils.get("configMinValue", m_minValue.intValue());
					}
				}
				if (m_maxValue != null) {
					if (intValue.intValue() > m_maxValue.intValue()) {
						return MessageUtils.get("configMaxValue", m_maxValue.intValue());
					}
				}
			}
			return null;
		}    	
    }
   

    private static class LongValidator implements Validator
    {
    	private Long m_minValue;
    	private Long m_maxValue;
    	
    	public LongValidator(String minValue, String maxValue)
    	{
			m_minValue = null;
			if (minValue != null) {
	    		try {
	    			m_minValue = Long.valueOf(minValue);
	    		}
	    		catch (NumberFormatException nfe) {
	    			FailureHandler.handle(nfe);
	    		}
			}			
			m_maxValue = null;
			if (maxValue != null) {
	    		try {
	    			m_maxValue = Long.valueOf(maxValue);
	    		}
	    		catch (NumberFormatException nfe) {
	    			FailureHandler.handle(nfe);
	    		}
			}
    	}
    	
		public String validate(Field<?> field, String value) {
			Long longValue = null;
			try {
				longValue = Long.valueOf(value);
			}
    		catch (NumberFormatException nfe) {
    			return nfe.getMessage();
    		}
			if (longValue != null) {				
				if (m_minValue != null) {
					if (longValue.longValue() < m_minValue.longValue()) {
						return MessageUtils.get("configMinValue", m_minValue.longValue());
					}
				}
				if (m_maxValue != null) {
					if (longValue.longValue() > m_maxValue.longValue()) {
						return MessageUtils.get("configMaxValue", m_maxValue.longValue());
					}
				}
			}
			return null;
		}    	
    }
    
    
    private static class DoubleValidator implements Validator
    {
    	private Double m_minValue;
    	private Double m_maxValue;
    	
    	public DoubleValidator(String minValue, String maxValue)
    	{
			m_minValue = null;
			if (minValue != null) {
	    		try {
	    			m_minValue = Double.valueOf(minValue);
	    		}
	    		catch (NumberFormatException nfe) {
	    			FailureHandler.handle(nfe);
	    		}
			}			
			m_maxValue = null;
			if (maxValue != null) {
	    		try {
	    			m_maxValue = Double.valueOf(maxValue);
	    		}
	    		catch (NumberFormatException nfe) {
	    			FailureHandler.handle(nfe);
	    		}
			}
    	}
    	

		public String validate(Field<?> field, String value) {
			Double doubleValue = null;
			try {
				doubleValue = Double.valueOf(value);
			}
    		catch (NumberFormatException nfe) {
    			return nfe.getMessage();
    		}
			if (doubleValue != null) {				
				if (m_minValue != null) {
					if (doubleValue.doubleValue() < m_minValue.doubleValue()) {
						return MessageUtils.get("configMinValue", m_minValue.doubleValue());
					}
				}
				if (m_maxValue != null) {
					if (doubleValue.doubleValue() > m_maxValue.doubleValue()) {
						return MessageUtils.get("configMaxValue", m_maxValue.doubleValue());
					}
				}
			}
			return null;
		}    	
    }

    
    private static class FloatValidator implements Validator
    {
    	private Float m_minValue;
    	private Float m_maxValue;
    	
    	public FloatValidator(String minValue, String maxValue)
    	{
			m_minValue = null;
			if (minValue != null) {
	    		try {
	    			m_minValue = Float.valueOf(minValue);
	    		}
	    		catch (NumberFormatException nfe) {
	    			FailureHandler.handle(nfe);
	    		}
			}			
			m_maxValue = null;
			if (maxValue != null) {
	    		try {
	    			m_maxValue = Float.valueOf(maxValue);
	    		}
	    		catch (NumberFormatException nfe) {
	    			FailureHandler.handle(nfe);
	    		}
			}
    	}
    	
		public String validate(Field<?> field, String value) {
			Float floatValue = null;
			try {
				floatValue = Float.valueOf(value);
			}
    		catch (NumberFormatException nfe) {
    			return nfe.getMessage();
    		}
			if (floatValue != null) {				
				if (m_minValue != null) {
					if (floatValue.floatValue() < m_minValue.floatValue()) {
						return MessageUtils.get("configMinValue", m_minValue.floatValue());
					}
				}
				if (m_maxValue != null) {
					if (floatValue.floatValue() > m_maxValue.floatValue()) {
						return MessageUtils.get("configMaxValue", m_maxValue.floatValue());
					}
				}
			}
			return null;
		}    	
    }

    
    private static class ShortValidator implements Validator
    {
    	private Short m_minValue;
    	private Short m_maxValue;
    	
    	public ShortValidator(String minValue, String maxValue)
    	{
			m_minValue = null;
			if (minValue != null) {
	    		try {
	    			m_minValue = Short.valueOf(minValue);
	    		}
	    		catch (NumberFormatException nfe) {
	    			FailureHandler.handle(nfe);
	    		}
			}			
			m_maxValue = null;
			if (maxValue != null) {
	    		try {
	    			m_maxValue = Short.valueOf(maxValue);
	    		}
	    		catch (NumberFormatException nfe) {
	    			FailureHandler.handle(nfe);
	    		}
			}
    	}
    	
		public String validate(Field<?> field, String value) {
			Short shortValue = null;
			try {
				shortValue = Short.valueOf(value);
			}
    		catch (NumberFormatException nfe) {
    			return nfe.getMessage();
    		}
			if (shortValue != null) {				
				if (m_minValue != null) {
					if (shortValue.shortValue() < m_minValue.shortValue()) {
						return MessageUtils.get("configMinValue", m_minValue.shortValue());
					}
				}
				if (m_maxValue != null) {
					if (shortValue.shortValue() > m_maxValue.shortValue()) {
						return MessageUtils.get("configMaxValue", m_maxValue.shortValue());
					}
				}
			}
			return null;
		}    	
    }


    private static class ByteValidator implements Validator
    {
    	private Byte m_minValue;
    	private Byte m_maxValue;
    	
    	public ByteValidator(String minValue, String maxValue)
    	{
			m_minValue = null;
			if (minValue != null) {
	    		try {
	    			m_minValue = Byte.valueOf(minValue);
	    		}
	    		catch (NumberFormatException nfe) {
	    			FailureHandler.handle(nfe);
	    		}
			}			
			m_maxValue = null;
			if (maxValue != null) {
	    		try {
	    			m_maxValue = Byte.valueOf(maxValue);
	    		}
	    		catch (NumberFormatException nfe) {
	    			FailureHandler.handle(nfe);
	    		}
			}
    	}
    	
		public String validate(Field<?> field, String value) {
			Byte byteValue = null;
			try {
				byteValue = Byte.valueOf(value);
			}
    		catch (NumberFormatException nfe) {
    			return nfe.getMessage();
    		}
			if (byteValue != null) {				
				if (m_minValue != null) {
					if (byteValue.byteValue() < m_minValue.byteValue()) {
						return MessageUtils.get("configMinValue", m_minValue.byteValue());
					}
				}
				if (m_maxValue != null) {
					if (byteValue.byteValue() > m_maxValue.byteValue()) {
						return MessageUtils.get("configMaxValue", m_maxValue.byteValue());
					}
				}
			}
			return null;
		}    	
    }



    private static class CharValidator implements Validator
    {
    	private Character m_minValue;
    	private Character m_maxValue;
    	
    	public CharValidator(String minValue, String maxValue)
    	{
			m_minValue = null;
			if (minValue != null) {
	    		try {
	    			m_minValue = Character.valueOf(minValue.charAt(0));
	    		}
	    		catch (NumberFormatException nfe) {
	    			FailureHandler.handle(nfe);
	    		}
			}			
			m_maxValue = null;
			if (maxValue != null) {
	    		try {
	    			m_maxValue = Character.valueOf(maxValue.charAt(0));
	    		}
	    		catch (NumberFormatException nfe) {
	    			FailureHandler.handle(nfe);
	    		}
			}
    	}
    	
		public String validate(Field<?> field, String value) {
			Character charValue = null;
			try {
				charValue = Character.valueOf(value.charAt(0));
			}
    		catch (NumberFormatException nfe) {
    			return nfe.getMessage();
    		}
			if (charValue != null) {				
				if (m_minValue != null) {
					if (charValue.charValue() < m_minValue.charValue()) {
						return MessageUtils.get("configMinValue", m_minValue.charValue());
					}
				}
				if (m_maxValue != null) {
					if (charValue.charValue() > m_maxValue.charValue()) {
						return MessageUtils.get("configMaxValue", m_maxValue.charValue());
					}
				}
			}
			return null;
		}    	
    }


    private static class StringValidator implements Validator {

    	private int m_minValue = 0;
    	private int m_maxValue = 255;
    	
    	public StringValidator(String minValue, String maxValue) {
    		if (minValue != null) {
    			try {
    				m_minValue = Integer.parseInt(minValue);
    			}
    			catch (NumberFormatException nfe) {
    				FailureHandler.handle(nfe);
    			}
    		}
    		if (maxValue != null) {
    			try {
    				m_maxValue = Integer.parseInt(maxValue);
    			}
    			catch (NumberFormatException nfe) {
    				FailureHandler.handle(nfe);
    			}
    		}
    	}
    	
		public String validate(Field<?> field, String value) {
			if (value.length() > m_maxValue) {
				return MessageUtils.get("configMaxValue", (m_maxValue + 1));
			}
			if (value.length() < m_minValue) {
				return MessageUtils.get("configMinValue", m_minValue);
			}
			return null;
		}
    	
    }

    private static class BytePropertyEditor extends NumberPropertyEditor
    {
		@Override
		public String getStringValue(Number value) {
			return value.toString();
		}

		@Override
		public Number convertStringValue(String value) {
			return Byte.valueOf(value);
		}
    }
}
