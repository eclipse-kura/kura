/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.drivers.assets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.AlertDialog;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.ui.drivers.assets.AssetModel.ChannelModel;
import org.eclipse.kura.web.shared.AssetConstants;
import org.eclipse.kura.web.shared.model.GwtChannelRecord;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.PanelBody;
import org.gwtbootstrap3.client.ui.gwt.CellTable;

import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.cell.client.TextInputCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DefaultHeaderOrFooterBuilder;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;

public class AssetDataUi extends Composite {

    private static AssetDataUiBinder uiBinder = GWT.create(AssetDataUiBinder.class);

    interface AssetDataUiBinder extends UiBinder<Widget, AssetDataUi> {
    }

    protected static final Messages MSGS = GWT.create(Messages.class);

    private static final int MAXIMUM_PAGE_SIZE = 5;

    private final ListDataProvider<AssetModel.ChannelModel> channelsDataProvider = new ListDataProvider<>();
    private final SingleSelectionModel<AssetModel.ChannelModel> selectionModel = new SingleSelectionModel<>();

    private AssetModel model;
    private Map<String, GwtChannelRecord> channelValues = new HashMap<>();

    private boolean dirty;

    @UiField
    PanelBody configurationPanelBody;
    @UiField
    Button applyDataChanges;
    @UiField
    Button refreshData;
    @UiField
    CellTable<AssetModel.ChannelModel> assetDataTable;
    @UiField
    SimplePager channelPager;
    @UiField
    AlertDialog alertDialog;

    public AssetDataUi(AssetModel model) {
        initWidget(uiBinder.createAndBindUi(this));

        this.channelPager.setPageSize(MAXIMUM_PAGE_SIZE);
        this.channelPager.setDisplay(this.assetDataTable);
        this.assetDataTable.setSelectionModel(this.selectionModel);
        this.channelsDataProvider.addDataDisplay(this.assetDataTable);

        this.model = model;

        initButtons();
        initTable();
    }

    public void setModel(AssetModel model) {
        this.model = model;
    }

