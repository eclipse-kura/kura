/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
/**
 * Render the Content in the Wire Component Properties Panel based on Service (GwtBSConfigComponent) selected in Wire graph
 *
 * Fields are rendered based on their type (Password(Input), Choice(Dropboxes) etc. with Text fields rendered
 * for both numeric and other textual field with validate() checking if value in numeric fields is numeric
 */
package org.eclipse.kura.web.client.ui.wires;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.eclipse.kura.web.client.ui.AbstractServicesUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtChannelInfo;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;
import org.eclipse.kura.web.shared.model.GwtConfigParameter.GwtConfigParameterType;
import org.eclipse.kura.web.shared.model.GwtWiresChannelType;
import org.eclipse.kura.web.shared.model.GwtWiresDataType;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.eclipse.kura.web.shared.service.GwtWireService;
import org.eclipse.kura.web.shared.service.GwtWireServiceAsync;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.FieldSet;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.Panel;
import org.gwtbootstrap3.client.ui.gwt.CellTable;
import org.gwtbootstrap3.client.ui.html.Strong;
import org.gwtbootstrap3.client.ui.html.Text;

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.SelectionCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.cell.client.TextInputCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DefaultHeaderOrFooterBuilder;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;

public class PropertiesUi extends AbstractServicesUi {

    interface ServicesUiUiBinder extends UiBinder<Widget, PropertiesUi> {
    }

    private static final String PROPERTIES_DRIVER_IDENTIFIER = "DRIVER";
    private static final String PROPERTIES_CHANNEL_IDENTIFIER = "CH";

    private static final String CHANNEL_VALUE_TYPE = "value.type";
    private static final String CHANNEL_TYPE = "type";
    private static final String CHANNEL_NAME = "name";

    private static final int MAXIMUM_PAGE_SIZE = 5;

    private static ServicesUiUiBinder uiBinder = GWT.create(ServicesUiUiBinder.class);

