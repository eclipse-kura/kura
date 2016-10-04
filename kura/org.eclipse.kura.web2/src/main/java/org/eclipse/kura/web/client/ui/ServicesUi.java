/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat - Fixes and cleanups
 *          - Allow the use of text boxed and text areas
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
import org.gwtbootstrap3.client.ui.InlineHelpBlock;
import org.gwtbootstrap3.client.ui.InlineRadio;
import org.gwtbootstrap3.client.ui.Input;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.ModalHeader;
import org.gwtbootstrap3.client.ui.NavPills;
import org.gwtbootstrap3.client.ui.PanelBody;
import org.gwtbootstrap3.client.ui.TextArea;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.base.TextBoxBase;
import org.gwtbootstrap3.client.ui.constants.IconType;
import org.gwtbootstrap3.client.ui.constants.InputType;
import org.gwtbootstrap3.client.ui.constants.ValidationState;
import org.gwtbootstrap3.client.ui.form.error.BasicEditorError;
import org.gwtbootstrap3.client.ui.form.validator.Validator;
import org.gwtbootstrap3.client.ui.gwt.FlowPanel;
import org.gwtbootstrap3.client.ui.html.Span;
import org.gwtbootstrap3.client.ui.html.Text;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorError;
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
    private static final ServicesUiUiBinder uiBinder = GWT.create(ServicesUiUiBinder.class);
    private static final Logger logger = Logger.getLogger(ServicesUi.class.getSimpleName());
    private static final Logger errorLogger = Logger.getLogger("ErrorLogger");

    interface ServicesUiUiBinder extends UiBinder<Widget, ServicesUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);
    private final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);
    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);

    HashMap<String, Boolean> valid = new HashMap<String, Boolean>();

    GwtConfigComponent m_configurableComponent;
    private boolean dirty, initialized;

    NavPills menu;
    PanelBody content;
    AnchorListItem service;
    TextBox validated;
    FormGroup validatedGroup;
    EntryClassUi entryClass;
    Modal modal;

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
        this.initialized = false;
        this.entryClass = entryClassUi;
        this.m_configurableComponent = addedItem;
        this.fields.clear();
        setOriginalValues(this.m_configurableComponent);

        this.apply.setText(MSGS.apply());
        this.apply.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                apply();
            }
        });

        this.reset.setText(MSGS.reset());
        this.reset.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                reset();
            }
        });
        renderForm();
        initInvalidDataModal();

        setDirty(false);
        this.apply.setEnabled(false);
        this.reset.setEnabled(false);
    }

    public void setDirty(boolean flag) {
        this.dirty = flag;
        if (this.dirty && this.initialized) {
            this.apply.setEnabled(true);
            this.reset.setEnabled(true);
        }
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public void reset() {
        if (isDirty()) {
            // Modal
            this.modal = new Modal();

            ModalHeader header = new ModalHeader();
            header.setTitle(MSGS.confirm());
            this.modal.add(header);

            ModalBody body = new ModalBody();
            body.add(new Span(MSGS.deviceConfigDirty()));
            this.modal.add(body);

            ModalFooter footer = new ModalFooter();
            ButtonGroup group = new ButtonGroup();
            Button yes = new Button();
            yes.setText(MSGS.yesButton());
            yes.addClickHandler(new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {
                    ServicesUi.this.modal.hide();
                    renderForm();
                    ServicesUi.this.apply.setEnabled(false);
                    ServicesUi.this.reset.setEnabled(false);
                    setDirty(false);
                    ServicesUi.this.entryClass.initServicesTree();
                }
            });
            group.add(yes);
            Button no = new Button();
            no.setText(MSGS.noButton());
            no.addClickHandler(new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {
                    ServicesUi.this.modal.hide();
                }
            });
            group.add(no);
            footer.add(group);
            this.modal.add(footer);
            this.modal.show();
        }                  // end is dirty
    }

    // TODO: Separate render methods for each type (ex: Boolean, String,
    // Password, etc.). See latest org.eclipse.kura.web code.
    // Iterates through all GwtConfigParameter in the selected
    // GwtConfigComponent
    public void renderForm() {
        this.fields.clear();
        for (GwtConfigParameter param : this.m_configurableComponent.getParameters()) {
            if (param.getCardinality() == 0 || param.getCardinality() == 1 || param.getCardinality() == -1) {
                FormGroup formGroup = new FormGroup();
                renderConfigParameter(param, true, formGroup);
            } else {
                renderMultiFieldConfigParameter(param);
            }
        }
        this.initialized = true;
    }

    public GwtConfigComponent getConfiguration() {
        return this.m_configurableComponent;
    }

    //
    // Private methods
    //
    private void apply() {
        if (isValid()) {
            if (isDirty()) {
                // TODO ask for confirmation first
                this.modal = new Modal();

                ModalHeader header = new ModalHeader();
                header.setTitle(MSGS.confirm());
                this.modal.add(header);

                ModalBody body = new ModalBody();
                body.add(new Span(MSGS.deviceConfigConfirmation(this.m_configurableComponent.getComponentName())));
                this.modal.add(body);

                ModalFooter footer = new ModalFooter();
                ButtonGroup group = new ButtonGroup();
                Button yes = new Button();
                yes.setText(MSGS.yesButton());
                yes.addClickHandler(new ClickHandler() {

                    @Override
                    public void onClick(ClickEvent event) {
                        EntryClassUi.showWaitModal();
                        try {
                            getUpdatedConfiguration();
                        } catch (Exception ex) {
                            EntryClassUi.hideWaitModal();
                            FailureHandler.handle(ex);
                            return;
                        }
                        ServicesUi.this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                            @Override
                            public void onFailure(Throwable ex) {
                                EntryClassUi.hideWaitModal();
                                FailureHandler.handle(ex);
                            }

                            @Override
                            public void onSuccess(GwtXSRFToken token) {
                                ServicesUi.this.gwtComponentService.updateComponentConfiguration(token,
                                        ServicesUi.this.m_configurableComponent, new AsyncCallback<Void>() {

                                    @Override
                                    public void onFailure(Throwable caught) {
                                        EntryClassUi.hideWaitModal();
                                        FailureHandler.handle(caught);
                                        errorLogger.log(
                                                Level.SEVERE, caught.getLocalizedMessage() != null
                                                        ? caught.getLocalizedMessage() : caught.getClass().getName(),
                                                caught);
                                    }

                                    @Override
                                    public void onSuccess(Void result) {
                                        ServicesUi.this.modal.hide();
                                        logger.info(MSGS.info() + ": " + MSGS.deviceConfigApplied());
                                        ServicesUi.this.apply.setEnabled(false);
                                        ServicesUi.this.reset.setEnabled(false);
                                        setDirty(false);
                                        ServicesUi.this.entryClass.initServicesTree();
                                        EntryClassUi.hideWaitModal();
                                    }
                                });

                            }
                        });
                    }
                });
                group.add(yes);
                Button no = new Button();
                no.setText(MSGS.noButton());
                no.addClickHandler(new ClickHandler() {

                    @Override
                    public void onClick(ClickEvent event) {
                        ServicesUi.this.modal.hide();
                    }
                });
                group.add(no);
                footer.add(group);
                this.modal.add(footer);
                this.modal.show();

                // ----

            }                  // end isDirty()
        } else {
            errorLogger.log(Level.SEVERE, "Device configuration error!");
            this.incompleteFieldsModal.show();
        }                  // end else isValid
    }

    // Get updated parameters
    private GwtConfigComponent getUpdatedConfiguration() {
        Iterator<Widget> it = this.fields.iterator();
        while (it.hasNext()) {
            Widget w = it.next();
            if (w instanceof FormGroup) {
                FormGroup fg = (FormGroup) w;
                fillUpdatedConfiguration(fg);
            }
        }
        return this.m_configurableComponent;
    }

    private void fillUpdatedConfiguration(FormGroup fg) {
        GwtConfigParameter param = new GwtConfigParameter();
        List<String> multiFieldValues = new ArrayList<String>();
        int fgwCount = fg.getWidgetCount();
        for (int i = 0; i < fgwCount; i++) {
            logger.fine("Widget: " + fg.getClass());

            if (fg.getWidget(i) instanceof FormLabel) {
                final String id = ((FormLabel) fg.getWidget(i)).getTitle();
                param = this.m_configurableComponent.getParameter(id);

            } else if (fg.getWidget(i) instanceof ListBox || fg.getWidget(i) instanceof Input
                    || fg.getWidget(i) instanceof TextBoxBase) {

                String value = getUpdatedFieldConfiguration(param, fg.getWidget(i));
                if (value == null) {
                    continue;
                }
                if (param.getCardinality() == 0 || param.getCardinality() == 1 || param.getCardinality() == -1) {
                    param.setValue(value);
                } else {
                    multiFieldValues.add(value);
                }
            }
        }
        if (!multiFieldValues.isEmpty()) {
            param.setValues(multiFieldValues.toArray(new String[] {}));
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
        } else {
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
                TextBoxBase tb = (TextBoxBase) wg;
                String value = tb.getText();
                if (value != null) {
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
        boolean isFirstInstance = true;
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
                isFirstInstance = false;
            }
        }
        // restore a null current value
        mParam.setValue(null);
    }

    // passes the parameter to the corresponding method depending on the type of
    // field to be rendered
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
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void renderTextField(final GwtConfigParameter param, boolean isFirstInstance, final FormGroup formGroup) {

        this.valid.put(param.getId(), true);

        if (isFirstInstance) {
            FormLabel formLabel = new FormLabel();
            formLabel.setTitle(param.getId()); // title is used to hold ID
            formLabel.setText(param.getName());
            if (param.isRequired()) {
                formLabel.setShowRequiredIndicator(true);
            }
            formGroup.add(formLabel);

            InlineHelpBlock ihb = new InlineHelpBlock();
            ihb.setIconType(IconType.EXCLAMATION_TRIANGLE);
            formGroup.add(ihb);

            HelpBlock tooltip = new HelpBlock();
            tooltip.setText(getDescription(param));
            formGroup.add(tooltip);
        }

        final TextBoxBase textBox = createTextBox(param);

        String formattedValue = new String();

        // TODO: Probably this formatting step has no
        // sense. But it seems that, if not in debug,
        // all the browsers are able to display the
        // double value as expected
        switch (param.getType()) {
        case LONG:
            if (param.getValue() != null && !"".equals(param.getValue().trim())) {
                formattedValue = String.valueOf(Long.parseLong(param.getValue()));
            }
            break;
        case DOUBLE:
            if (param.getValue() != null && !"".equals(param.getValue().trim())) {
                formattedValue = String.valueOf(Double.parseDouble(param.getValue()));
            }
            break;
        case FLOAT:
            if (param.getValue() != null && !"".equals(param.getValue().trim())) {
                formattedValue = String.valueOf(Float.parseFloat(param.getValue()));
            }
            break;
        case SHORT:
            if (param.getValue() != null && !"".equals(param.getValue().trim())) {
                formattedValue = String.valueOf(Short.parseShort(param.getValue()));
            }
            break;
        case BYTE:
            if (param.getValue() != null && !"".equals(param.getValue().trim())) {
                formattedValue = String.valueOf(Byte.parseByte(param.getValue()));
            }
            break;
        case INTEGER:
            if (param.getValue() != null && !"".equals(param.getValue().trim())) {
                formattedValue = String.valueOf(Integer.parseInt(param.getValue()));
            }
            break;
        default:
            formattedValue = param.getValue();
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

        textBox.setValidateOnBlur(true);
        textBox.addValidator(new Validator() {

            @Override
            public List<EditorError> validate(Editor editor, Object value) {
                setDirty(true);
                return validateTextBox(param, textBox, formGroup);
            }

            @Override
            public int getPriority() {
                return 0;
            }
        });
        textBox.validate();

        this.fields.add(formGroup);
    }

    private TextBoxBase createTextBox(final GwtConfigParameter param) {
        if (param.getDescription() != null && param.getDescription().contains("\u200B\u200B\u200B\u200B\u200B")) {
            final TextArea result = createTextArea();
            result.setHeight("120px");
            return result;
        }
        if (isTextArea(param)) {
            return createTextArea();
        }
        return new TextBox();
    }

    private boolean isTextArea(final GwtConfigParameter param) {
        if (param == null) {
            return false;
        }

        if (param.getType() != GwtConfigParameterType.STRING) {
            return false;
        }

        final String description = param.getDescription();

        if (description == null) {
            return false;
        }

        final String[] result = splitDescription(description);
        if (result.length < 2 || result[1] == null) {
            return false;
        }

        return result[1].equalsIgnoreCase("TextArea");
    }

    private String getDescription(final GwtConfigParameter param) {
        if (param == null || param.getDescription() == null) {
            return null;
        }

        final String[] result = splitDescription(param.getDescription());
        if (result.length > 0) {
            return result[0];
        }
        return "";
    }

    private static String[] splitDescription(final String description) {
        final int idx = description.lastIndexOf('|');
        if (idx < 0) {
            return new String[] { description };
        }
        if (idx < 1) {
            return new String[] { "", description.substring(idx + 1) };
        }
        return new String[] { description.substring(0, idx), description.substring(idx + 1) };
    }

    private TextArea createTextArea() {
        final TextArea textArea = new TextArea();
        textArea.setVisibleLines(10);
        textArea.setCharacterWidth(120);
        return textArea;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void renderPasswordField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
        this.valid.put(param.getId(), true);

        if (isFirstInstance) {
            FormLabel formLabel = new FormLabel();
            formLabel.setTitle(param.getId()); // title is used to hold ID
            formLabel.setText(param.getName());
            if (param.isRequired()) {
                formLabel.setShowRequiredIndicator(true);
            }
            formGroup.add(formLabel);

            InlineHelpBlock ihb = new InlineHelpBlock();
            ihb.setIconType(IconType.EXCLAMATION_TRIANGLE);
            formGroup.add(ihb);

            if (param.getDescription() != null) {
                HelpBlock toolTip = new HelpBlock();
                toolTip.setText(getDescription(param));
                formGroup.add(toolTip);
            }
        }

        final Input input = new Input();
        input.setType(InputType.PASSWORD);
        if (param.getValue() != null) {
            input.setText(param.getValue());
        } else {
            input.setText("");
        }

        if (param.getMin() != null && param.getMin().equals(param.getMax())) {
            input.setReadOnly(true);
            input.setEnabled(false);
        }

        input.setValidateOnBlur(true);
        input.addValidator(new Validator() {

            @Override
            public List<EditorError> validate(Editor editor, Object value) {
                setDirty(true);

                List<EditorError> result = new ArrayList<EditorError>();
                if ((input.getText() == null || "".equals(input.getText().trim())) && param.isRequired()) {
                    // null in required field
                    result.add(new BasicEditorError(input, input.getText(), MSGS.formRequiredParameter()));
                    ServicesUi.this.valid.put(param.getName(), false);
                } else {
                    param.setValue(input.getText());
                    ServicesUi.this.valid.put(param.getName(), true);
                }

                return result;
            }

            @Override
            public int getPriority() {
                return 0;
            }
        });

        formGroup.add(input);
        this.fields.add(formGroup);

    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void renderBooleanField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
        this.valid.put(param.getId(), true);

        if (isFirstInstance) {
            FormLabel formLabel = new FormLabel();
            formLabel.setTitle(param.getId()); // title is used to hold ID
            formLabel.setText(param.getName());
            if (param.isRequired()) {
                formLabel.setShowRequiredIndicator(true);
            }
            formGroup.add(formLabel);

            if (param.getDescription() != null) {
                HelpBlock toolTip = new HelpBlock();
                toolTip.setText(getDescription(param));
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

        this.fields.add(formGroup);
    }

    private void renderChoiceField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
        this.valid.put(param.getId(), true);

        if (isFirstInstance) {
            FormLabel formLabel = new FormLabel();
            formLabel.setTitle(param.getId()); // title is used to hold ID
            formLabel.setText(param.getName());
            if (param.isRequired()) {
                formLabel.setShowRequiredIndicator(true);
            }
            formGroup.add(formLabel);

            if (param.getDescription() != null) {
                HelpBlock toolTip = new HelpBlock();
                toolTip.setText(getDescription(param));
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
            if (param.getDefault() != null && oMap.get(current).equals(param.getDefault())) {
                listBox.setSelectedIndex(i);
            }

            if (param.getValue() != null && oMap.get(current).equals(param.getValue())) {
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

        this.fields.add(formGroup);
    }

    // Checks if all the fields are valid according to the Validate() method
    private boolean isValid() {
        // check if all fields are valid
        for (Map.Entry<String, Boolean> entry : this.valid.entrySet()) {
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

    // Validates all the entered values
    // TODO: validation should be done like in the old web ui: cleaner approach
    private List<EditorError> validateTextBox(GwtConfigParameter param, TextBoxBase box, FormGroup group) {
        group.setValidationState(ValidationState.NONE);
        this.valid.put(param.getName(), true);

        List<EditorError> result = new ArrayList<EditorError>();

        if (param.isRequired() && (box.getText().trim() == null || "".equals(box.getText().trim()))) {
            this.valid.put(param.getId(), false);
            result.add(new BasicEditorError(box, box.getText(), MSGS.formRequiredParameter()));
        }

        if (box.getText().trim() != null && !"".equals(box.getText().trim())) {
            if (param.getType().equals(GwtConfigParameterType.CHAR)) {
                if (box.getText().trim().length() > 1) {
                    this.valid.put(param.getId(), false);
                    result.add(new BasicEditorError(box, box.getText(),
                            MessageUtils.get(Integer.toString(box.getText().trim().length()), box.getText())));
                }
                if (param.getMin() != null && Character.valueOf(param.getMin().charAt(0)).charValue() > Character
                        .valueOf(box.getText().trim().charAt(0)).charValue()) {
                    this.valid.put(param.getId(), false);
                    result.add(new BasicEditorError(box, box.getText(), MessageUtils.get(CONFIG_MIN_VALUE,
                            Character.valueOf(param.getMin().charAt(0)).charValue())));
                }
                if (param.getMax() != null && Character.valueOf(param.getMax().charAt(0)).charValue() < Character
                        .valueOf(box.getText().trim().charAt(0)).charValue()) {
                    this.valid.put(param.getId(), false);
                    result.add(new BasicEditorError(box, box.getText(), MessageUtils.get(CONFIG_MAX_VALUE,
                            Character.valueOf(param.getMax().charAt(0)).charValue())));
                }
            } else if (param.getType().equals(GwtConfigParameterType.STRING)) {
                int configMinValue = 0;
                int configMaxValue = 255;
                try {
                    configMinValue = Integer.parseInt(param.getMin());
                } catch (NumberFormatException nfe) {
                    errorLogger.log(Level.SEVERE, "Configuration min value error! Applying UI defaults...");
                }
                try {
                    configMaxValue = Integer.parseInt(param.getMax());
                } catch (NumberFormatException nfe) {
                    errorLogger.log(Level.SEVERE, "Configuration max value error! Applying UI defaults...");
                }

                if (String.valueOf(box.getText().trim()).length() < configMinValue) {
                    this.valid.put(param.getName(), false);
                    result.add(new BasicEditorError(box, box.getText(),
                            MessageUtils.get(CONFIG_MIN_VALUE, configMinValue)));
                }
                if (String.valueOf(box.getText().trim()).length() > configMaxValue) {
                    this.valid.put(param.getName(), false);
                    result.add(new BasicEditorError(box, box.getText(),
                            MessageUtils.get(CONFIG_MAX_VALUE, configMaxValue)));
                }
            } else {
                try {
                    // numeric value
                    if (param.getType().equals(GwtConfigParameterType.FLOAT)) {
                        Float uiValue = Float.parseFloat(box.getText().trim());
                        if (param.getMin() != null && Float.parseFloat(param.getMin()) > uiValue) {
                            this.valid.put(param.getId(), false);
                            result.add(new BasicEditorError(box, box.getText(),
                                    MessageUtils.get(CONFIG_MIN_VALUE, param.getMin())));
                        }
                        if (param.getMax() != null && Float.parseFloat(param.getMax()) < uiValue) {
                            this.valid.put(param.getId(), false);
                            result.add(new BasicEditorError(box, box.getText(),
                                    MessageUtils.get(CONFIG_MAX_VALUE, param.getMax())));
                        }
                    } else if (param.getType().equals(GwtConfigParameterType.INTEGER)) {
                        Integer uiValue = Integer.parseInt(box.getText().trim());
                        if (param.getMin() != null && Integer.parseInt(param.getMin()) > uiValue) {
                            this.valid.put(param.getId(), false);
                            result.add(new BasicEditorError(box, box.getText(),
                                    MessageUtils.get(CONFIG_MIN_VALUE, param.getMin())));
                        }
                        if (param.getMax() != null && Integer.parseInt(param.getMax()) < uiValue) {
                            this.valid.put(param.getId(), false);
                            result.add(new BasicEditorError(box, box.getText(),
                                    MessageUtils.get(CONFIG_MAX_VALUE, param.getMax())));
                        }
                    } else if (param.getType().equals(GwtConfigParameterType.SHORT)) {
                        Short uiValue = Short.parseShort(box.getText().trim());
                        if (param.getMin() != null && Short.parseShort(param.getMin()) > uiValue) {
                            this.valid.put(param.getId(), false);
                            result.add(new BasicEditorError(box, box.getText(),
                                    MessageUtils.get(CONFIG_MIN_VALUE, param.getMin())));
                        }
                        if (param.getMax() != null && Short.parseShort(param.getMax()) < uiValue) {
                            this.valid.put(param.getId(), false);
                            result.add(new BasicEditorError(box, box.getText(),
                                    MessageUtils.get(CONFIG_MAX_VALUE, param.getMax())));
                        }
                    } else if (param.getType().equals(GwtConfigParameterType.BYTE)) {
                        Byte uiValue = Byte.parseByte(box.getText().trim());
                        if (param.getMin() != null && Byte.parseByte(param.getMin()) > uiValue) {
                            this.valid.put(param.getId(), false);
                            result.add(new BasicEditorError(box, box.getText(),
                                    MessageUtils.get(CONFIG_MIN_VALUE, param.getMin())));
                        }
                        if (param.getMax() != null && Byte.parseByte(param.getMax()) < uiValue) {
                            this.valid.put(param.getId(), false);
                            result.add(new BasicEditorError(box, box.getText(),
                                    MessageUtils.get(CONFIG_MAX_VALUE, param.getMax())));
                        }
                    } else if (param.getType().equals(GwtConfigParameterType.LONG)) {
                        Long uiValue = Long.parseLong(box.getText().trim());
                        if (param.getMin() != null && Long.parseLong(param.getMin()) > uiValue) {
                            this.valid.put(param.getId(), false);
                            result.add(new BasicEditorError(box, box.getText(),
                                    MessageUtils.get(CONFIG_MIN_VALUE, param.getMin())));
                        }
                        if (param.getMax() != null && Long.parseLong(param.getMax()) < uiValue) {
                            this.valid.put(param.getId(), false);
                            result.add(new BasicEditorError(box, box.getText(),
                                    MessageUtils.get(CONFIG_MAX_VALUE, param.getMax())));
                        }
                    } else if (param.getType().equals(GwtConfigParameterType.DOUBLE)) {
                        Double uiValue = Double.parseDouble(box.getText().trim());
                        if (param.getMin() != null && Double.parseDouble(param.getMin()) > uiValue) {
                            this.valid.put(param.getId(), false);
                            result.add(new BasicEditorError(box, box.getText(),
                                    MessageUtils.get(CONFIG_MIN_VALUE, param.getMin())));

                        }
                        if (param.getMax() != null && Double.parseDouble(param.getMax()) < uiValue) {
                            this.valid.put(param.getId(), false);
                            result.add(new BasicEditorError(box, box.getText(),
                                    MessageUtils.get(CONFIG_MAX_VALUE, param.getMax())));
                        }
                    }
                } catch (NumberFormatException e) {
                    this.valid.put(param.getId(), false);
                    result.add(new BasicEditorError(box, box.getText(), e.getLocalizedMessage()));
                }
            }
        }

        return result;
    }

    private void initInvalidDataModal() {
        this.incompleteFieldsModal.setTitle(MSGS.warning());
        this.incompleteFieldsText.setText(MSGS.formWithErrorsOrIncomplete());
    }
}