    private void initButtons() {
        this.applyDataChanges.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                write();
            }
        });

        this.refreshData.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                renderForm();
            }
        });
        this.applyDataChanges.setEnabled(false);
    }

    private void initTable() {
        this.assetDataTable.setHeaderBuilder(
                new DefaultHeaderOrFooterBuilder<AssetModel.ChannelModel>(this.assetDataTable, false));

        final Column<AssetModel.ChannelModel, String> c = new Column<AssetModel.ChannelModel, String>(new TextCell()) {

            @Override
            public String getValue(final AssetModel.ChannelModel object) {
                return object.getChannelName();
            }

        };

        this.assetDataTable.addColumn(c, new TextHeader(MSGS.wiresChannelName()));

        final Column<AssetModel.ChannelModel, String> c2 = new Column<AssetModel.ChannelModel, String>(new TextCell()) {

            @Override
            public String getValue(final AssetModel.ChannelModel object) {
                return object.getValue(AssetConstants.TYPE.value());
            }
        };

        this.assetDataTable.addColumn(c2, new TextHeader(MSGS.wiresChannelOperation()));

        final Column<AssetModel.ChannelModel, String> c3 = new Column<AssetModel.ChannelModel, String>(new TextCell()) {

            @Override
            public String getValue(final AssetModel.ChannelModel object) {
                return object.getValue(AssetConstants.VALUE_TYPE.value());
            }
        };

        this.assetDataTable.addColumn(c3, new TextHeader(MSGS.wiresChannelValueType()));

        final TextInputCell cell = new TextInputCell();
        final Column<AssetModel.ChannelModel, String> c4 = new Column<AssetModel.ChannelModel, String>(cell) {

            @Override
            public void onBrowserEvent(Context context, Element elem, ChannelModel object, NativeEvent event) {
                if (!"READ".equals(object.getValue(AssetConstants.TYPE.value()))) {
                    super.onBrowserEvent(context, elem, object, event);
                }
            }

            @Override
            public String getCellStyleNames(Context context, ChannelModel object) {
                final GwtChannelRecord result = channelValues.get(object.getChannelName());
                if (result == null) {
                    return null;
                }
                final String value = result.getValue();
                if (value == null) {
                    return "cell-not-valid cell-readonly";
                }
                return null;
            }

            @Override
            public void render(Context context, AssetModel.ChannelModel object, SafeHtmlBuilder sb) {
                if ("false".equals(object.getValue(AssetConstants.ENABLED.value()))) {
                    sb.appendEscaped("This channel is disabled");
                } else if ("READ".equals(object.getValue(AssetConstants.TYPE.value()))) {
                    sb.appendEscaped(getValue(object));
                } else {
                    super.render(context, object, sb);
                }
            }

            @Override
            public String getValue(final AssetModel.ChannelModel object) {
                final GwtChannelRecord result = channelValues.get(object.getChannelName());
                if (result == null) {
                    return "";
                }
                final String value = result.getValue();
                if (value == null) {
                    return "Read Error!";
                }
                return value;
            }
        };

        c4.setFieldUpdater(new FieldUpdater<AssetModel.ChannelModel, String>() {

            @Override
            public void update(final int index, final AssetModel.ChannelModel object, final String value) {
                setDirty(true);
                GwtChannelRecord result = channelValues.get(object.getChannelName());
                if (result == null) {
                    result = createWriteRecord(object);
                    channelValues.put(object.getChannelName(), result);
                }
                result.setValue(value);
                AssetDataUi.this.assetDataTable.redraw();
            }
        });

        this.assetDataTable.addColumn(c4, new TextHeader(MSGS.devicePropValue()));
    }

    private GwtChannelRecord createWriteRecord(AssetModel.ChannelModel channel) {
        final GwtChannelRecord result = new GwtChannelRecord();
        result.setName(channel.getChannelName());
        result.setValueType(channel.getValue(AssetConstants.VALUE_TYPE.value()));
        return result;
    }

    private void write() {
        if (isDirty()) {

            final ArrayList<GwtChannelRecord> writeRecords = new ArrayList<>();
            for (AssetModel.ChannelModel channel : model.getChannels()) {
                if (channel.getValue(AssetConstants.TYPE.value()).contains("WRITE")) {
                    final GwtChannelRecord record = channelValues.get(channel.getChannelName());
                    if (record == null) {
                        continue;
                    }
                    writeRecords.add(record);
                }
            }

            if (writeRecords.isEmpty()) {
                return;
            }

            alertDialog.show(MSGS.deviceConfigConfirmation(model.getAssetPid()), new AlertDialog.Listener() {

                @Override
                public void onConfirm() {
                    DriversAndAssetsRPC.write(model.getAssetPid(), writeRecords,
                            new DriversAndAssetsRPC.Callback<Void>() {

                                @Override
                                public void onSuccess(Void result) {
                                    AssetDataUi.this.setDirty(false);
                                }

                            });
                }
            });
        }
    }

    public void setDirty(boolean flag) {
        this.dirty = flag;
        if (this.dirty) {
            this.applyDataChanges.setEnabled(true);
        }
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public void renderForm() {
        AssetDataUi.this.channelsDataProvider.getList().clear();
        EntryClassUi.showWaitModal();
        DriversAndAssetsRPC.readAllChannels(model.getAssetPid(),
                new DriversAndAssetsRPC.Callback<List<GwtChannelRecord>>() {

                    @Override
                    public void onSuccess(List<GwtChannelRecord> result) {
                        channelValues.clear();

                        for (GwtChannelRecord channelValue : result) {
                            channelValues.put(channelValue.getName(), channelValue);
                        }

                        AssetDataUi.this.channelsDataProvider.getList().addAll(model.getChannels());
                        AssetDataUi.this.channelsDataProvider.refresh();

                        int size = AssetDataUi.this.channelsDataProvider.getList().size();
                        AssetDataUi.this.assetDataTable.setVisibleRange(0, size);
                        AssetDataUi.this.assetDataTable.redraw();

                        AssetDataUi.this.applyDataChanges.setEnabled(false);
                    }
                });
    }

}
