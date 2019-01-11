/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *  
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.wires;

import com.google.gwt.cell.client.AbstractInputCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safecss.shared.SafeStyles;
import com.google.gwt.safecss.shared.SafeStylesUtils;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

/**
 * A custom Input Cell which is inherently used to validate its data based on
 * validation provided from Metatype
 */
public final class ValidationInputCell extends AbstractInputCell<String, ValidationData> {

    interface ValidationInputTemplate extends SafeHtmlTemplates {

        @Template("<input type=\"text\" value=\"{0}\" style=\"{1}\" class=\"{2}\" tabindex=\"-1\"/>")
        SafeHtml input(String value, SafeStyles color, String cssClassName);
    }

    private static final String CHANGE_EVENT = "change";
    private static final String NONPENDING_COLOR = "black";
    private static final String NONVALIDATED_COLOR = "red";
    private static final String NONVALIDATED_CSS_CLASS_NAME = "error-text-box";
    private static final String VALIDATED_COLOR = "blue";
    private static final String VALIDATED_CSS_CLASS_NAME = "noerror-text-box";

    private ValidationInputTemplate validationTemplate;

    public ValidationInputCell() {
        super(CHANGE_EVENT);
        if (this.validationTemplate == null) {
            this.validationTemplate = GWT.create(ValidationInputTemplate.class);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onBrowserEvent(final Context context, final Element parent, final String value, final NativeEvent event,
            final ValueUpdater<String> valueUpdater) {
        super.onBrowserEvent(context, parent, value, event, valueUpdater);
        final Element target = event.getEventTarget().cast();
        if (!parent.getFirstChildElement().isOrHasChild(target)) {
            return;
        }
        final Object key = context.getKey();
        ValidationData viewData = this.getViewData(key);
        final String eventType = event.getType();
        if (CHANGE_EVENT.equals(eventType)) {
            final InputElement input = parent.getFirstChild().cast();
            input.getStyle().setColor(VALIDATED_COLOR);
            if (viewData == null) {
                viewData = new ValidationData();
                this.setViewData(key, viewData);
            }
            final String newValue = input.getValue();
            viewData.setValue(newValue);
            this.finishEditing(parent, newValue, key, valueUpdater);
            if (valueUpdater != null) {
                valueUpdater.update(newValue);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void onEnterKeyDown(final Context context, final Element parent, final String value,
            final NativeEvent event, final ValueUpdater<String> valueUpdater) {
        final Element target = event.getEventTarget().cast();
        if (this.getInputElement(parent).isOrHasChild(target)) {
            this.finishEditing(parent, value, context.getKey(), valueUpdater);
        } else {
            super.onEnterKeyDown(context, parent, value, event, valueUpdater);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void render(final Context context, String value, final SafeHtmlBuilder shb) {
        final Object key = context.getKey();
        ValidationData validationViewData = this.getViewData(key);
        if ((validationViewData != null) && validationViewData.getValue().equals(value)) {
            this.clearViewData(key);
            validationViewData = null;
        }
        if (value == null) {
            value = "";
        }
        final String processingValue = (validationViewData == null) ? null : validationViewData.getValue();
        final boolean invalid = (validationViewData == null) ? false : validationViewData.isInvalid();
        final String color = processingValue != null ? (invalid ? NONVALIDATED_COLOR : VALIDATED_COLOR)
                : NONPENDING_COLOR;
        final SafeStyles safeColor = SafeStylesUtils.fromTrustedString("color: " + color + ";");
        shb.append(this.validationTemplate.input(processingValue != null ? processingValue : value, safeColor,
                invalid ? NONVALIDATED_CSS_CLASS_NAME : VALIDATED_CSS_CLASS_NAME));
    }
}