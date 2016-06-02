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
/*
 * Render the Content in the Main Panel corressponding to Service (GwtBSConfigComponent) selected in the Services Panel
 * 
 * Fields are rendered based on their type (Password(Input), Choice(Dropboxes) etc. with Text fields rendered
 * for both numeric and other textual field with validate() checking if value in numeric fields is numeric
 */
package org.eclipse.kura.web.client.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.client.util.MessageUtils;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;
import org.eclipse.kura.web.shared.model.GwtConfigParameter.GwtConfigParameterType;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtComponentService;
import org.eclipse.kura.web.shared.service.GwtComponentServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.ButtonGroup;
import org.gwtbootstrap3.client.ui.FieldSet;
import org.gwtbootstrap3.client.ui.Form;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.HelpBlock;
import org.gwtbootstrap3.client.ui.InlineRadio;
import org.gwtbootstrap3.client.ui.Input;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.ModalHeader;
import org.gwtbootstrap3.client.ui.NavPills;
import org.gwtbootstrap3.client.ui.PanelBody;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.InputType;
import org.gwtbootstrap3.client.ui.constants.ValidationState;
import org.gwtbootstrap3.client.ui.gwt.FlowPanel;
import org.gwtbootstrap3.client.ui.html.Span;
import org.gwtbootstrap3.client.ui.html.Text;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class ServicesUi extends Composite {

	private static final String CONFIG_MAX_VALUE = "configMaxValue";
	private static final String CONFIG_MIN_VALUE = "configMinValue";
	private static ServicesUiUiBinder uiBinder = GWT.create(ServicesUiUiBinder.class);
	private static final Logger logger = Logger.getLogger(ServicesUi.class.getSimpleName());
	private static Logger errorLogger = Logger.getLogger("ErrorLogger");

	interface ServicesUiUiBinder extends UiBinder<Widget, ServicesUi> {
	}

	private static final Messages MSGS = GWT.create(Messages.class);
	private final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);
	private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);

	HashMap<String, Boolean> valid = new HashMap<String, Boolean>();

	GwtConfigComponent	m_configurableComponent;
	private boolean		dirty, initialized;

	NavPills 		menu;
	PanelBody 		content;
	AnchorListItem 	service;
	TextBox 		validated;
	FormGroup 		validatedGroup;
	EntryClassUi 	entryClass;
	Modal 			modal;

	@UiField
	Button apply, reset;
	@UiField
	FieldSet fields;
	@UiField
	Form form;

	@UiField
	Modal incompleteFieldsModal;
	@UiField
	Alert incompleteFields;
	@UiField
	Text incompleteFieldsText;

	//
	// Public methods
	//
	public ServicesUi(final GwtConfigComponent addedItem, EntryClassUi entryClassUi) {
		initWidget(uiBinder.createAndBindUi(this));
		initialized = false;
		entryClass = entryClassUi;
		m_configurableComponent = addedItem;
		fields.clear();
		setOriginalValues(m_configurableComponent);

		apply.setText(MSGS.apply());
		apply.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				apply();
			}
		});

		reset.setText(MSGS.reset());
		reset.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				reset();
			}
		});
		renderForm();
		initInvalidDataModal();

		setDirty(false);
		apply.setEnabled(false);
		reset.setEnabled(false);
	}

	public void setDirty(boolean flag) {
		dirty = flag;
		if (dirty && initialized) {
			apply.setEnabled(true);
			reset.setEnabled(true);
		}
	}

	public boolean isDirty() {
		return dirty;
	}

	public void reset() {
		if (isDirty()) {
			//Modal
			modal = new Modal();

			ModalHeader header = new ModalHeader();
			header.setTitle(MSGS.confirm());
			modal.add(header);

			ModalBody body = new ModalBody();			
			body.add(new Span(MSGS.deviceConfigDirty()));
			modal.add(body);

			ModalFooter footer = new ModalFooter();
			ButtonGroup group= new ButtonGroup();
			Button yes = new Button();
			yes.setText(MSGS.yesButton());
			yes.addClickHandler(new ClickHandler(){
				@Override
				public void onClick(ClickEvent event) {
					modal.hide();	
					renderForm();
					apply.setEnabled(false);
					reset.setEnabled(false);
					setDirty(false);
					entryClass.initServicesTree();
				}});
			group.add(yes);
			Button no = new Button();
			no.setText(MSGS.noButton());
			no.addClickHandler(new ClickHandler(){
				@Override
				public void onClick(ClickEvent event) {
					modal.hide();	
				}});						
			group.add(no);
			footer.add(group);
			modal.add(footer);
			modal.show();							
		}//end is dirty	
	}

	//TODO: Separate render methods for each type (ex: Boolean, String, Password, etc.). See latest org.eclipse.kura.web code.
	//Iterates through all GwtConfigParameter in the selected GwtConfigComponent
	public void renderForm() {
		fields.clear();
		for (GwtConfigParameter param : m_configurableComponent.getParameters()) {
			if (param.getCardinality() == 0 || 
					param.getCardinality() == 1 || 
					param.getCardinality() == -1) {
				FormGroup formGroup = new FormGroup();
				renderConfigParameter(param, true, formGroup);				
			} else {
				renderMultiFieldConfigParameter(param);
			}
		}
		initialized = true;
	}

	public GwtConfigComponent getConfiguration() {
		return m_configurableComponent;
	}
	

	//
	// Private methods
	//
	private void apply() {		
		if (isValid()) {
			if(isDirty()){
				//TODO ask for confirmation first
				modal = new Modal();

				ModalHeader header = new ModalHeader();
				header.setTitle(MSGS.confirm());
				modal.add(header);

				ModalBody body = new ModalBody();
				body.add(new Span(MSGS.deviceConfigConfirmation(m_configurableComponent.getComponentName())));
				modal.add(body);

				ModalFooter footer = new ModalFooter();
				ButtonGroup group= new ButtonGroup();
				Button yes = new Button();
				yes.setText(MSGS.yesButton());
				yes.addClickHandler(new ClickHandler(){
					@Override
					public void onClick(ClickEvent event) {
						EntryClassUi.showWaitModal();
						getUpdatedConfiguration();
						gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {

							@Override
							public void onFailure(Throwable ex) {
								EntryClassUi.hideWaitModal();
								FailureHandler.handle(ex);
							}

							@Override
							public void onSuccess(GwtXSRFToken token) {
								gwtComponentService.updateComponentConfiguration(token, m_configurableComponent, new AsyncCallback<Void>(){
									@Override
									public void onFailure(Throwable caught) {
										EntryClassUi.hideWaitModal();
										errorLogger.log(Level.SEVERE, caught.getLocalizedMessage());
									}

									@Override
									public void onSuccess(Void result) {
										modal.hide();	
										logger.info(MSGS.info()+": " + MSGS.deviceConfigApplied());
										apply.setEnabled(false);
										reset.setEnabled(false);
										setDirty(false);
										entryClass.initServicesTree();
										EntryClassUi.hideWaitModal();
									}});

							}
						});
					}});
				group.add(yes);
				Button no = new Button();
				no.setText(MSGS.noButton());
				no.addClickHandler(new ClickHandler(){
					@Override
					public void onClick(ClickEvent event) {
						modal.hide();
					}});						
				group.add(no);
				footer.add(group);
				modal.add(footer);
				modal.show();


				//----

			}//end isDirty()
		}else{
			errorLogger.log(Level.SEVERE, "Device configuration error!");
			incompleteFieldsModal.show();
		}//end else isValid	
	}

	// Get updated parameters
	private GwtConfigComponent getUpdatedConfiguration() {
		Iterator<Widget> it = fields.iterator();
		while (it.hasNext()) {
			Widget w = it.next();
			if (w instanceof FormGroup) {
				FormGroup fg = (FormGroup) w;
				fillUpdatedConfiguration(fg);
			}
		}
		return m_configurableComponent;
	}

	private void fillUpdatedConfiguration(FormGroup fg) {
		GwtConfigParameter param = new GwtConfigParameter();
		List<String> multiFieldValues = new ArrayList<String>();
		int fgwCount = fg.getWidgetCount();
		for (int i = 0; i < fgwCount; i++) {
			if (fg.getWidget(i) instanceof FormLabel) {
				String id = ((FormLabel) fg.getWidget(i)).getText();
				param = m_configurableComponent.getParameter(id.trim().replaceAll("\\*$", ""));
			} else if (fg.getWidget(i) instanceof ListBox || 
					fg.getWidget(i) instanceof Input ||
					fg.getWidget(i) instanceof TextBox) {

				String value = getUpdatedFieldConfiguration(param, fg.getWidget(i));
				if (value == null) {
					continue;
				}
				if (param.getCardinality() == 0 || 
						param.getCardinality() == 1 || 
						param.getCardinality() == -1) {
					param.setValue(value);
				} else {
					multiFieldValues.add(value);
				}
			}
		}
		if (!multiFieldValues.isEmpty()) {
			param.setValues(multiFieldValues.toArray( new String[]{}));
		}
	}

	private String getUpdatedFieldConfiguration(GwtConfigParameter param, Widget wg) {
		Map<String, String> options = param.getOptions();   	
		if (options != null && options.size() > 0) {
			Map<String, String> oMap = param.getOptions();
			if (wg instanceof ListBox) {
				return oMap.get(((ListBox) wg).getSelectedItemText());
			} else {
				return null;
			}
		}
		else {
			switch (param.getType()) {
			case BOOLEAN:
				return param.getValue();
			case LONG:
			case DOUBLE:
			case FLOAT:
			case SHORT:
			case BYTE:
			case INTEGER:
			case CHAR:
			case STRING:
				TextBox tb = (TextBox) wg;
				String value = tb.getText();
				if (value != null && value.trim().length() > 0) {
					return value;
				} else {
					return null;
				}
			case PASSWORD:
				if (wg instanceof Input) {
					return ((Input) wg).getValue();
				} else {
					return null;
				}
			default:
				break;
			}
		}
		return null;
	}

	private void renderMultiFieldConfigParameter(GwtConfigParameter mParam) {
		String value = null;
		String[] values = mParam.getValues();
		boolean isFirstInstance= true;
		FormGroup formGroup = new FormGroup();
		for (int i = 0; i < Math.min(mParam.getCardinality(), 10); i++) {
			// temporary set the param value to the current one in the array
			// use a value from the one passed in if we have it.
			value = null;
			if (values != null && i < values.length) {
				value = values[i];
			}
			mParam.setValue(value);
			renderConfigParameter(mParam, isFirstInstance, formGroup);
			if (isFirstInstance) {
				isFirstInstance= false;
			}
		}
		// restore a null current value
		mParam.setValue(null);
	}

	//passes the parameter to the corresponding method depending on the type of field to be rendered
	private void renderConfigParameter(GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
		Map<String, String> options = param.getOptions();
		if (options != null && options.size() > 0) {
			renderChoiceField(param, isFirstInstance, formGroup);
		} else if (param.getType().equals(GwtConfigParameterType.BOOLEAN)) {
			renderBooleanField(param, isFirstInstance, formGroup);
		} else if (param.getType().equals(GwtConfigParameterType.PASSWORD)) {
			renderPasswordField(param, isFirstInstance, formGroup);
		} else {
			renderTextField(param, isFirstInstance, formGroup);
		}
	}

	// Field Render based on Type
	private void renderTextField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {

		valid.put(param.getName(), true);

		if (isFirstInstance) {
			FormLabel formLabel = new FormLabel();
			if (param.isRequired()) {
				formLabel.setText(param.getName() + "*");
			} else {
				formLabel.setText(param.getName());
			}
			formGroup.add(formLabel);

			HelpBlock tooltip = new HelpBlock();
			tooltip.setText(param.getDescription());
			formGroup.add(tooltip);
		}

		TextBox textBox = new TextBox();
		if (param.getDescription().contains("\u200B\u200B\u200B\u200B\u200B")) {
			textBox.setHeight("120px");
		}

		String formattedValue= new String();
		switch (param.getType()) {  //TODO: Probably this formatting step has no sense. But it seems that, if not in debug, all the browsers are able to display the double value as expected
		case LONG:
			if (param.getValue() != null) {
				formattedValue= String.valueOf(Long.parseLong(param.getValue()));
			}
			break;
		case DOUBLE:
			if (param.getValue() != null) {
				formattedValue= String.valueOf(Double.parseDouble(param.getValue()));
			}
			break;
		case FLOAT:
			if (param.getValue() != null) {
				formattedValue= String.valueOf(Float.parseFloat(param.getValue()));
			}
			break;
		case SHORT:
			if (param.getValue() != null) {
				formattedValue= String.valueOf(Short.parseShort(param.getValue()));
			}
			break;
		case BYTE:
			if (param.getValue() != null) {
				formattedValue= String.valueOf(Byte.parseByte(param.getValue()));
			}
			break;
		case INTEGER:
			if (param.getValue() != null) {
				formattedValue= String.valueOf(Integer.parseInt(param.getValue()));
			}
			break;
		default:
			formattedValue= (String) param.getValue();
			break;
		}

		if (param.getValue() != null) {
			textBox.setText(formattedValue);
		} else {
			textBox.setText("");
		}

		if (param.getMin() != null && param.getMin().equals(param.getMax())) {
			textBox.setReadOnly(true);
			textBox.setEnabled(false);
		}

		formGroup.add(textBox);

		textBox.addValueChangeHandler(new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				setDirty(true);
				TextBox box = (TextBox) event.getSource();
				FormGroup group = (FormGroup) box.getParent();
				validate(param,box,group);
			}
		});

		fields.add(formGroup);

	}

	private void renderPasswordField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
		valid.put(param.getName(), true);

		if (isFirstInstance) {
			FormLabel formLabel = new FormLabel();
			if (param.isRequired()) {
				formLabel.setText(param.getName() + "*");
			} else {
				formLabel.setText(param.getName());
			}
			formGroup.add(formLabel);

			if (param.getDescription() != null) {
				HelpBlock toolTip = new HelpBlock();
				toolTip.setText(param.getDescription());
				formGroup.add(toolTip);
			}
		}

		Input input = new Input();
		input.setType(InputType.PASSWORD);
		if (param.getValue() != null) {
			input.setText((String) param.getValue());
		} else {
			input.setText("");
		}

		if (param.getMin() != null && param.getMin().equals(param.getMax())) {
			input.setReadOnly(true);
			input.setEnabled(false);
		}

		input.addValueChangeHandler(new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				setDirty(true);
				Input box = (Input) event.getSource();
				FormGroup group = (FormGroup) box.getParent();
				// Validation
				if ((box.getText() == null || "".equals(box.getText().trim()))
						&& param.isRequired()) {
					// null in required field
					group.setValidationState(ValidationState.ERROR);
					box.setPlaceholder("Field is required");
					valid.put(param.getName(), false);
				} else {
					group.setValidationState(ValidationState.NONE);
					box.setPlaceholder("");
					param.setValue(box.getText());
					valid.put(param.getName(), true);
				}
			}
		});

		formGroup.add(input);
		fields.add(formGroup);

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void renderBooleanField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
		valid.put(param.getName(), true);

		if (isFirstInstance) {
			FormLabel formLabel = new FormLabel();
			if (param.isRequired()) {
				formLabel.setText(param.getName() + "*");
			} else {
				formLabel.setText(param.getName());
			}
			formGroup.add(formLabel);

			if (param.getDescription() != null) {
				HelpBlock toolTip = new HelpBlock();
				toolTip.setText(param.getDescription());
				formGroup.add(toolTip);
			}
		}

		FlowPanel flowPanel = new FlowPanel();

		InlineRadio radioTrue = new InlineRadio(param.getName());
		radioTrue.setText(MSGS.trueLabel());
		radioTrue.setFormValue("true");

		InlineRadio radioFalse = new InlineRadio(param.getName());
		radioFalse.setText(MSGS.falseLabel());
		radioFalse.setFormValue("false");

		radioTrue.setValue(Boolean.parseBoolean(param.getValue()));
		radioFalse.setValue(!Boolean.parseBoolean(param.getValue()));

		if (param.getMin() != null && param.getMin().equals(param.getMax())) {
			radioTrue.setEnabled(false);
			radioFalse.setEnabled(false);
		}

		flowPanel.add(radioTrue);
		flowPanel.add(radioFalse);

		radioTrue.addValueChangeHandler(new ValueChangeHandler() {
			@Override
			public void onValueChange(ValueChangeEvent event) {
				setDirty(true);
				InlineRadio box = (InlineRadio) event.getSource();
				if (box.getValue()) {
					param.setValue(String.valueOf(true));
				}
			}
		});
		radioFalse.addValueChangeHandler(new ValueChangeHandler() {
			@Override
			public void onValueChange(ValueChangeEvent event) {
				setDirty(true);
				InlineRadio box = (InlineRadio) event.getSource();
				if (box.getValue()) {
					param.setValue(String.valueOf(false));
				}
			}
		});

		formGroup.add(flowPanel);

		fields.add(formGroup);
	}

	private void renderChoiceField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
		valid.put(param.getName(), true);

		if (isFirstInstance) {
			FormLabel formLabel = new FormLabel();
			if (param.isRequired()) {
				formLabel.setText(param.getName() + "*");
			} else {
				formLabel.setText(param.getName());
			}
			formGroup.add(formLabel);

			if (param.getDescription() != null) {
				HelpBlock toolTip = new HelpBlock();
				toolTip.setText(param.getDescription());
				formGroup.add(toolTip);
			}
		}

		ListBox listBox = new ListBox();

		String current;
		int i = 0;
		Map<String, String> oMap = param.getOptions();
		java.util.Iterator<String> it = oMap.keySet().iterator();
		while (it.hasNext()) {
			current = it.next();
			listBox.addItem(current);
			if (param.getDefault() != null && 
					oMap.get(current).equals((String) param.getDefault())) {
				listBox.setSelectedIndex(i);
			}

			if (param.getValue() != null && 
					oMap.get(current).equals((String) param.getValue())) {
				listBox.setSelectedIndex(i);
			}
			i++;
		}

		listBox.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				setDirty(true);
				ListBox box = (ListBox) event.getSource();
				param.setValue(box.getSelectedItemText());
			}
		});

		formGroup.add(listBox);

		fields.add(formGroup);
	}

	//Checks if all the fields are valid according to the Validate() method
	private boolean isValid() {
		// check if all fields are valid
		for (Map.Entry<String, Boolean> entry : valid.entrySet()) {
			if (!entry.getValue()) {
				return false;
			}
		}
		return true;
	}

	private void setOriginalValues(GwtConfigComponent component) {
		for (GwtConfigParameter parameter : component.getParameters()) {
			parameter.setValue(parameter.getValue());
		}
	}

	//Validates all the entered values
	private boolean validate(GwtConfigParameter param, TextBox box, FormGroup group){  //TODO: validation should be done like in the old web ui: cleaner approach
		if(param.isRequired() && (box.getText().trim() == null || "".equals(box.getText().trim()))) {
			group.setValidationState(ValidationState.ERROR);
			valid.put(param.getName(), false);
			box.setPlaceholder(MSGS.formRequiredParameter());
			return false;
		} else if (box.getText().trim() != null && !"".equals(box.getText().trim())){
			if (param.getType().equals(GwtConfigParameterType.CHAR)) {
				if (box.getText().trim().length() > 1) {
					group.setValidationState(ValidationState.ERROR);
					valid.put(param.getName(), false);
					box.setPlaceholder(MessageUtils.get(Integer.toString(box.getText().trim().length()), box.getText()));
					return false;
				}
				if (param.getMin() != null) {
					if(Character.valueOf(param.getMin().charAt(0)).charValue() > Character.valueOf(box.getText().trim().charAt(0)).charValue()){  //TODO: why this character boxing?
						group.setValidationState(ValidationState.ERROR);
						valid.put(param.getName(), false);
						box.setPlaceholder(MessageUtils.get(CONFIG_MIN_VALUE, Character.valueOf(param.getMin().charAt(0)).charValue()));
						return false;
					}
				}
				if (param.getMax()!=null) {
					if(Character.valueOf(param.getMax().charAt(0)).charValue() < Character.valueOf(box.getText().trim().charAt(0)).charValue()){
						group.setValidationState(ValidationState.ERROR);
						valid.put(param.getName(), false);
						box.setPlaceholder(MessageUtils.get(CONFIG_MAX_VALUE, Character.valueOf(param.getMax().charAt(0)).charValue()));
						return false;
					}
				}
			} else if (param.getType().equals(GwtConfigParameterType.STRING)) {
				int configMinValue= Integer.parseInt(param.getMin());
				int configMaxValue= Integer.parseInt(param.getMax());
				if ((String.valueOf(box.getText().trim()).length()) < Math.max(configMinValue, 0)) {
					group.setValidationState(ValidationState.ERROR);
					valid.put(param.getName(), false);
					box.setPlaceholder(MessageUtils.get(CONFIG_MIN_VALUE, Math.max(configMinValue, 0)));
					return false;
				}				
				if ((String.valueOf(box.getText().trim()).length()) > Math.min(configMaxValue, 255)) {
					group.setValidationState(ValidationState.ERROR);
					valid.put(param.getName(), false);
					box.setPlaceholder(MessageUtils.get(CONFIG_MAX_VALUE, Math.min(configMaxValue, 255)));
					return false;
				}	
			} else {
				try{
					//numeric value
					if (param.getType().equals(GwtConfigParameterType.FLOAT)) {
						if (param.getMin() != null) {
							if (Float.valueOf(param.getMin()).floatValue() > Float.valueOf(box.getText().trim()).floatValue()) {
								group.setValidationState(ValidationState.ERROR);
								valid.put(param.getName(), false);
								box.setPlaceholder(MessageUtils.get(CONFIG_MIN_VALUE, param.getMin()));
								return false;
							}
						}
						if (param.getMax()!=null) {
							if (Float.valueOf(param.getMax()).floatValue() < Float.valueOf(box.getText().trim()).floatValue()) {
								group.setValidationState(ValidationState.ERROR);
								valid.put(param.getName(), false);
								box.setPlaceholder(MessageUtils.get(CONFIG_MAX_VALUE, param.getMax()));
								return false;
							}
						}
					} else if (param.getType().equals(GwtConfigParameterType.INTEGER)) {
						if (param.getMin() != null) {
							if (Integer.valueOf(param.getMin()).intValue() > Integer.valueOf(box.getText().trim()).intValue()) {
								group.setValidationState(ValidationState.ERROR);
								valid.put(param.getName(), false);
								box.setPlaceholder(MessageUtils.get(CONFIG_MIN_VALUE, param.getMin()));
								return false;
							}
						}
						if (param.getMax() != null) {
							if (Integer.valueOf(param.getMax()).intValue() < Integer.valueOf(box.getText().trim()).intValue()) {
								group.setValidationState(ValidationState.ERROR);
								valid.put(param.getName(), false);
								box.setPlaceholder(MessageUtils.get(CONFIG_MAX_VALUE, param.getMax()));
								return false;
							}
						}
					} else if (param.getType().equals(GwtConfigParameterType.SHORT)) {
						if (param.getMin() != null) {
							if (Short.valueOf(param.getMin()).shortValue() > Short.valueOf(box.getText().trim()).shortValue()) {
								group.setValidationState(ValidationState.ERROR);
								valid.put(param.getName(), false);
								box.setPlaceholder(MessageUtils.get(CONFIG_MIN_VALUE, param.getMin()));
								return false;
							}
						}
						if (param.getMax() != null) {
							if (Short.valueOf(param.getMax()).shortValue() < Short.valueOf(box.getText().trim()).shortValue()) {
								group.setValidationState(ValidationState.ERROR);
								valid.put(param.getName(), false);
								box.setPlaceholder(MessageUtils.get(CONFIG_MAX_VALUE, param.getMax()));
								return false;
							}
						}
					} else if (param.getType().equals(GwtConfigParameterType.BYTE)) {
						if (param.getMin() != null) {
							if (Byte.valueOf(param.getMin()).byteValue() > Byte.valueOf(box.getText().trim()).byteValue()) {
								group.setValidationState(ValidationState.ERROR);
								valid.put(param.getName(), false);
								box.setPlaceholder(MessageUtils.get(CONFIG_MIN_VALUE, param.getMin()));
								return false;
							}
						}
						if (param.getMax() != null) {
							if (Byte.valueOf(param.getMax()).byteValue() < Byte.valueOf(box.getText().trim()).byteValue()) {
								group.setValidationState(ValidationState.ERROR);
								valid.put(param.getName(), false);
								box.setPlaceholder(MessageUtils.get(CONFIG_MAX_VALUE, param.getMax()));
								return false;
							}
						}
					} else if (param.getType().equals(GwtConfigParameterType.LONG)) {
						if (param.getMin() != null) {
							if (Long.valueOf(param.getMin()).longValue() > Long.valueOf(box.getText().trim()).longValue()) {
								group.setValidationState(ValidationState.ERROR);
								valid.put(param.getName(), false);
								box.setPlaceholder(MessageUtils.get(CONFIG_MIN_VALUE, param.getMin()));
								return false;
							}
						}
						if (param.getMax() != null) {
							if (Long.valueOf(param.getMax()).longValue() < Long.valueOf(box.getText().trim()).longValue()) {
								group.setValidationState(ValidationState.ERROR);
								valid.put(param.getName(), false);
								box.setPlaceholder(MessageUtils.get(CONFIG_MAX_VALUE, param.getMax()));
								return false;
							}
						}
					} else if (param.getType().equals(GwtConfigParameterType.DOUBLE)) {
						if (param.getMin() != null) {
							if (Double.valueOf(param.getMin()).doubleValue() > Double.valueOf(box.getText().trim()).doubleValue()) {
								group.setValidationState(ValidationState.ERROR);
								valid.put(param.getName(), false);
								box.setPlaceholder(MessageUtils.get(CONFIG_MIN_VALUE, param.getMin()));
								return false;
							}
						}
						if (param.getMax() != null) {
							if (Double.valueOf(param.getMax()).doubleValue() < Double.valueOf(box.getText().trim()).doubleValue()) {
								group.setValidationState(ValidationState.ERROR);
								valid.put(param.getName(), false);
								box.setPlaceholder(MessageUtils.get(CONFIG_MAX_VALUE, param.getMax()));
								return false;
							}
						}
					}
				} catch(NumberFormatException e) {
					group.setValidationState(ValidationState.ERROR);
					valid.put(param.getName(), false);
					box.setPlaceholder(e.getLocalizedMessage());
					return false;
				}
			}
		}	
		group.setValidationState(ValidationState.NONE);
		valid.put(param.getName(), true);
		return true;
	}

	private void initInvalidDataModal() {
		incompleteFieldsModal.setTitle(MSGS.warning());
		incompleteFieldsText.setText(MSGS.formWithErrorsOrIncomplete());
	}
}
