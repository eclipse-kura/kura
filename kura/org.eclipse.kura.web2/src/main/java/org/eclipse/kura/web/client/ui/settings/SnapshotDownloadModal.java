/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.client.ui.settings;

import java.util.ArrayList;
import java.util.List;

import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.CheckBox;
import org.gwtbootstrap3.client.ui.Modal;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class SnapshotDownloadModal extends Composite {

    private static SnapshotDownloadModalUiBinder uiBinder = GWT.create(SnapshotDownloadModalUiBinder.class);
    // private static final Logger logger = Logger.getLogger(SnapshotDownloadModal.class.getSimpleName());

    interface SnapshotDownloadModalUiBinder extends UiBinder<Widget, SnapshotDownloadModal> {
    }

    private List<String> snapshotConfigs = new ArrayList<>();

    @UiField
    ScrollPanel scrollPanel;
    @UiField
    Modal modal;
    @UiField
    Button downloadXml;
    @UiField
    Button downloadJson;

    VerticalPanel pidPanel = new VerticalPanel();

    private Listener listener = format -> {
    };

    public SnapshotDownloadModal() {

        initWidget(uiBinder.createAndBindUi(this));

        initPidList();

        initScrollPanel();

        this.downloadJson.addClickHandler(e -> {
            this.modal.hide();
            this.listener.onDonwload("TEST");
        });

        this.downloadXml.addClickHandler(e -> {
            this.modal.hide();
            this.listener.onDonwload("TEST");
        });
    }

    private void initPidList() {

        this.pidPanel.clear();

        this.snapshotConfigs.forEach(pid -> {
            CheckBox box = new CheckBox(pid);
            this.pidPanel.add(box);
        });
    }

    private void initScrollPanel() {
        this.scrollPanel.setAlwaysShowScrollBars(false);
        this.scrollPanel.setHeight("500px");
        this.scrollPanel.clear();
        this.scrollPanel.add(pidPanel);
    }

    public void show(final Listener listener) {
        this.listener = listener;
        initPidList();
        this.modal.show();
    }

    public interface Listener {

        public void onDonwload(String format);
    }

    public void setSnapshotConfigurations(List<String> configs) {
        this.snapshotConfigs = configs;

    }

}
