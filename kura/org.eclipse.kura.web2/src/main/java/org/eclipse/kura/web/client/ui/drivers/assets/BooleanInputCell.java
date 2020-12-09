/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/

package org.eclipse.kura.web.client.ui.drivers.assets;

import java.util.HashSet;
import java.util.Set;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

public class BooleanInputCell extends AbstractCell<String> {

    private final CheckboxCell inner = new CheckboxCell(false, false);

    @Override
    public void render(Context context, String value, SafeHtmlBuilder sb) {
        this.inner.render(context, toBoolean(value), sb);
    }

    @Override
    public void onBrowserEvent(Context context, Element parent, String value, NativeEvent event,
            ValueUpdater<String> valueUpdater) {
        final NativeEvent forwarded;

        if (BrowserEvents.CLICK.contentEquals(event.getType())) {
            forwarded = Document.get().createChangeEvent();
        } else {
            forwarded = event;
        }

        this.inner.onBrowserEvent(context, parent, toBoolean(value), forwarded, new ValueUpdaterWrapper(valueUpdater));
    }

    @Override
    public Set<String> getConsumedEvents() {
        final HashSet<String> events = new HashSet<>(this.inner.getConsumedEvents());
        events.add(BrowserEvents.CLICK);
        return events;
    }

    @Override
    public boolean dependsOnSelection() {
        return this.inner.dependsOnSelection();
    }

    @Override
    public boolean handlesSelection() {
        return this.inner.handlesSelection();
    }

    @Override
    public boolean isEditing(Context context, Element parent, String value) {
        return this.inner.isEditing(context, parent, toBoolean(value));
    }

    @Override
    public boolean resetFocus(Context context, Element parent, String value) {
        return this.inner.resetFocus(context, parent, toBoolean(value));
    }

    @Override
    public void setValue(Context context, Element parent, String value) {
        this.inner.setValue(context, parent, toBoolean(value));
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
            this.wrapped.update(Boolean.toString(value));
        }
    }
}
