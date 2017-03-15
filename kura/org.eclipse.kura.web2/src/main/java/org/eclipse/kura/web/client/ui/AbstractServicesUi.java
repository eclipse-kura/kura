/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.MessageUtils;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;
import org.eclipse.kura.web.shared.model.GwtConfigParameter.GwtConfigParameterType;
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
import org.gwtbootstrap3.client.ui.constants.ValidationState;
import org.gwtbootstrap3.client.ui.form.error.BasicEditorError;
import org.gwtbootstrap3.client.ui.form.validator.Validator;
import org.gwtbootstrap3.client.ui.gwt.FlowPanel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorError;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public abstract class AbstractServicesUi extends Composite {

    private static final String CONFIG_MAX_VALUE = "configMaxValue";
    private static final String CONFIG_MIN_VALUE = "configMinValue";
    private static final String INVALID_VALUE = "invalidValue";

    protected static final Logger logger = Logger.getLogger(ServicesUi.class.getSimpleName());
    protected static final Logger errorLogger = Logger.getLogger("ErrorLogger");

    protected static final Messages MSGS = GWT.create(Messages.class);
    private static final DropdownLabelComparator DROPDOWN_LABEL_COMPARATOR = new DropdownLabelComparator();

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

        input.setValidateOnBlur(true);
        input.addValidator(new Validator() {

            @Override
            public List<EditorError> validate(Editor editor, Object value) {
                setDirty(true);

                List<EditorError> result = new ArrayList<EditorError>();
                if ((input.getText() == null || "".equals(input.getText().trim())) && param.isRequired()) {
                    // null in required field
                    result.add(new BasicEditorError(input, input.getText(), MSGS.formRequiredParameter()));
                    AbstractServicesUi.this.valid.put(param.getName(), false);
                } else {
                    param.setValue(input.getText());
                    AbstractServicesUi.this.valid.put(param.getName(), true);
                }

                return result;
            }

            @Override
            public int getPriority() {
                return 0;
            }
        });

        formGroup.add(input);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
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

        ArrayList<Entry<String, String>> sortedOptions = new ArrayList<Entry<String, String>>(oMap.entrySet());
        Collections.sort(sortedOptions, DROPDOWN_LABEL_COMPARATOR);

        for (Entry<String, String> current : sortedOptions) {
            String label = current.getKey();
            String value = current.getValue();
            listBox.addItem(label, value);
            if (param.getDefault() != null && value.equals(param.getDefault())) {
                listBox.setSelectedIndex(i);
            }

            if (param.getValue() != null && value.equals(param.getValue())) {
                listBox.setSelectedIndex(i);
            }
            i++;
        }

        listBox.addChangeHandler(new ChangeHandler() {

            @Override
            public void onChange(ChangeEvent event) {
                setDirty(true);
                ListBox box = (ListBox) event.getSource();
                param.setValue(box.getSelectedValue());
            }
        });

        formGroup.add(listBox);
    }

    // Validates all the entered values
    // TODO: validation should be done like in the old web ui: cleaner approach
    private List<EditorError> validateTextBox(GwtConfigParameter param, TextBoxBase box, FormGroup group) {
        group.setValidationState(ValidationState.NONE);
        this.valid.put(param.getName(), true);

        List<EditorError> result = new ArrayList<>();

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
                if (param.getMin() != null && param.getMin().charAt(0) > box.getText().trim().charAt(0)) {
                    this.valid.put(param.getId(), false);
                    result.add(new BasicEditorError(box, box.getText(),
                            MessageUtils.get(CONFIG_MIN_VALUE, param.getMin().charAt(0))));
                }
                if (param.getMax() != null && param.getMax().charAt(0) < box.getText().trim().charAt(0)) {
                    this.valid.put(param.getId(), false);
                    result.add(new BasicEditorError(box, box.getText(),
                            MessageUtils.get(CONFIG_MAX_VALUE, param.getMax().charAt(0))));
                }
            } else if (param.getType().equals(GwtConfigParameterType.STRING)) {
                int configMinValue = 0;
                int configMaxValue = Integer.MAX_VALUE;
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
                    result.add(new BasicEditorError(box, box.getText(),
                            MessageUtils.get(INVALID_VALUE, box.getText().trim())));
                }
            }
        }

        return result;
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

    private enum ComparatorState {
        COMPARE_STRING,
        COMPARE_NUMBER,
    }

    private static class DropdownLabelComparator implements Comparator<Entry<String, String>> {

        private int getNumberEnd(String s, int index) {

            while (index < s.length()) {
                if (!Character.isDigit(s.charAt(index))) {
                    return index;
                }
                index++;
            }

            return index;
        }

        @Override
        public int compare(Entry<String, String> e1, Entry<String, String> e2) {
            String o1 = e1.getKey();
            String o2 = e2.getKey();

            ComparatorState state = ComparatorState.COMPARE_STRING;
            int i1 = 0;
            int i2 = 0;

            while (i1 < o1.length() && i2 < o2.length()) {
                final char c1 = o1.charAt(i1);
                final char c2 = o2.charAt(i2);

                if (state == ComparatorState.COMPARE_STRING) {

                    if (Character.isDigit(c1) && Character.isDigit(c2)) {
                        state = ComparatorState.COMPARE_NUMBER;
                        continue;
                    }

                    if (c1 < c2) {
                        return -1;
                    }
                    if (c1 > c2) {
                        return 1;
                    }

                    i1++;
                    i2++;

                } else {

                    final int s1 = i1;
                    final int s2 = i2;

                    i1 = getNumberEnd(o1, s1);
                    i2 = getNumberEnd(o2, s2);

                    int n1 = Integer.parseInt(o1.substring(s1, i1));
                    int n2 = Integer.parseInt(o2.substring(s2, i2));

                    if (n1 < n2) {
                        return -1;
                    }
                    if (n1 > n2) {
                        return 1;
                    }

                    state = ComparatorState.COMPARE_STRING;
                }
            }

            return 0;
        }

    }
}