    private final GwtWireServiceAsync gwtWireService = GWT.create(GwtWireService.class);

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);

    private final ListDataProvider<GwtChannelInfo> channelsDataProvider = new ListDataProvider<>();

    private final SingleSelectionModel<GwtChannelInfo> selectionModel = new SingleSelectionModel<>();

    private final String pid;

    private String driverPidProp = "driver.pid";

    private boolean dirty;
    private boolean isWireAsset = false;

    private GwtConfigComponent baseDriverDescriptor;
    private GwtConfigComponent driverDescriptor;

    private boolean nonValidated;
    private final Set<String> nonValidatedCells;

    @UiField
    Button btnAdd;
    @UiField
    Button btnRemove;
    @UiField
    Button btnDownload;
    @UiField
    Button btnUpload;

    @UiField
    SimplePager channelPager;
    @UiField
    Panel channelPanel;

    @UiField
    CellTable<GwtChannelInfo> channelTable;

    @UiField
    Strong channelTitle;
    @UiField
    FieldSet fields;

    @UiField
    Alert incompleteFields;

    @UiField
    Modal incompleteFieldsModal;

    @UiField
    Text incompleteFieldsText;

    public PropertiesUi(final GwtConfigComponent addedItem, final String pid) {
        initWidget(uiBinder.createAndBindUi(this));
        this.pid = pid;
        this.configurableComponent = addedItem;
        this.fields.clear();

        this.channelPager.setPageSize(MAXIMUM_PAGE_SIZE);
        this.channelPager.setDisplay(this.channelTable);
        this.channelTable.setSelectionModel(this.selectionModel);
        this.channelsDataProvider.addDataDisplay(this.channelTable);
        this.channelPanel.setVisible(false);
        this.btnRemove.setEnabled(false);

        this.nonValidatedCells = new HashSet<>();
        this.isWireAsset = this.configurableComponent.getFactoryId() != null
                && this.configurableComponent.getFactoryId().contains("WireAsset");

        if (this.isWireAsset) {
            this.gwtWireService.getDriverPidProp(new AsyncCallback<String>() {

                @Override
                public void onFailure(final Throwable caught) {
                    FailureHandler.handle(caught);
                }

                @Override
                public void onSuccess(String result) {
                    PropertiesUi.this.channelTitle.setText(
                            MSGS.channelTableTitle(PropertiesUi.this.configurableComponent.get(result).toString()));
                    driverPidProp = result;
                }
            });
        }

        this.btnDownload.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                for (final GwtChannelInfo info : PropertiesUi.this.channelsDataProvider.getList()) {
                    logger.log(Level.SEVERE, info.getName());
                    for (final Map.Entry<String, Object> entry : info.getProperties().entrySet()) {
                        final String key = entry.getKey();
                        final String value = String.valueOf(entry.getValue());
                        logger.log(Level.SEVERE, key + "<==>" + value);
                    }
                }

            }
        });

        this.btnAdd.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                final GwtChannelInfo ci = new GwtChannelInfo();
                ci.setId(String.valueOf(PropertiesUi.this.findNextChannelId()));
                ci.setName(MSGS.wiresChannel() + " " + ci.getId());
                ci.setType(GwtWiresChannelType.READ.name());
                ci.setValueType(GwtWiresDataType.INTEGER.name());
                for (final GwtConfigParameter param : PropertiesUi.this.driverDescriptor.getParameters()) {
                    ci.set(param.getName(), param.getDefault());
                }

                PropertiesUi.this.channelsDataProvider.getList().add(ci);
                PropertiesUi.this.channelsDataProvider.refresh();
                PropertiesUi.this.channelPager.lastPage();
                PropertiesUi.this.setDirty(true);
            }
        });

        this.btnRemove.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                final GwtChannelInfo ci = PropertiesUi.this.selectionModel.getSelectedObject();
                PropertiesUi.this.channelsDataProvider.getList().remove(ci);
                PropertiesUi.this.channelsDataProvider.refresh();
                PropertiesUi.this.btnRemove.setEnabled(false);
                PropertiesUi.this.setDirty(true);
            }
        });

        this.selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {

            @Override
            public void onSelectionChange(final SelectionChangeEvent event) {
                PropertiesUi.this.btnRemove.setEnabled(PropertiesUi.this.selectionModel.getSelectedObject() != null);
            }
        });

        renderForm();
        initInvalidDataModal();

        setDirty(false);

        if (this.isWireAsset) {
            // Retrieve base Driver descriptor
            this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                @Override
                public void onFailure(final Throwable caught) {
                    FailureHandler.handle(caught);
                }

                @Override
                public void onSuccess(final GwtXSRFToken result) {
                    PropertiesUi.this.gwtWireService.getGwtBaseChannelDescriptor(result,
                            new AsyncCallback<GwtConfigComponent>() {

                        @Override
                        public void onFailure(final Throwable caught) {
                            FailureHandler.handle(caught);
                        }

                        @Override
                        public void onSuccess(final GwtConfigComponent result) {
                            PropertiesUi.this.baseDriverDescriptor = result;
                        }
                    });
                }
            });
        }
    }

    public GwtConfigComponent getConfiguration() {
        return this.configurableComponent;
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    public boolean isNonValidated() {
        return this.nonValidated;
    }

    // TODO: Separate render methods for each type (ex: Boolean, String,
    // Password, etc.). See latest org.eclipse.kura.web code.
    // Iterates through all GwtConfigParameter in the selected
    // GwtConfigComponent
    @Override
    public void renderForm() {
        this.fields.clear();
        for (final GwtConfigParameter param : this.configurableComponent.getParameters()) {
            final String[] tokens = param.getId().split("\\.");
            boolean isChannelData = tokens.length > 2 && tokens[1].trim().equals(PROPERTIES_CHANNEL_IDENTIFIER);
            final boolean isDriverField = param.getId().equals(driverPidProp);

            isChannelData = isChannelData && this.isWireAsset;
            if (!isChannelData && !isDriverField) {
                if (param.getCardinality() == 0 || param.getCardinality() == 1 || param.getCardinality() == -1) {
                    final FormGroup formGroup = new FormGroup();
                    renderConfigParameter(param, true, formGroup);
                } else {
                    renderMultiFieldConfigParameter(param);
                }
            }
        }

        if (this.isWireAsset) {
            this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                @Override
                public void onFailure(final Throwable caught) {
                    FailureHandler.handle(caught);
                }

                @Override
                public void onSuccess(final GwtXSRFToken result) {
                    PropertiesUi.this.gwtWireService.getGwtChannelDescriptor(result,
                            PropertiesUi.this.configurableComponent.get(driverPidProp).toString(),
                            new AsyncCallback<GwtConfigComponent>() {

                        @Override
                        public void onFailure(final Throwable caught) {
                            FailureHandler.handle(caught);
                        }

                        @Override
                        public void onSuccess(final GwtConfigComponent result) {
                            PropertiesUi.this.driverDescriptor = result;
                            PropertiesUi.this.addDefaultColumns();
                            for (final GwtConfigParameter param : result.getParameters()) {
                                PropertiesUi.this.channelTable.addColumn(PropertiesUi.this.getColumnFromParam(param),
                                        new TextHeader(param.getName()));
                            }

                            PropertiesUi.this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                                @Override
                                public void onFailure(final Throwable caught) {
                                    FailureHandler.handle(caught);
                                }

                                @Override
                                public void onSuccess(final GwtXSRFToken result) {
                                    PropertiesUi.this.gwtWireService.getGwtChannels(result,
                                            PropertiesUi.this.driverDescriptor, PropertiesUi.this.configurableComponent,
                                            new AsyncCallback<List<GwtChannelInfo>>() {

                                        @Override
                                        public void onFailure(final Throwable caught) {
                                            FailureHandler.handle(caught);
                                        }

                                        @Override
                                        public void onSuccess(final List<GwtChannelInfo> result) {
                                            PropertiesUi.this.channelsDataProvider.getList().clear();
                                            PropertiesUi.this.channelsDataProvider.getList().addAll(result);
                                            PropertiesUi.this.channelsDataProvider.refresh();

                                            PropertiesUi.this.channelPanel.setVisible(true);

                                        }
                                    });
                                }
                            });
                        }
                    });
                }
            });
        }

    }

    @Override
    public void setDirty(final boolean flag) {
        this.dirty = flag;
        if (flag) {
            WiresPanelUi.propertiesPanelHeader.setText(
                    WiresPanelUi.getFormattedPid(this.configurableComponent.getFactoryId()) + " - " + this.pid + "*");
            WiresPanelUi.setDirty(true);
        } else {
            WiresPanelUi.propertiesPanelHeader.setText(
                    WiresPanelUi.getFormattedPid(this.configurableComponent.getFactoryId()) + " - " + this.pid);
        }
    }

    public void setNonValidated(final boolean flag) {
        this.nonValidated = flag;
        if (flag) {
            WiresPanelUi.propertiesPanelHeader.setText(
                    WiresPanelUi.getFormattedPid(this.configurableComponent.getFactoryId()) + " - " + this.pid + "*");
            WiresPanelUi.btnSave.setEnabled(false);
        } else if (this.nonValidatedCells.isEmpty()) {
            WiresPanelUi.propertiesPanelHeader.setText(
                    WiresPanelUi.getFormattedPid(this.configurableComponent.getFactoryId()) + " - " + this.pid);
            WiresPanelUi.btnSave.setEnabled(true);
        }
    }

    // Get updated parameters
    protected GwtConfigComponent getUpdatedConfiguration() {
        Iterator<Widget> it = this.fields.iterator();
        while (it.hasNext()) {
            Widget w = it.next();
            if (w instanceof FormGroup) {
                FormGroup fg = (FormGroup) w;
                fillUpdatedConfiguration(fg);
            }
        }

        if (this.isWireAsset) {
            this.channelsDataProvider.refresh();
            clearChannelsFromConfig();

            for (final GwtChannelInfo ci : this.channelsDataProvider.getList()) {
                StringBuilder prefixBuilder = new StringBuilder(ci.getId());
                prefixBuilder.append(".");
                prefixBuilder.append(PROPERTIES_CHANNEL_IDENTIFIER);
                prefixBuilder.append(".");
                String prefix = prefixBuilder.toString();

                final GwtConfigParameter newName = copyOf(this.baseDriverDescriptor.getParameter(CHANNEL_NAME));
                newName.setName(prefix + CHANNEL_NAME);
                newName.setId(prefix + CHANNEL_NAME);
                newName.setValue(ci.getName());
                this.configurableComponent.getParameters().add(newName);

                final GwtConfigParameter newType = copyOf(this.baseDriverDescriptor.getParameter(CHANNEL_TYPE));
                newType.setName(prefix + CHANNEL_TYPE);
                newType.setId(prefix + CHANNEL_TYPE);
                newType.setValue(ci.getType());
                this.configurableComponent.getParameters().add(newType);

                final GwtConfigParameter newValueType = copyOf(
                        this.baseDriverDescriptor.getParameter(CHANNEL_VALUE_TYPE));
                newValueType.setName(prefix + CHANNEL_VALUE_TYPE);
                newValueType.setId(prefix + CHANNEL_VALUE_TYPE);
                newValueType.setValue(ci.getValueType());
                this.configurableComponent.getParameters().add(newValueType);

                prefixBuilder.append(PROPERTIES_DRIVER_IDENTIFIER);
                prefixBuilder.append(".");
                prefix = prefixBuilder.toString();
                for (final GwtConfigParameter param : this.driverDescriptor.getParameters()) {
                    final GwtConfigParameter newParam = copyOf(param);
                    newParam.setName(prefix + param.getName());
                    newParam.setId(prefix + param.getId());
                    newParam.setValue(ci.get(param.getName()).toString());
                    this.configurableComponent.getParameters().add(newParam);
                }
            }
        }

        return this.configurableComponent;
    }

    @Override
    protected void renderBooleanField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
        super.renderBooleanField(param, isFirstInstance, formGroup);
        this.fields.add(formGroup);
    }

    @Override
    protected void renderChoiceField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
        super.renderChoiceField(param, isFirstInstance, formGroup);
        this.fields.add(formGroup);
    }

    @Override
    protected void renderPasswordField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
        super.renderPasswordField(param, isFirstInstance, formGroup);
        this.fields.add(formGroup);
    }

    @Override
    protected void renderTextField(final GwtConfigParameter param, boolean isFirstInstance, final FormGroup formGroup) {
        super.renderTextField(param, isFirstInstance, formGroup);
        this.fields.add(formGroup);
    }

    @Override
    protected void reset() {
        return;
    }

    //
    // Private methods
    //

    private void addDefaultColumns() {

        this.channelTable.setHeaderBuilder(new DefaultHeaderOrFooterBuilder<GwtChannelInfo>(this.channelTable, false));

        final Column<GwtChannelInfo, String> c0 = new Column<GwtChannelInfo, String>(new TextCell()) {

            @Override
            public String getValue(final GwtChannelInfo object) {
                return object.getId();
            }

        };

        this.channelTable.addColumn(c0, new TextHeader(MSGS.wiresChannelId()));

        final Column<GwtChannelInfo, String> c = new Column<GwtChannelInfo, String>(new TextInputCell()) {

            @Override
            public String getValue(final GwtChannelInfo object) {
                return object.getName();
            }

        };

        c.setFieldUpdater(new FieldUpdater<GwtChannelInfo, String>() {

            @Override
            public void update(final int index, final GwtChannelInfo object, final String value) {
                object.setName(value);
                PropertiesUi.this.setDirty(true);
                PropertiesUi.this.channelTable.redraw();
            }
        });

        this.channelTable.addColumn(c, new TextHeader(MSGS.wiresChannelName()));

        final List<String> valueOptions = Arrays.asList(GwtWiresChannelType.READ.name(),
                GwtWiresChannelType.WRITE.name(), GwtWiresChannelType.READ_WRITE.name());

        final Column<GwtChannelInfo, String> c2 = new Column<GwtChannelInfo, String>(new SelectionCell(valueOptions)) {

            @Override
            public String getValue(final GwtChannelInfo object) {
                return object.getType();
            }
        };

        c2.setFieldUpdater(new FieldUpdater<GwtChannelInfo, String>() {

            @Override
            public void update(final int index, final GwtChannelInfo object, final String value) {
                object.setType(value);
                PropertiesUi.this.setDirty(true);
                PropertiesUi.this.channelTable.redraw();
            }
        });
        this.channelTable.addColumn(c2, new TextHeader(MSGS.wiresChannelOperation()));

        final List<String> valueTypeOptions = Arrays.asList(GwtWiresDataType.BOOLEAN.name(),
                GwtWiresDataType.BYTE.name(), GwtWiresDataType.BYTE_ARRAY.name(), GwtWiresDataType.DOUBLE.name(),
                GwtWiresDataType.INTEGER.name(), GwtWiresDataType.LONG.name(), GwtWiresDataType.SHORT.name(),
                GwtWiresDataType.STRING.name());

        final Column<GwtChannelInfo, String> c3 = new Column<GwtChannelInfo, String>(
                new SelectionCell(valueTypeOptions)) {

            @Override
            public String getValue(final GwtChannelInfo object) {
                return object.getValueType();
            }
        };

        c3.setFieldUpdater(new FieldUpdater<GwtChannelInfo, String>() {

            @Override
            public void update(final int index, final GwtChannelInfo object, final String value) {
                object.setValueType(value);
                PropertiesUi.this.setDirty(true);
                PropertiesUi.this.channelTable.redraw();
            }
        });
        this.channelTable.addColumn(c3, new TextHeader(MSGS.wiresChannelValueType()));

    }

    private void clearChannelsFromConfig() {
        final List<GwtConfigParameter> params = this.configurableComponent.getParameters();
        final Iterator<GwtConfigParameter> it = params.iterator();
        while (it.hasNext()) {
            final GwtConfigParameter p = it.next();
            if (p.getName() != null && p.getName().contains("." + PROPERTIES_CHANNEL_IDENTIFIER + ".")) {
                final String[] tokens = p.getName().split("\\.");
                if (tokens.length > 2 && tokens[1].trim().equals(PROPERTIES_CHANNEL_IDENTIFIER)) {
                    it.remove();
                }
            }
        }
    }

    private GwtConfigParameter copyOf(final GwtConfigParameter source) {
        final GwtConfigParameter newParam = new GwtConfigParameter();
        newParam.setCardinality(source.getCardinality());
        newParam.setDefault(source.getDefault());
        newParam.setDescription(source.getDescription());
        newParam.setId(source.getId());
        newParam.setMax(source.getMax());
        newParam.setMin(source.getMin());
        newParam.setName(source.getName());
        newParam.setRequired(source.isRequired());
        newParam.setType(source.getType());
        newParam.setValue(source.getValue());
        if (source.getValues() != null) {
            newParam.setValues(Arrays.copyOf(source.getValues(), source.getValues().length));
        }
        if (source.getOptions() != null) {
            final Map<String, String> newOpts = new HashMap<>(source.getOptions());
            newParam.setOptions(newOpts);
        }

        return newParam;
    }

    private int findNextChannelId() {
        int channelIndex = 0;
        for (final GwtChannelInfo ci : this.channelsDataProvider.getList()) {
            channelIndex = Math.max(channelIndex, Integer.valueOf(ci.getId()));
        }
        return channelIndex + 1;
    }

    private Column<GwtChannelInfo, String> getColumnFromParam(final GwtConfigParameter param) {
        final Map<String, String> options = param.getOptions();
        if (options != null && options.size() > 0) {
            return getSelectionInputColumn(param);
        } else {
            return getInputCellColumn(param);
        }
    }

    private Column<GwtChannelInfo, String> getInputCellColumn(final GwtConfigParameter param) {
        final String id = param.getId();
        final GwtConfigParameterType type = param.getType();
        final String max = param.getMax();
        final String min = param.getMin();
        final ValidationInputCell cell = new ValidationInputCell();
        final Column<GwtChannelInfo, String> result = new Column<GwtChannelInfo, String>(cell) {

            @Override
            public String getValue(final GwtChannelInfo object) {
                final String value = object.get(id).toString();
                return value == null ? "" : value;
            }

        };

        result.setFieldUpdater(new FieldUpdater<GwtChannelInfo, String>() {

            @Override
            public void update(final int index, final GwtChannelInfo object, final String value) {
                ValidationData viewData;
                if (!PropertiesUi.this.invalidateType(type, value)
                        || max != null && PropertiesUi.this.invalidateMax(value, max)
                        || min != null && PropertiesUi.this.invalidateMin(value, min)) {
                    viewData = cell.getViewData(object);
                    viewData.setInvalid(true);
                    PropertiesUi.this.nonValidatedCells.add(object.getId());
                    PropertiesUi.this.setNonValidated(true);
                    // We only modified the cell, so do a local redraw.
                    PropertiesUi.this.channelTable.redraw();
                    return;
                }
                PropertiesUi.this.nonValidatedCells.remove(object.getId());
                PropertiesUi.this.setNonValidated(false);
                PropertiesUi.this.setDirty(true);
                PropertiesUi.this.channelTable.redraw();
                object.set(param.getId(), value);
            }
        });

        return result;
    }

    private Column<GwtChannelInfo, String> getSelectionInputColumn(final GwtConfigParameter param) {
        final String id = param.getId();
        final Map<String, String> options = param.getOptions();
        final ArrayList<String> opts = new ArrayList<>(options.keySet());
        final SelectionCell cell = new SelectionCell(opts);
        final Column<GwtChannelInfo, String> result = new Column<GwtChannelInfo, String>(cell) {

            @Override
            public String getValue(final GwtChannelInfo object) {
                return object.get(id).toString();
            }

        };

        result.setFieldUpdater(new FieldUpdater<GwtChannelInfo, String>() {

            @Override
            public void update(final int index, final GwtChannelInfo object, final String value) {
                PropertiesUi.this.setDirty(true);
                object.set(param.getId(), value);
                PropertiesUi.this.channelTable.redraw();
            }
        });

        return result;
    }

    private void initInvalidDataModal() {
        this.incompleteFieldsModal.setTitle(MSGS.warning());
        this.incompleteFieldsText.setText(MSGS.formWithErrorsOrIncomplete());
    }

    private boolean invalidateMax(final String value, final String maximum) {
        final int val = Integer.parseInt(value);
        final int max = Integer.parseInt(maximum);
        if (val > max) {
            return true;
        }
        return false;
    }

    private boolean invalidateMin(final String value, final String minimum) {
        final int val = Integer.parseInt(value);
        final int min = Integer.parseInt(minimum);
        if (val < min) {
            return true;
        }
        return false;
    }

    private boolean invalidateType(final GwtConfigParameterType param, final String value) {
        switch (param) {
        case STRING:
            return true;
        case LONG:
            try {
                Long.parseLong(value);
                return true;
            } catch (final NumberFormatException e) {
                return false;
            }
        case DOUBLE:
            try {
                Double.parseDouble(value);
                return true;
            } catch (final NumberFormatException e) {
                return false;
            }
        case FLOAT:
            try {
                Float.parseFloat(value);
                return true;
            } catch (final NumberFormatException e) {
                return false;
            }
        case INTEGER:
            try {
                Integer.parseInt(value);
                return true;
            } catch (final NumberFormatException e) {
                return false;
            }
        case BYTE:
            try {
                Byte.parseByte(value);
                return true;
            } catch (final NumberFormatException e) {
                return false;
            }
        case CHAR:
            return Character.isLetter(value.charAt(0));
        case BOOLEAN:
            try {
                Boolean.parseBoolean(value);
                return true;
            } catch (final NumberFormatException e) {
                return false;
            }
        case SHORT:
            try {
                Short.parseShort(value);
                return true;
            } catch (final NumberFormatException e) {
                return false;
            }
        case PASSWORD:
            // no need
            break;
        }
        return false;
    }
}
