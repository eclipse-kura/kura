/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.shared.model.GwtModelTest;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Well;
import org.gwtbootstrap3.client.ui.gwt.CellTable;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class TestPanelUi extends Composite {

    private static final Logger logger = Logger.getLogger(TestPanelUi.class.getSimpleName());
    private static StatusPanelUiUiBinder uiBinder = GWT.create(StatusPanelUiUiBinder.class);

    interface StatusPanelUiUiBinder extends UiBinder<Widget, TestPanelUi> {
    }

    private static final Messages MSG = GWT.create(Messages.class);

    private GwtSession currentSession;
    private EntryClassUi parent;

    private static List<GwtModelTest> testList = new ArrayList<>();

    @UiField
    Well testWell;
    @UiField
    Button testRefresh;
    @UiField
    CellTable<GwtModelTest> testGrid = new CellTable<GwtModelTest>();

    public TestPanelUi() {
        logger.log(Level.FINER, "Initializing TestPanelUi...");
        initWidget(uiBinder.createAndBindUi(this));
        // Set text for buttons
        this.testRefresh.setText(MSG.refresh());
        loadTestTable();
    }

    // get current session from UI parent
    public void setSession(GwtSession gwtBSSession) {
        this.currentSession = gwtBSSession;
    }

    public void loadTestTable() {
        this.testRefresh.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                testGrid.redraw();
                loadTestData();
            }
        });

        TextColumn<GwtModelTest> labelTest1 = new TextColumn<GwtModelTest>() {

            @Override
            public String getValue(GwtModelTest gwtModelTest) {
                return gwtModelTest.labelTest1;
            }
        };
        labelTest1.setCellStyleNames("status-table-row");
        this.testGrid.addColumn(labelTest1, "labelTest1");

        TextColumn<GwtModelTest> labelTest2 = new TextColumn<GwtModelTest>() {

            @Override
            public String getValue(GwtModelTest gwtModelTest) {
                return gwtModelTest.labelTest2;
            }
        };
        labelTest2.setCellStyleNames("status-table-row");
        this.testGrid.addColumn(labelTest2, "labelTest2");
        TextColumn<GwtModelTest> labelTest3 = new TextColumn<GwtModelTest>() {

            @Override
            public String getValue(GwtModelTest gwtModelTest) {
                return gwtModelTest.labelTest3;
            }
        };
        labelTest3.setCellStyleNames("status-table-row");
        this.testGrid.addColumn(labelTest3, "labelTest3");

        this.testWell.add(this.testGrid);
    }

    public void loadTestData() {
        List<GwtModelTest> listTest = new ArrayList<GwtModelTest>();
        listTest.add(new GwtModelTest("test1", "test1", "test1"));
        listTest.add(new GwtModelTest("test2", "test2", "test2"));

        this.testGrid.setRowCount(listTest.size(), true);
        this.testGrid.setRowData(0, listTest);
    }

    public void setParent(EntryClassUi parent) {
        this.parent = parent;
    }
}