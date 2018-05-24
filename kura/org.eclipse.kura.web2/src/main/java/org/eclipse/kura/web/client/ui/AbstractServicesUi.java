/*******************************************************************************
 * Copyright (c) 2016, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat
 *******************************************************************************/
package org.eclipse.kura.web.client.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.LabelComparator;
import org.eclipse.kura.web.client.util.MessageUtils;
import org.eclipse.kura.web.client.util.request.RequestQueue;
import org.eclipse.kura.web.shared.model.GwtCloudEntry;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;
import org.eclipse.kura.web.shared.model.GwtConfigParameter.GwtConfigParameterType;
import org.eclipse.kura.web.shared.service.GwtComponentService;
import org.eclipse.kura.web.shared.service.GwtComponentServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Anchor;
import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.DropDown;
import org.gwtbootstrap3.client.ui.DropDownHeader;
import org.gwtbootstrap3.client.ui.DropDownMenu;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.HelpBlock;
import org.gwtbootstrap3.client.ui.InlineHelpBlock;
import org.gwtbootstrap3.client.ui.InlineRadio;
import org.gwtbootstrap3.client.ui.Input;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.TextArea;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.base.TextBoxBase;
import org.gwtbootstrap3.client.ui.constants.IconType;
import org.gwtbootstrap3.client.ui.constants.InputType;
import org.gwtbootstrap3.client.ui.constants.Toggle;
import org.gwtbootstrap3.client.ui.constants.ValidationState;
import org.gwtbootstrap3.client.ui.form.error.BasicEditorError;
import org.gwtbootstrap3.client.ui.form.validator.Validator;
import org.gwtbootstrap3.client.ui.gwt.FlowPanel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorError;
import com.google.gwt.user.client.TakesValue;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public abstract class AbstractServicesUi extends Composite {

    private static final String TARGET_SUFFIX = ".target";
    private static final String CONFIG_MAX_VALUE = "configMaxValue";
    private static final String CONFIG_MIN_VALUE = "configMinValue";
    private static final String INVALID_VALUE = "invalidValue";

    protected static final Logger logger = Logger.getLogger(ServicesUi.class.getSimpleName());
    protected static final Logger errorLogger = Logger.getLogger("ErrorLogger");

    protected static final Messages MSGS = GWT.create(Messages.class);
    protected static final LabelComparator<String> DROPDOWN_LABEL_COMPARATOR = new LabelComparator<>();

    private final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);
    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);

    protected List<GwtCloudEntry> cloudInstancesBinder;

    protected GwtConfigComponent configurableComponent;

    protected HashMap<String, Boolean> valid = new HashMap<>();

    protected abstract void setDirty(boolean flag);

    protected abstract boolean isDirty();

    protected abstract void reset();

    protected abstract void renderForm();

    protected void renderMultiFieldConfigParameter(GwtConfigParameter mParam) {
        String value;
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
    protected void renderConfigParameter(GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
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

    // Checks if all the fields are valid according to the Validate() method
    protected boolean isValid() {
        // check if all fields are valid
        for (Map.Entry<String, Boolean> entry : this.valid.entrySet()) {
            if (!entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    // Field Render based on Type
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void renderTextField(final GwtConfigParameter param, boolean isFirstInstance, final FormGroup formGroup) {

        this.valid.put(param.getId(), true);

        if (isFirstInstance) {
            FormLabel formLabel = new FormLabel();
            formLabel.setText(param.getName());
            if (param.isRequired()) {
                formLabel.setShowRequiredIndicator(true);
            }
            formLabel.setTitle(param.getId());
            formGroup.add(formLabel);

            InlineHelpBlock ihb = new InlineHelpBlock();
            ihb.setIconType(IconType.EXCLAMATION_TRIANGLE);
            formGroup.add(ihb);

            HelpBlock tooltip = new HelpBlock();
            tooltip.setText(getDescription(param));
            formGroup.add(tooltip);
        }

        final TextBoxBase textBox = createTextBox(param);

        String formattedValue = "";

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

        textBox.addValidator(new Validator() {

            @Override
            public List<EditorError> validate(Editor editor, Object value) {
                return validateTextBox(param, formGroup);
            }

            @Override
            public int getPriority() {
                return 0;
            }
        });
        textBox.addKeyUpHandler(event -> {
            textBox.validate(true);
            setDirty(true);
        });

        if (param.getId().endsWith(TARGET_SUFFIX)) {
            String targetedService = param.getId().split(TARGET_SUFFIX)[0];

            DropDown dropDown = new DropDown();
            Anchor dropDownAnchor = new Anchor();
            dropDownAnchor.setText(MSGS.selectAvailableTargets());
            dropDownAnchor.setDataToggle(Toggle.DROPDOWN);

            dropDown.add(dropDownAnchor);

            final DropDownMenu dropDownMenu = new DropDownMenu();
            dropDownMenu.addStyleName("drop-down");

            DropDownHeader dropDownHeader = new DropDownHeader();
            dropDownHeader.setVisible(false);
            dropDownMenu.add(dropDownHeader);

            dropDown.add(dropDownMenu);

            RequestQueue.submit(context -> this.gwtXSRFService.generateSecurityToken(
                    context.callback(token -> AbstractServicesUi.this.gwtComponentService.getPidsFromTarget(token,
                            this.configurableComponent.getComponentId(), targetedService, context.callback(data -> {
                                if (data.isEmpty()) {
                                    dropDownHeader.setText(MSGS.noTargetsAvailable());
                                } else {
                                    dropDownHeader.setText(MSGS.targetsAvailable());
                                    data.forEach(targetEntry -> {
                                        AnchorListItem listItem = createListItem(textBox, targetEntry);
                                        dropDownMenu.add(listItem);
                                    });
                                }
                                dropDownHeader.setVisible(true);
                            })))));

            formGroup.add(dropDown);
        }
    }

    private AnchorListItem createListItem(final TextBoxBase textBox, String targetEntry) {
        AnchorListItem listItem = new AnchorListItem();
        listItem.setText("(kura.service.pid=" + targetEntry + ")");
        listItem.addClickHandler(event -> {
            Anchor eventGenerator = (Anchor) event.getSource();
            textBox.setText(eventGenerator.getText());
            setDirty(true);
        });
        return listItem;
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
    protected void renderPasswordField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
        this.valid.put(param.getId(), true);

        if (isFirstInstance) {
            FormLabel formLabel = new FormLabel();
            formLabel.setText(param.getName());
            if (param.isRequired()) {
                formLabel.setShowRequiredIndicator(true);
            }
            formLabel.setTitle(param.getId());
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

        input.addValidator(new Validator() {

            @Override
            public List<EditorError> validate(Editor editor, Object value) {

                List<EditorError> result = new ArrayList<>();
                if ((input.getText() == null || "".equals(input.getText().trim())) && param.isRequired()) {
                    // null in required field
                    result.add(new BasicEditorError(input, input.getText(), MSGS.formRequiredParameter()));
                    AbstractServicesUi.this.valid.put(param.getId(), false);
                } else {
                    param.setValue(input.getText());
                    AbstractServicesUi.this.valid.put(param.getId(), true);
                }

                return result;
            }

            @Override
            public int getPriority() {
                return 0;
            }
        });

        input.addKeyUpHandler(event -> {
            input.validate(true);
            setDirty(true);
        });

        formGroup.add(input);
    }
    
    protected void renderBooleanField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
        this.valid.put(param.getId(), true);

        if (isFirstInstance) {
            FormLabel formLabel = new FormLabel();
            formLabel.setText(param.getName());
            if (param.isRequired()) {
                formLabel.setShowRequiredIndicator(true);
            }
            formLabel.setTitle(param.getId());
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

        radioTrue.addValueChangeHandler(event -> {

            InlineRadio box = (InlineRadio) event.getSource();
            if (box.getValue()) {
                param.setValue(String.valueOf(true));
            }
            setDirty(true);
        });

        radioFalse.addValueChangeHandler(event -> {
            InlineRadio box = (InlineRadio) event.getSource();
            if (box.getValue()) {
                param.setValue(String.valueOf(false));
            }
            setDirty(true);
        });

        formGroup.add(flowPanel);
    }

    protected void renderChoiceField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
        this.valid.put(param.getId(), true);

        if (isFirstInstance) {
            FormLabel formLabel = new FormLabel();
            formLabel.setText(param.getName());
            if (param.isRequired()) {
                formLabel.setShowRequiredIndicator(true);
            }
            formLabel.setTitle(param.getId());
            formGroup.add(formLabel);

            if (param.getDescription() != null) {
                HelpBlock toolTip = new HelpBlock();
                toolTip.setText(getDescription(param));
                formGroup.add(toolTip);
            }
        }

        ListBox listBox = new ListBox();

        int i = 0;
        Map<String, String> oMap = param.getOptions();

        ArrayList<Entry<String, String>> sortedOptions = new ArrayList<>(oMap.entrySet());
        Collections.sort(sortedOptions, DROPDOWN_LABEL_COMPARATOR);

        final String selection = param.getValue() != null ? param.getValue() : param.getDefault();

        for (Entry<String, String> current : sortedOptions) {
            String label = current.getKey();
            String value = current.getValue();

            listBox.addItem(label, value);

            if (value.equals(selection)) {
                listBox.setSelectedIndex(i);
            }

            i++;
        }

        listBox.addChangeHandler(event -> {
            ListBox box = (ListBox) event.getSource();
            param.setValue(box.getSelectedValue());
            setDirty(true);
        });

        formGroup.add(listBox);
    }

    protected List<EditorError> validateTextBox(final GwtConfigParameter param, final FormGroup group) {

        group.setValidationState(ValidationState.NONE);

        final List<EditorError> editorErrors = new ArrayList<>();

        this.valid.put(param.getId(), true);

        int widgetCount = group.getWidgetCount();
        for (int i = 0; i < widgetCount; i++) {
            Widget widget = group.getWidget(i);
            if (!(widget instanceof TextBoxBase)) {
                continue;
            }

            final TextBoxBase currentText = (TextBoxBase) widget;

            final String text = currentText.getText();

            validate(param, text, errorDescription -> {
                AbstractServicesUi.this.valid.put(param.getId(), false);
                editorErrors.add(new BasicEditorError(currentText, text, errorDescription));
            });

        }

        return editorErrors;
    }

    // Validates all the entered values
    protected void validate(GwtConfigParameter param, String value, ValidationErrorConsumer consumer) {

        String trimmedValue = value.trim();
        final boolean isEmpty = trimmedValue.isEmpty();

        if (param.isRequired() && isEmpty) {
            consumer.addError(MSGS.formRequiredParameter());
        }

        if (!isEmpty) {
            try {
                switch (param.getType()) {
                case CHAR:
                    new CharGwtValue().setValue(trimmedValue, param, consumer);
                    break;
                case STRING:
                    new StringGwtValue().setValue(trimmedValue, param, consumer);
                    break;
                case FLOAT:
                    new FloatGwtValue().setValue(trimmedValue, param, consumer);
                    break;
                case INTEGER:
                    new IntegerGwtValue().setValue(trimmedValue, param, consumer);
                    break;
                case SHORT:
                    new ShortGwtValue().setValue(trimmedValue, param, consumer);
                    break;
                case BYTE:
                    new ByteGwtValue().setValue(trimmedValue, param, consumer);
                    break;
                case LONG:
                    new LongGwtValue().setValue(trimmedValue, param, consumer);
                    break;
                case DOUBLE:
                    new DoubleGwtValue().setValue(trimmedValue, param, consumer);
                    break;
                default:
                    consumer.addError("Unsupported data type: " + param.getType().toString());
                    break;
                }
            } catch (NumberFormatException e) {
                consumer.addError(MessageUtils.get(INVALID_VALUE, trimmedValue));
            }
        }
    }

    protected boolean isValid(GwtConfigParameter param, String value) {
        final TakesValue<Boolean> isValid = new TakesValue<Boolean>() {

            private boolean value = true;

            @Override
            public void setValue(Boolean value) {
                this.value = value;
            }

            @Override
            public Boolean getValue() {
                return this.value;
            }
        };

        validate(param, value, errorDescription -> isValid.setValue(false));

        return isValid.getValue();
    }

    protected void fillUpdatedConfiguration(FormGroup fg) {
        GwtConfigParameter param = new GwtConfigParameter();
        List<String> multiFieldValues = new ArrayList<>();
        int fgwCount = fg.getWidgetCount();
        for (int i = 0; i < fgwCount; i++) {
            logger.fine("Widget: " + fg.getClass());

            if (fg.getWidget(i) instanceof FormLabel) {
                param = this.configurableComponent.getParameter(fg.getWidget(i).getTitle());
                logger.fine("Param: " + fg.getTitle() + " -> " + param);

            } else if (fg.getWidget(i) instanceof ListBox || fg.getWidget(i) instanceof Input
                    || fg.getWidget(i) instanceof TextBoxBase) {

                if (param == null) {
                    errorLogger.warning("Missing parameter");
                    continue;
                }
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
        if (!multiFieldValues.isEmpty() && param != null) {
            param.setValues(multiFieldValues.toArray(new String[] {}));
        }
    }

    protected void restoreConfiguration(GwtConfigComponent originalConfig) {
        this.configurableComponent = new GwtConfigComponent();
        this.configurableComponent.setComponentDescription(originalConfig.getComponentDescription());
        this.configurableComponent.setComponentIcon(originalConfig.getComponentIcon());
        this.configurableComponent.setComponentId(originalConfig.getComponentId());
        this.configurableComponent.setComponentName(originalConfig.getComponentName());

        List<GwtConfigParameter> originalParameters = new ArrayList<>();
        for (GwtConfigParameter parameter : originalConfig.getParameters()) {
            GwtConfigParameter tempParam = new GwtConfigParameter(parameter);
            originalParameters.add(tempParam);
        }
        this.configurableComponent.setParameters(originalParameters);

        Map<String, Object> originalProperties = new HashMap<>();
        originalProperties.putAll(originalConfig.getProperties());
        this.configurableComponent.setProperties(originalProperties);
    }

    private String getUpdatedFieldConfiguration(GwtConfigParameter param, Widget wg) {
        Map<String, String> options = param.getOptions();
        if (options != null && !options.isEmpty()) {
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
                return wg instanceof Input ? ((Input) wg).getValue() : null;
            default:
                break;
            }
        }
        return null;
    }

    protected interface ValidationErrorConsumer {

        public void addError(String errorDescription);
    }

    private abstract class GwtValue<T> {

        T value;

        public abstract void setValue(String csvInput, GwtConfigParameter param, ValidationErrorConsumer consumer);
    }

    private class CharGwtValue extends GwtValue<Object> {

        @Override
        public void setValue(String csvInput, GwtConfigParameter param, ValidationErrorConsumer consumer) {
            this.value = csvInput.charAt(0);
            if (csvInput.length() > 1) {
                consumer.addError(MessageUtils.get(Integer.toString(csvInput.length()), csvInput));
            }
            if (param.getMin() != null && param.getMin().charAt(0) > csvInput.charAt(0)) {
                consumer.addError(MessageUtils.get(CONFIG_MIN_VALUE, param.getMin().charAt(0)));
            }
            if (param.getMax() != null && param.getMax().charAt(0) < csvInput.charAt(0)) {
                consumer.addError(MessageUtils.get(CONFIG_MAX_VALUE, param.getMax().charAt(0)));
            }
        }
    }

    private class StringGwtValue extends GwtValue<Object> {

        @Override
        public void setValue(String csvInput, GwtConfigParameter param, ValidationErrorConsumer consumer) {
            int configMinValue = 0;
            int configMaxValue = Integer.MAX_VALUE;
            this.value = csvInput;
            try {
                configMinValue = Integer.parseInt(param.getMin());
            } catch (NumberFormatException nfe) {
                errorLogger.log(Level.FINE, "Configuration min value error! Applying UI defaults...");
            }
            try {
                configMaxValue = Integer.parseInt(param.getMax());
            } catch (NumberFormatException nfe) {
                errorLogger.log(Level.FINE, "Configuration max value error! Applying UI defaults...");
            }

            if (String.valueOf(csvInput).length() < configMinValue) {
                consumer.addError(MessageUtils.get(CONFIG_MIN_VALUE, configMinValue));
            }
            if (String.valueOf(csvInput).length() > configMaxValue) {
                consumer.addError(MessageUtils.get(CONFIG_MAX_VALUE, configMaxValue));
            }
        }
    }

    private class LongGwtValue extends GwtValue<Object> {

        @Override
        public void setValue(String csvInput, GwtConfigParameter param, ValidationErrorConsumer consumer) {
            this.value = Long.parseLong(csvInput);
            if (param.getMin() != null && Long.parseLong(param.getMin()) > (Long) this.value) {
                consumer.addError(MessageUtils.get(CONFIG_MIN_VALUE, param.getMin()));
            }
            if (param.getMax() != null && Long.parseLong(param.getMax()) < (Long) this.value) {
                consumer.addError(MessageUtils.get(CONFIG_MAX_VALUE, param.getMax()));
            }
        }
    }

    private class DoubleGwtValue extends GwtValue<Object> {

        @Override
        public void setValue(String csvInput, GwtConfigParameter param, ValidationErrorConsumer consumer) {
            this.value = Double.parseDouble(csvInput);
            if (param.getMin() != null && Double.parseDouble(param.getMin()) > (Double) this.value) {
                consumer.addError(MessageUtils.get(CONFIG_MIN_VALUE, param.getMin()));
            }
            if (param.getMax() != null && Double.parseDouble(param.getMax()) < (Double) this.value) {
                consumer.addError(MessageUtils.get(CONFIG_MAX_VALUE, param.getMax()));
            }
        }
    }

    private class ByteGwtValue extends GwtValue<Object> {

        @Override
        public void setValue(String csvInput, GwtConfigParameter param, ValidationErrorConsumer consumer) {
            this.value = Byte.parseByte(csvInput);
            if (param.getMin() != null && Byte.parseByte(param.getMin()) > (Byte) this.value) {
                consumer.addError(MessageUtils.get(CONFIG_MIN_VALUE, param.getMin()));
            }
            if (param.getMax() != null && Byte.parseByte(param.getMax()) < (Byte) this.value) {
                consumer.addError(MessageUtils.get(CONFIG_MAX_VALUE, param.getMax()));
            }
        }
    }

    private class ShortGwtValue extends GwtValue<Object> {

        @Override
        public void setValue(String csvInput, GwtConfigParameter param, ValidationErrorConsumer consumer) {
            this.value = Short.parseShort(csvInput);
            if (param.getMin() != null && Short.parseShort(param.getMin()) > (Short) this.value) {
                consumer.addError(MessageUtils.get(CONFIG_MIN_VALUE, param.getMin()));
            }
            if (param.getMax() != null && Short.parseShort(param.getMax()) < (Short) this.value) {
                consumer.addError(MessageUtils.get(CONFIG_MAX_VALUE, param.getMax()));
            }
        }
    }

    private class IntegerGwtValue extends GwtValue<Object> {

        @Override
        public void setValue(String csvInput, GwtConfigParameter param, ValidationErrorConsumer consumer) {
            this.value = Integer.parseInt(csvInput);
            if (param.getMin() != null && Integer.parseInt(param.getMin()) > (Integer) this.value) {
                consumer.addError(MessageUtils.get(CONFIG_MIN_VALUE, param.getMin()));
            }
            if (param.getMax() != null && Integer.parseInt(param.getMax()) < (Integer) this.value) {
                consumer.addError(MessageUtils.get(CONFIG_MAX_VALUE, param.getMax()));
            }
        }
    }

    private class FloatGwtValue extends GwtValue<Object> {

        @Override
        public void setValue(String csvInput, GwtConfigParameter param, ValidationErrorConsumer consumer) {
            this.value = Float.parseFloat(csvInput);
            if (param.getMin() != null && Float.parseFloat(param.getMin()) > (Float) this.value) {
                consumer.addError(MessageUtils.get(CONFIG_MIN_VALUE, param.getMin()));
            }
            if (param.getMax() != null && Float.parseFloat(param.getMax()) < (Float) this.value) {
                consumer.addError(MessageUtils.get(CONFIG_MAX_VALUE, param.getMax()));
            }
        }
    }

}
