/*******************************************************************************
 * Copyright (c) 2016, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.wires;

import org.eclipse.kura.web.client.util.DragSupport;
import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.constants.IconType;

public class WireComponentsAnchorListItem extends AnchorListItem {

    private final boolean isEmitter;
    private final boolean isReceiver;
    private Listener listener;

    public WireComponentsAnchorListItem(final String label, final String factoryPid, final boolean isEmitter,
            final boolean isReceiver) {
        super();
        this.isEmitter = isEmitter;
        this.isReceiver = isReceiver;
        super.setIcon(getFactoryIcon());
        super.setText(label);

        DragSupport drag = DragSupport.addIfSupported(this);

        if (drag != null) {
            drag.setListener(event -> event.setTextData(WiresPanelUi.FACTORY_PID_DROP_PREFIX + factoryPid));
        }

        super.addClickHandler(event -> {
            if (this.listener != null) {
                this.listener.onClick(factoryPid);
            }
        });
    }

    private IconType getFactoryIcon() {
        if (this.isEmitter && this.isReceiver) {
            return IconType.EXCHANGE;
        }
        if (this.isEmitter) {
            return IconType.LONG_ARROW_LEFT;
        }
        return IconType.LONG_ARROW_RIGHT;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public interface Listener {

        public void onClick(String factoryPid);
    }
}