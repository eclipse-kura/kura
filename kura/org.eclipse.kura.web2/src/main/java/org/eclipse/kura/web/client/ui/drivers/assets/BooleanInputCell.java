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

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

public class BooleanInputCell extends AbstractCell<String> {

    public BooleanInputCell() {
        super("change");
    }

    @Override
    public void render(Context context, String value, SafeHtmlBuilder sb) {
        sb.append(new Checkbox("true".equals(value)));
    }

    @Override
    public void onBrowserEvent(Context context, Element parent, String value, NativeEvent event,
            ValueUpdater<String> valueUpdater) {
        if (valueUpdater == null) {
            return;
        }

        if ("change".equals(event.getType())) {
            final InputElement input = event.getEventTarget().cast();
            valueUpdater.update(input.isChecked() ? "true" : "false");
        }
    }

    private class Checkbox implements SafeHtml {

        private final boolean checked;

        public Checkbox(boolean checked) {
            this.checked = checked;
        }

        @Override
        public String asString() {
            return "<div style=\"text-align: center\"><input type=\"checkbox\" " + (checked ? "checked" : "")
                    + "></input></div>";
        }

    }

}
