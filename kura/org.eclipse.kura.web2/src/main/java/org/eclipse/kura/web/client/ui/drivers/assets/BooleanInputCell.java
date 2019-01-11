/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.web.client.ui.drivers.assets;

import java.util.Set;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

public class BooleanInputCell extends AbstractCell<String> {

    private CheckboxCell inner = new CheckboxCell(false, false);

    @Override
    public void render(Context context, String value, SafeHtmlBuilder sb) {
        inner.render(context, toBoolean(value), sb);
    }

    @Override
    public void onBrowserEvent(Context context, Element parent, String value, NativeEvent event,
            ValueUpdater<String> valueUpdater) {
        inner.onBrowserEvent(context, parent, toBoolean(value), event, new ValueUpdaterWrapper(valueUpdater));
    }

    @Override
    public Set<String> getConsumedEvents() {
        return inner.getConsumedEvents();
    }

    @Override
    public boolean dependsOnSelection() {
        return inner.dependsOnSelection();
    }

    @Override
    public boolean handlesSelection() {
        return inner.handlesSelection();
    }

    @Override
    public boolean isEditing(Context context, Element parent, String value) {
        return inner.isEditing(context, parent, toBoolean(value));
    }

    @Override
    public boolean resetFocus(Context context, Element parent, String value) {
        return inner.resetFocus(context, parent, toBoolean(value));
    }

    @Override
    public void setValue(Context context, Element parent, String value) {
        inner.setValue(context, parent, toBoolean(value));
    }

    private static boolean toBoolean(final String value) {
        return "true".equalsIgnoreCase(value);
    }

    private static final class ValueUpdaterWrapper implements ValueUpdater<Boolean> {

        private final ValueUpdater<String> wrapped;

        public ValueUpdaterWrapper(final ValueUpdater<String> wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public void update(Boolean value) {
            wrapped.update(Boolean.toString(value));
        }
    }
}
