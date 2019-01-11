/*******************************************************************************
 * Copyright (c) 2017, 2019 Eurotech and/or its affiliates
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.AlertDialog;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.ui.drivers.assets.AssetModel.ChannelModel;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.AssetConstants;
import org.eclipse.kura.web.shared.model.GwtChannelRecord;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.PanelBody;
import org.gwtbootstrap3.client.ui.gwt.CellTable;

import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.cell.client.TextInputCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
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

    private final Map<String, GwtChannelRecord> channelValues = new HashMap<>();
    private final Set<String> modifiedWriteChannels = new HashSet<>();
    private final TextInputCell valuesCell = new TextInputCell();

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
        this.applyDataChanges.addClickHandler(event -> write());

        this.refreshData.addClickHandler(event -> renderForm());
        this.applyDataChanges.setEnabled(false);
    }

    private void initTable() {
        this.assetDataTable.setHeaderBuilder(
                new DefaultHeaderOrFooterBuilder<AssetModel.ChannelModel>(this.assetDataTable, false));

        this.assetDataTable.addColumn(new StaticColumn(AssetConstants.NAME.value()),
                new TextHeader(MSGS.wiresChannelName()));
        this.assetDataTable.addColumn(new StaticColumn(AssetConstants.TYPE.value()),
                new TextHeader(MSGS.wiresChannelOperation()));
        this.assetDataTable.addColumn(new StaticColumn(AssetConstants.VALUE_TYPE.value()),
                new TextHeader(MSGS.wiresChannelValueType()));

        final Column<AssetModel.ChannelModel, String> statusColumn = new Column<AssetModel.ChannelModel, String>(
                new StatusCell()) {

            @Override
            public void onBrowserEvent(Context context, Element elem, ChannelModel object, NativeEvent event) {
                if (getChannelStatus(object) == ChannelStatus.FAILURE) {
                    final GwtChannelRecord record = channelValues.get(object.getChannelName());
                    showFailureDetails(record);
                }
            }

            @Override
            public String getValue(final AssetModel.ChannelModel object) {
                return getChannelStatus(object).getLabel();
            }

            @Override
            public String getCellStyleNames(Context context, ChannelModel object) {
                return getChannelStatus(object).getCellStyle();
            }

            @Override
            public void render(final Context context, final ChannelModel object, final SafeHtmlBuilder sb) {
                final ChannelStatus status = getChannelStatus(object);

                sb.append(() -> "<i class=\"fa assets-status-icon " + status.getIconStyle() + "\"></i><span>"
                        + SafeHtmlUtils.htmlEscape(getValue(object)) + "</span>");
            }
        };

        this.assetDataTable.addColumn(statusColumn, new TextHeader(MSGS.wiresChannelStatus()));

        final Column<AssetModel.ChannelModel, String> valueColumn = new Column<AssetModel.ChannelModel, String>(
                valuesCell) {

            @Override
            public void onBrowserEvent(Context context, Element elem, ChannelModel object, NativeEvent event) {
                if (!"READ".equals(object.getValue(AssetConstants.TYPE.value()))) {
                    super.onBrowserEvent(context, elem, object, event);
                }
            }

            @Override
            public String getCellStyleNames(Context context, ChannelModel object) {
                if (getChannelStatus(object) == ChannelStatus.FAILURE) {
                    return "cell-readonly";
                }
                return null;
            }

            @Override
            public String getValue(final AssetModel.ChannelModel object) {
                if (getChannelStatus(object) == ChannelStatus.SUCCESS) {
                    final GwtChannelRecord result = channelValues.get(object.getChannelName());
                    return result.getValue();
                }
                return "Not available";
            }

            @Override
            public void render(Context context, ChannelModel object, SafeHtmlBuilder sb) {
                if ("READ".equals(object.getValue(AssetConstants.TYPE.value()))) {
                    sb.appendEscaped(getValue(object));
                    return;
                }
                if (!isDirty(object.getChannelName())) {
                    valuesCell.clearViewData(context.getKey());
                }
                super.render(context, object, sb);
            }
        };

        valueColumn.setFieldUpdater((index, object, value) -> {
            final String channelName = object.getChannelName();

            GwtChannelRecord result = createWriteRecord(object);
            result.setValue(value);
            channelValues.put(channelName, result);

            markAsDirty(channelName);

            AssetDataUi.this.assetDataTable.redraw();
        });

        this.assetDataTable.addColumn(valueColumn, new TextHeader(MSGS.devicePropValue()));
    }

    private static void showFailureDetails(final GwtChannelRecord record) {
        record.setUnescaped(true);
        String reason = record.getExceptionMessage();
        record.setUnescaped(false);

        if (reason == null || reason.trim().isEmpty()) {
            reason = "unknown";
        }

        FailureHandler.showErrorMessage("Channel failure details", "Reason: " + reason,
                record.getExceptionStackTrace());
    }

    private GwtChannelRecord createWriteRecord(AssetModel.ChannelModel channel) {
        final GwtChannelRecord result = new GwtChannelRecord();
        result.setName(channel.getChannelName());
        result.setValueType(channel.getValue(AssetConstants.VALUE_TYPE.value()));
        return result;
    }

    private void write() {
        if (!isDirty()) {
            return;
        }

        final ArrayList<GwtChannelRecord> writeRecords = new ArrayList<>();

        for (final String channelName : modifiedWriteChannels) {
            final GwtChannelRecord record = channelValues.get(channelName);
            if (record == null) {
                continue;
            }
            writeRecords.add(record);
        }

        if (writeRecords.isEmpty()) {
            return;
        }

        alertDialog.show(MSGS.driversAssetsWriteConfirm(model.getAssetPid()), () -> DriversAndAssetsRPC.write(model.getAssetPid(), writeRecords,
                result -> {
                    final List<GwtChannelRecord> records = result.getRecords();

                    if (records != null) {
                        AssetDataUi.this.setDirty(false);
                        for (GwtChannelRecord channelRecord : records) {
                            channelValues.put(channelRecord.getName(), channelRecord);
                        }
                        AssetDataUi.this.channelsDataProvider.refresh();
                        AssetDataUi.this.assetDataTable.redraw();
                    } else {
                        FailureHandler.showErrorMessage("Channel operation failed",
                                result.getExceptionMessage(), result.getStackTrace());
                    }
                }));
    }

    private boolean isDirty(final String channelName) {
        return modifiedWriteChannels.contains(channelName);
    }

    private void markAsDirty(final String channelName) {
        AssetDataUi.this.modifiedWriteChannels.add(channelName);
        AssetDataUi.this.applyDataChanges.setEnabled(true);
    }

    public void setDirty(boolean flag) {
        if (!flag) {
            this.modifiedWriteChannels.clear();
        }
        if (this.isDirty()) {
            this.applyDataChanges.setEnabled(true);
        }
    }

    public boolean isDirty() {
        return !this.modifiedWriteChannels.isEmpty();
    }

    public void renderForm() {

        this.setDirty(false);
        this.channelValues.clear();
        this.applyDataChanges.setEnabled(false);
        this.channelsDataProvider.getList().clear();
        this.channelsDataProvider.refresh();

        EntryClassUi.showWaitModal();
        DriversAndAssetsRPC.readAllChannels(model.getAssetPid(),
                result -> {
                    final List<GwtChannelRecord> records = result.getRecords();

                    if (records != null) {
                        for (final GwtChannelRecord record : records) {
                            channelValues.put(record.getName(), record);
                        }
                        AssetDataUi.this.channelsDataProvider.getList().addAll(model.getChannels());
                        AssetDataUi.this.channelsDataProvider.refresh();

                        int size = AssetDataUi.this.channelsDataProvider.getList().size();
                        AssetDataUi.this.assetDataTable.setVisibleRange(0, size);

                        AssetDataUi.this.assetDataTable.redraw();
                    } else {
                        FailureHandler.showErrorMessage("Channel operation failed", result.getExceptionMessage(),
                                result.getStackTrace());
                    }
                });
    }

    private ChannelStatus getChannelStatus(final ChannelModel model) {
        final String channelName = model.getChannelName();
        final GwtChannelRecord record = channelValues.get(model.getChannelName());

        if ("false".equals(model.getValue(AssetConstants.ENABLED.value()))) {
            return ChannelStatus.DISABLED;
        } else if (modifiedWriteChannels.contains(channelName)) {
            return ChannelStatus.DIRTY;
        } else if (record == null) {
            return ChannelStatus.UNKNOWN;
        } else if (record.getValue() == null) {
            return ChannelStatus.FAILURE;
        } else {
            return ChannelStatus.SUCCESS;
        }
    }

    private static final class StaticColumn extends Column<AssetModel.ChannelModel, String> {

        private final String key;

        public StaticColumn(final String key) {
            super(new TextCell());
            this.key = key;
        }

        @Override
        public String getValue(final AssetModel.ChannelModel object) {
            return object.getValue(key);
        }
    }

    private static final class StatusCell extends TextCell {

        @Override
        public Set<String> getConsumedEvents() {
            final HashSet<String> set = new HashSet<>();
            set.add(BrowserEvents.CLICK);
            return set;
        }
    }

    private enum ChannelStatus {

        UNKNOWN("Unknown", "fa-times text-danger", "text-danger"),
        SUCCESS("Success", "fa-check text-success", "text-success"),
        FAILURE("Failure - click for details", "fa-times text-danger", "text-danger cell-clickable"),
        DIRTY("Modified", "fa-pencil", ""),
        DISABLED("Disabled", "", "");

        private String label;
        private String iconStyle;
        private String cellStyle;

        private ChannelStatus(final String label, final String iconStyle, final String cellStyle) {
            this.label = label;
            this.iconStyle = iconStyle;
            this.cellStyle = cellStyle;
        }

        public String getLabel() {
            return label;
        }

        public String getIconStyle() {
            return iconStyle;
        }

        public String getCellStyle() {
            return cellStyle;
        }
    }
}
