/*
 * Render the Content in the Main Panel corressponding to Service (GwtBSConfigComponent) selected in the Services Panel
 * 
 * Fields are rendered based on their type (Password(Input), Choice(Dropboxes) etc. with Text fields rendered
 * for both numeric and other textual field with validate() checking if value in numeric fields is numeric
 */
package org.eclipse.kura.web.client.bootstrap.ui;

import java.util.HashMap;
import java.util.Map;
import java.lang.Integer;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.MessageUtils;
import org.eclipse.kura.web.shared.model.GwtBSConfigComponent;
import org.eclipse.kura.web.shared.model.GwtBSConfigParameter;
import org.eclipse.kura.web.shared.model.GwtBSConfigParameter.GwtBSConfigParameterType;
import org.eclipse.kura.web.shared.service.GwtBSComponentService;
import org.eclipse.kura.web.shared.service.GwtBSComponentServiceAsync;
import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.ButtonGroup;
import org.gwtbootstrap3.client.ui.FieldSet;
import org.gwtbootstrap3.client.ui.Form;
import org.gwtbootstrap3.client.ui.HelpBlock;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.FormLabel;
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
import org.gwtbootstrap3.extras.growl.client.ui.Growl;

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

	private static ServicesUiUiBinder uiBinder = GWT
			.create(ServicesUiUiBinder.class);

	interface ServicesUiUiBinder extends UiBinder<Widget, ServicesUi> {
	}

	private static final Messages MSGS = GWT.create(Messages.class);
	private final GwtBSComponentServiceAsync gwtComponentService = GWT.create(GwtBSComponentService.class);

	private final static String REGEX_NUM = "^[0-9][\\.\\d]*(,\\d+)?$";
	HashMap<String, Boolean> valid = new HashMap<String, Boolean>();

	NavPills menu;
	PanelBody content;
	AnchorListItem service;
	GwtBSConfigComponent selected;
	private boolean dirty, initialized;
	TextBox validated;
	FormGroup validatedGroup;
	EntryClassUi entryClass;
	Modal modal;
	
	@UiField
	AnchorListItem apply, reset;
	@UiField
	FieldSet fields;
	@UiField
	Form form;

	public ServicesUi(final GwtBSConfigComponent addedItem, EntryClassUi entryClassUi) {
		initWidget(uiBinder.createAndBindUi(this));
		initialized = false;
		entryClass = entryClassUi;
		selected = addedItem;
		fields.clear();
		setOriginalValues(selected);

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

	private void apply() {		
		if (isValid()) {
			if(isDirty()){
				//TODO ask for confirmation first
				modal = new Modal();
				
				ModalHeader header = new ModalHeader();
				header.setTitle(MSGS.confirm());
				modal.add(header);
				
				ModalBody body = new ModalBody();
				body.add(new Span(MSGS.deviceConfigConfirmation(selected.getComponentName())));
				modal.add(body);
				
				ModalFooter footer = new ModalFooter();
					ButtonGroup group= new ButtonGroup();
						Button yes = new Button();
						yes.setText(MSGS.yesButton());
						yes.addClickHandler(new ClickHandler(){
								@Override
								public void onClick(ClickEvent event) {									
									gwtComponentService.updateComponentConfiguration(selected, new AsyncCallback<Void>(){
										@Override
										public void onFailure(Throwable caught) {
											Growl.growl(MSGS.error()+": ",caught.getLocalizedMessage());
										}

										@Override
										public void onSuccess(Void result) {
											modal.hide();	
											Growl.growl(MSGS.info()+": ",MSGS.deviceConfigApplied());
											apply.setEnabled(false);
											reset.setEnabled(false);
											setDirty(false);
											entryClass.initServicesTree();
										}});
									

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
			Growl.growl(MSGS.deviceConfigError());
		}//end else isValid	
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

	//Iterates through all GwtBSConfigParameter in the selected GwtBSConfigComponent
	public void renderForm() {
		fields.clear();
		for (GwtBSConfigParameter param : selected.getParameters()) {
			if (param.getCardinality() == 0 || param.getCardinality() == 1
					|| param.getCardinality() == -1) {
				renderConfigParameter(param);				
			} else {
				renderMultiFieldConfigParameter(param);
			}
		}
		initialized = true;
	}

	private void renderMultiFieldConfigParameter(GwtBSConfigParameter mParam) {
		String value = null;
		String[] values = mParam.getValues();
		for (int i = 0; i < Math.min(mParam.getCardinality(), 10); i++) {
			// temporary set the param value to the current one in the array
			// use a value from the one passed in if we have it.
			value = null;
			if (values != null && i < values.length) {
				value = values[i];
			}
			mParam.setValue(value);
			;
			renderConfigParameter(mParam);
		}
		// restore a null current value
		mParam.setValue(null);
	}

	//passes the parameter to the corressponding method depending on the type of field to be rendered
	private void renderConfigParameter(GwtBSConfigParameter param) {
		Map<String, String> options = param.getOptions();
		if (options != null && options.size() > 0) {
			renderChoiceField(param);
		} else if (param.getType().equals(GwtBSConfigParameterType.BOOLEAN)) {
			renderBooleanField(param);
		} else if (param.getType().equals(GwtBSConfigParameterType.PASSWORD)) {
			renderPasswordField(param);
		} else {
			renderTextField(param);
		}
	}

	// Field Render based on Type
	private void renderTextField(final GwtBSConfigParameter param) {

		valid.put(param.getName(), true);
		FormGroup formGroup = new FormGroup();

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

		TextBox textBox = new TextBox();
		if (param.getDescription().contains("\u200B\u200B\u200B\u200B\u200B")) {
			textBox.setHeight("120px");
		}
		if (param.getOriginalValue() != null) {
			textBox.setText(param.getOriginalValue());
		} else {
			textBox.setText("");
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

	private void renderPasswordField(final GwtBSConfigParameter param) {

		valid.put(param.getName(), true);
		FormGroup formGroup = new FormGroup();

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

		Input input = new Input();
		input.setType(InputType.PASSWORD);
		if (param.getOriginalValue() != null) {
			input.setText((String) param.getOriginalValue());
		} else {
			input.setText("");
		}

		input.addValueChangeHandler(new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				setDirty(true);
				Input box = (Input) event.getSource();
				FormGroup group = (FormGroup) box.getParent();
				// Validation
				if ((box.getText() == null || box.getText().trim() == "")
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
	private void renderBooleanField(final GwtBSConfigParameter param) {

		valid.put(param.getName(), true);

		FormGroup formGroup = new FormGroup();

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

		FlowPanel flowPanel = new FlowPanel();

		InlineRadio radioTrue = new InlineRadio(param.getName());
		radioTrue.setText(MSGS.trueLabel());
		radioTrue.setFormValue("true");

		InlineRadio radioFalse = new InlineRadio(param.getName());
		radioFalse.setText(MSGS.falseLabel());
		radioFalse.setFormValue("false");

		radioTrue.setValue(param.getOriginalValue().equalsIgnoreCase("true"));
		radioFalse.setValue(!param.getOriginalValue().equalsIgnoreCase("true"));

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

	private void renderChoiceField(final GwtBSConfigParameter param) {

		valid.put(param.getName(), true);
		FormGroup formGroup = new FormGroup();

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

		ListBox listBox = new ListBox();

		String current;
		int i = 0;
		Map<String, String> oMap = param.getOptions();
		java.util.Iterator<String> it = oMap.keySet().iterator();
		while (it.hasNext()) {
			current = it.next();
			listBox.addItem(current);
			if (param.getDefault() != null
					&& oMap.get(current).equals(param.getDefault())) {
				listBox.setSelectedIndex(i);
			}

			if (param.getOriginalValue() != null
					&& oMap.get(current).equals(param.getOriginalValue())) {
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

	// -------------

	public GwtBSConfigComponent getConfiguration() {
		return selected;
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

	private void setOriginalValues(GwtBSConfigComponent component) {
		for (GwtBSConfigParameter parameter : component.getParameters()) {
			parameter.setOriginalValue(parameter.getValue());
		}
	}
	
	//Validates all the entered values
	private boolean validate(GwtBSConfigParameter param, TextBox box, FormGroup group){
		
		if(param.isRequired() && (box.getText().trim()==null || box.getText().trim()=="")){
			group.setValidationState(ValidationState.ERROR);
			valid.put(param.getName(), false);
			box.setPlaceholder(MSGS.formRequiredParameter());
			return false;
		}else{
			if(param.getType().equals(GwtBSConfigParameterType.CHAR)){
				if(param.getMin()!=null){
					if(Character.valueOf(param.getMin().charAt(0)).charValue() > Character.valueOf(box.getText().trim().charAt(0)).charValue()){
						group.setValidationState(ValidationState.ERROR);
						valid.put(param.getName(), false);
						box.setPlaceholder(MessageUtils.get("configMinValue", Character.valueOf(param.getMin().charAt(0)).charValue()));
						return false;
					}
				}
				if(param.getMax()!=null){
					if(Character.valueOf(param.getMax().charAt(0)).charValue() < Character.valueOf(box.getText().trim().charAt(0)).charValue()){
						group.setValidationState(ValidationState.ERROR);
						valid.put(param.getName(), false);
						box.setPlaceholder(MessageUtils.get("configMaxValue", Character.valueOf(param.getMax().charAt(0)).charValue()));
						return false;
					}
				}
			}else if(param.getType().equals(GwtBSConfigParameterType.STRING)){
				if((String.valueOf(box.getText().trim()).length())<0){
					group.setValidationState(ValidationState.ERROR);
					valid.put(param.getName(), false);
					box.setPlaceholder(MessageUtils.get("configMinValue", 0));
					return false;
				}				
				if((String.valueOf(box.getText().trim()).length())>255){
					group.setValidationState(ValidationState.ERROR);
					valid.put(param.getName(), false);
					box.setPlaceholder(MessageUtils.get("configMaxValue", 255));
					return false;
				}	
			}else if(!box.getText().trim().matches(REGEX_NUM)){ 
				//not a numeric value
				group.setValidationState(ValidationState.ERROR);
				valid.put(param.getName(), false);
				box.setPlaceholder(MSGS.formNumericParameter());
				return false;
			}else{
				try{
					//numeric value
					if(param.getType().equals(GwtBSConfigParameterType.FLOAT)){
						if(param.getMin()!=null){
							if(Float.valueOf(param.getMin()).floatValue() > Float.valueOf(box.getText().trim()).floatValue()){
								group.setValidationState(ValidationState.ERROR);
								valid.put(param.getName(), false);
								box.setPlaceholder(MessageUtils.get("configMinValue", param.getMin()));
								return false;
							}
						}
						if(param.getMax()!=null){
							if(Float.valueOf(param.getMax()).floatValue() < Float.valueOf(box.getText().trim()).floatValue()){
								group.setValidationState(ValidationState.ERROR);
								valid.put(param.getName(), false);
								box.setPlaceholder(MessageUtils.get("configMaxValue", param.getMax()));
								return false;
							}
						}
					}else if(param.getType().equals(GwtBSConfigParameterType.INTEGER)){
						if(param.getMin()!=null){
							if(Integer.valueOf(param.getMin()).intValue() > Integer.valueOf(box.getText().trim()).intValue()){
								group.setValidationState(ValidationState.ERROR);
								valid.put(param.getName(), false);
								box.setPlaceholder(MessageUtils.get("configMinValue", param.getMin()));
								return false;
							}
						}
						if(param.getMax()!=null){
							if(Integer.valueOf(param.getMax()).intValue() < Integer.valueOf(box.getText().trim()).intValue()){
								group.setValidationState(ValidationState.ERROR);
								valid.put(param.getName(), false);
								box.setPlaceholder(MessageUtils.get("configMaxValue", param.getMax()));
								return false;
							}
						}
					}else if(param.getType().equals(GwtBSConfigParameterType.SHORT)){
						if(param.getMin()!=null){
							if(Short.valueOf(param.getMin()).shortValue() > Short.valueOf(box.getText().trim()).shortValue()){
								group.setValidationState(ValidationState.ERROR);
								valid.put(param.getName(), false);
								box.setPlaceholder(MessageUtils.get("configMinValue", param.getMin()));
								return false;
							}
						}
						if(param.getMax()!=null){
							if(Short.valueOf(param.getMax()).shortValue() < Short.valueOf(box.getText().trim()).shortValue()){
								group.setValidationState(ValidationState.ERROR);
								valid.put(param.getName(), false);
								box.setPlaceholder(MessageUtils.get("configMaxValue", param.getMax()));
								return false;
							}
						}
					}else if(param.getType().equals(GwtBSConfigParameterType.BYTE)){
						if(param.getMin()!=null){
							if(Byte.valueOf(param.getMin()).byteValue() > Byte.valueOf(box.getText().trim()).byteValue()){
								group.setValidationState(ValidationState.ERROR);
								valid.put(param.getName(), false);
								box.setPlaceholder(MessageUtils.get("configMinValue", param.getMin()));
								return false;
							}
						}
						if(param.getMax()!=null){
							if(Byte.valueOf(param.getMax()).byteValue() < Byte.valueOf(box.getText().trim()).byteValue()){
								group.setValidationState(ValidationState.ERROR);
								valid.put(param.getName(), false);
								box.setPlaceholder(MessageUtils.get("configMaxValue", param.getMax()));
								return false;
							}
						}
					}else if(param.getType().equals(GwtBSConfigParameterType.LONG)){
						if(param.getMin()!=null){
							if(Long.valueOf(param.getMin()).longValue() > Long.valueOf(box.getText().trim()).longValue()){
								group.setValidationState(ValidationState.ERROR);
								valid.put(param.getName(), false);
								box.setPlaceholder(MessageUtils.get("configMinValue", param.getMin()));
								return false;
							}
						}
						if(param.getMax()!=null){
							if(Long.valueOf(param.getMax()).longValue() < Long.valueOf(box.getText().trim()).longValue()){
								group.setValidationState(ValidationState.ERROR);
								valid.put(param.getName(), false);
								box.setPlaceholder(MessageUtils.get("configMaxValue", param.getMax()));
								return false;
							}
						}
					}else if(param.getType().equals(GwtBSConfigParameterType.DOUBLE)){
						if(param.getMin()!=null){
							if(Double.valueOf(param.getMin()).doubleValue() > Double.valueOf(box.getText().trim()).doubleValue()){
								group.setValidationState(ValidationState.ERROR);
								valid.put(param.getName(), false);
								box.setPlaceholder(MessageUtils.get("configMinValue", param.getMin()));
								return false;
							}
						}
						if(param.getMax()!=null){
							if(Double.valueOf(param.getMax()).doubleValue() < Double.valueOf(box.getText().trim()).doubleValue()){
								group.setValidationState(ValidationState.ERROR);
								valid.put(param.getName(), false);
								box.setPlaceholder(MessageUtils.get("configMaxValue", param.getMax()));
								return false;
							}
						}
					}
				}catch(NumberFormatException e){
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
}
