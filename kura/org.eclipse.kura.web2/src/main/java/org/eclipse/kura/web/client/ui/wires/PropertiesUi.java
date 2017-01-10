/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
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
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.client.util.MessageUtils;
import org.eclipse.kura.web.shared.model.GwtChannelInfo;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;
import org.eclipse.kura.web.shared.model.GwtConfigParameter.GwtConfigParameterType;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.eclipse.kura.web.shared.service.GwtWireService;
import org.eclipse.kura.web.shared.service.GwtWireServiceAsync;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.FieldSet;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.HelpBlock;
import org.gwtbootstrap3.client.ui.InlineRadio;
import org.gwtbootstrap3.client.ui.Input;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.Panel;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.base.TextBoxBase;
import org.gwtbootstrap3.client.ui.constants.InputType;
import org.gwtbootstrap3.client.ui.constants.ValidationState;
import org.gwtbootstrap3.client.ui.gwt.CellTable;
import org.gwtbootstrap3.client.ui.gwt.FlowPanel;
import org.gwtbootstrap3.client.ui.html.Strong;
import org.gwtbootstrap3.client.ui.html.Text;

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.SelectionCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.cell.client.TextInputCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DefaultHeaderOrFooterBuilder;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;

public class PropertiesUi extends Composite {

    interface ServicesUiUiBinder extends UiBinder<Widget, PropertiesUi> {
    }

    private static final String CONFIG_MAX_VALUE = "configMaxValue";
    private static final String CONFIG_MIN_VALUE = "configMinValue";
    private static Logger errorLogger = Logger.getLogger("ErrorLogger");
    private static final Logger logger = Logger.getLogger(PropertiesUi.class.getSimpleName());

    private static final Messages MSGS = GWT.create(Messages.class);

    private static ServicesUiUiBinder uiBinder = GWT.create(ServicesUiUiBinder.class);

    @UiField
    Button btn_add, btn_remove, btn_download, btn_upload;

    @UiField
    SimplePager channelPager;
    @UiField
    Panel channelPanel;

    private final ListDataProvider<GwtChannelInfo> channelsDataProvider = new ListDataProvider<GwtChannelInfo>();
    @UiField
    CellTable<GwtChannelInfo> channelTable;

    @UiField
    Strong channelTitle;
    private boolean dirty;
    @UiField
    FieldSet fields;

    private final GwtWireServiceAsync gwtWireService = GWT.create(GwtWireService.class);

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);

    private boolean hasDriver = false;

    @UiField
    Alert incompleteFields;

    @UiField
    Modal incompleteFieldsModal;

    @UiField
    Text incompleteFieldsText;

    private boolean isWireAsset = false;

    GwtConfigComponent m_baseDriverDescriptor;

    GwtConfigComponent m_configurableComponent;

    GwtConfigComponent m_driverDescriptor;
    Modal modal;
    private boolean nonValidated;
    Set<String> nonValidatedCells;
    String pid;

    final SingleSelectionModel<GwtChannelInfo> selectionModel = new SingleSelectionModel<GwtChannelInfo>();

    AnchorListItem service;

    HashMap<String, Boolean> valid = new HashMap<String, Boolean>();

    TextBox validated;

    public PropertiesUi(final GwtConfigComponent addedItem, final String pid) {
        this.initWidget(uiBinder.createAndBindUi(this));
        this.pid = pid;
        this.m_configurableComponent = addedItem;
        this.fields.clear();
        this.setOriginalValues(this.m_configurableComponent);
        this.channelPager.setPageSize(5);
        this.channelPager.setDisplay(this.channelTable);
        this.channelTable.setSelectionModel(this.selectionModel);
        this.channelsDataProvider.addDataDisplay(this.channelTable);
        this.channelPanel.setVisible(false);
        this.btn_remove.setEnabled(false);

        this.nonValidatedCells = new HashSet<String>();
        this.hasDriver = this.m_configurableComponent.get("driver.pid") != null;
        this.isWireAsset = (this.m_configurableComponent.getFactoryId() != null)
                && this.m_configurableComponent.getFactoryId().contains("WireAsset");

        if (this.hasDriver) {
            this.channelTitle
                    .setText(MSGS.channelTableTitle(this.m_configurableComponent.get("driver.pid").toString()));
        }

        this.btn_download.addClickHandler(new ClickHandler() {

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

        this.btn_add.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                final GwtChannelInfo ci = new GwtChannelInfo();
                ci.setId(String.valueOf(PropertiesUi.this.findNextChannelId()));
                ci.setName("Channel " + ci.getId());
                ci.setType("READ");
                ci.setValueType("INTEGER");
                for (final GwtConfigParameter param : PropertiesUi.this.m_driverDescriptor.getParameters()) {
                    ci.set(param.getName(), param.getDefault());
                }

                PropertiesUi.this.channelsDataProvider.getList().add(ci);
                PropertiesUi.this.channelsDataProvider.refresh();
                PropertiesUi.this.channelPager.lastPage();
                PropertiesUi.this.setDirty(true);
            }
        });

        this.btn_remove.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                final GwtChannelInfo ci = PropertiesUi.this.selectionModel.getSelectedObject();
                PropertiesUi.this.channelsDataProvider.getList().remove(ci);
                PropertiesUi.this.channelsDataProvider.refresh();
                PropertiesUi.this.btn_remove.setEnabled(false);
                PropertiesUi.this.setDirty(true);
            }
        });

        this.selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {

            @Override
            public void onSelectionChange(final SelectionChangeEvent event) {
                PropertiesUi.this.btn_remove.setEnabled(PropertiesUi.this.selectionModel.getSelectedObject() != null);
            }
        });

        this.renderForm();
        this.initInvalidDataModal();

        this.setDirty(false);

        if (this.hasDriver) {
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
                            PropertiesUi.this.m_baseDriverDescriptor = result;
                        }
                    });
                }
            });
        }
    }

    private void addDefaultColumns() {

        this.channelTable.setHeaderBuilder(new DefaultHeaderOrFooterBuilder<GwtChannelInfo>(this.channelTable, false));

        final Column<GwtChannelInfo, String> c0 = new Column<GwtChannelInfo, String>(new TextCell()) {

            @Override
            public String getValue(final GwtChannelInfo object) {
                return object.getId();
            }

        };

        this.channelTable.addColumn(c0, new TextHeader("ID"));

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
            }
        });

        this.channelTable.addColumn(c, new TextHeader("name"));

        final List<String> valueOptions = Arrays.asList("READ", "WRITE", "READ_WRITE");

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
            }
        });
        this.channelTable.addColumn(c2, new TextHeader("type"));

        final List<String> valueTypeOptions = Arrays.asList("BOOLEAN", "BYTE", "BYTE_ARRAY", "DOUBLE", "INTEGER",
                "LONG", "SHORT", "STRING");

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
            }
        });
        this.channelTable.addColumn(c3, new TextHeader("value type"));

    }

    private void clearChannelsFromConfig() {
        final List<GwtConfigParameter> params = this.m_configurableComponent.getParameters();
        final Iterator<GwtConfigParameter> it = params.iterator();
        while (it.hasNext()) {
            final GwtConfigParameter p = it.next();
            if (p.getName() != null && p.getName().contains(".CH.")) {
                final String[] tokens = p.getName().split("\\.");
                if ((tokens.length > 2) && tokens[1].trim().equals("CH")) {
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
            final Map<String, String> newOpts = new HashMap<String, String>(source.getOptions());
            newParam.setOptions(newOpts);
        }

        return newParam;
    }

    protected void fillUpdatedConfiguration(FormGroup fg) {
        GwtConfigParameter param = new GwtConfigParameter();
        List<String> multiFieldValues = new ArrayList<String>();
        int fgwCount = fg.getWidgetCount();
        for (int i = 0; i < fgwCount; i++) {
            logger.fine("Widget: " + fg.getClass());

            if (fg.getWidget(i) instanceof FormLabel) {
                param = this.m_configurableComponent.getParameter(fg.getWidget(i).getTitle());
                logger.fine("Param: " + fg.getTitle() + " -> " + param);

            } else if (fg.getWidget(i) instanceof ListBox || fg.getWidget(i) instanceof Input
                    || fg.getWidget(i) instanceof TextBoxBase) {

                if (param == null) {
                    errorLogger.warning("Missing parameter");
                    continue;
                }
                String value = getUpdatedFieldConfiguration(param, fg.getWidget(i));
                if (value == null) {
                    continue;
                }
                if (param.getCardinality() == 0 || param.getCardinality() == 1 || param.getCardinality() == -1) {
                    param.setValue(value);
                } else {
                    multiFieldValues.add(value);
                }
            }
        }
        if (!multiFieldValues.isEmpty() && param != null) {
            param.setValues(multiFieldValues.toArray(new String[] {}));
        }
    }

    //
    // Private methods
    //
    private int findNextChannelId() {
        int channelIndex = 0;
        for (final GwtChannelInfo ci : this.channelsDataProvider.getList()) {
            channelIndex = Math.max(channelIndex, Integer.valueOf(ci.getId()));
        }
        return channelIndex + 1;
    }

    private Column<GwtChannelInfo, String> getColumnFromParam(final GwtConfigParameter param) {
        final Map<String, String> options = param.getOptions();
        if ((options != null) && (options.size() > 0)) {
            return this.getSelectionInputColumn(param);
        } else {
            return this.getInputCellColumn(param);
        }
    }

    public GwtConfigComponent getConfiguration() {
        return this.m_configurableComponent;
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
                ValidationData viewData = null;
                if ((!PropertiesUi.this.invalidateType(type, value))
                        || ((max != null) && PropertiesUi.this.invalidateMax(value, max))
                        || ((min != null) && PropertiesUi.this.invalidateMin(value, min))) {
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
                viewData.setValue(value);
                PropertiesUi.this.channelTable.redraw();
                object.set(param.getId(), value);
            }
        });

        return result;
    }

    private Column<GwtChannelInfo, String> getSelectionInputColumn(final GwtConfigParameter param) {
        final String id = param.getId();
        final Map<String, String> options = param.getOptions();
        final ArrayList<String> opts = new ArrayList<String>(options.keySet());
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
            }
        });

        return result;
    }

    // Get updated parameters
    GwtConfigComponent getUpdatedConfiguration() {
        final Iterator<Widget> it = this.fields.iterator();
        while (it.hasNext()) {
            final Widget w = it.next();
            if (w instanceof FormGroup) {
                final FormGroup fg = (FormGroup) w;
                this.fillUpdatedConfiguration(fg);
            }
        }

        if (this.isWireAsset) {
            this.channelsDataProvider.refresh();
            this.clearChannelsFromConfig();

            for (final GwtChannelInfo ci : this.channelsDataProvider.getList()) {
                String prefix = ci.getId() + ".CH.";

                final GwtConfigParameter newName = this.copyOf(this.m_baseDriverDescriptor.getParameter("name"));
                newName.setName(prefix + "name");
                newName.setId(prefix + "name");
                newName.setValue(ci.getName());
                this.m_configurableComponent.getParameters().add(newName);

                final GwtConfigParameter newType = this.copyOf(this.m_baseDriverDescriptor.getParameter("type"));
                newType.setName(prefix + "type");
                newType.setId(prefix + "type");
                newType.setValue(ci.getType());
                this.m_configurableComponent.getParameters().add(newType);

                final GwtConfigParameter newValueType = this
                        .copyOf(this.m_baseDriverDescriptor.getParameter("value.type"));
                newValueType.setName(prefix + "value.type");
                newValueType.setId(prefix + "value.type");
                newValueType.setValue(ci.getValueType());
                this.m_configurableComponent.getParameters().add(newValueType);

                prefix += "DRIVER.";
                for (final GwtConfigParameter param : this.m_driverDescriptor.getParameters()) {
                    final GwtConfigParameter newParam = this.copyOf(param);
                    newParam.setName(prefix + param.getName());
                    newParam.setId(prefix + param.getId());
                    newParam.setValue(ci.get(param.getName()).toString());
                    this.m_configurableComponent.getParameters().add(newParam);
                }
            }
        }

        return this.m_configurableComponent;
    }

    private String getUpdatedFieldConfiguration(GwtConfigParameter param, Widget wg) {
        Map<String, String> options = param.getOptions();
        if (options != null && options.size() > 0) {
            Map<String, String> oMap = param.getOptions();
            if (wg instanceof ListBox) {
                return oMap.get(((ListBox) wg).getSelectedItemText());
            } else {
                return null;
            }
        } else {
            switch (param.getType()) {
            case BOOLEAN:
                return param.getValue();
            case LONG:
            case DOUBLE:
            case FLOAT:
            case SHORT:
            case BYTE:
            case INTEGER:
            case CHAR:
            case STRING:
                TextBoxBase tb = (TextBoxBase) wg;
                String value = tb.getText();
                if (value != null) {
                    return value;
                } else {
                    return null;
                }
            case PASSWORD:
                if (wg instanceof Input) {
                    return ((Input) wg).getValue();
                } else {
                    return null;
                }
            default:
                break;
            }
        }
        return null;
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

    public boolean isDirty() {
        return this.dirty;
    }

    public boolean isNonValidated() {
        return this.nonValidated;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void renderBooleanField(final GwtConfigParameter param, final boolean isFirstInstance,
            final FormGroup formGroup) {
        this.valid.put(param.getName(), true);

        if (isFirstInstance) {
            final FormLabel formLabel = new FormLabel();
            if (param.isRequired()) {
                formLabel.setText(param.getName() + "*");
            } else {
                formLabel.setText(param.getName());
            }
            formGroup.add(formLabel);

            if (param.getDescription() != null) {
                final HelpBlock toolTip = new HelpBlock();
                toolTip.setText(param.getDescription());
                formGroup.add(toolTip);
            }
        }

        final FlowPanel flowPanel = new FlowPanel();

        final InlineRadio radioTrue = new InlineRadio(param.getName());
        radioTrue.setText(MSGS.trueLabel());
        radioTrue.setFormValue("true");

        final InlineRadio radioFalse = new InlineRadio(param.getName());
        radioFalse.setText(MSGS.falseLabel());
        radioFalse.setFormValue("false");

        radioTrue.setValue(Boolean.parseBoolean(param.getValue()));
        radioFalse.setValue(!Boolean.parseBoolean(param.getValue()));

        if ((param.getMin() != null) && param.getMin().equals(param.getMax())) {
            radioTrue.setEnabled(false);
            radioFalse.setEnabled(false);
        }

        flowPanel.add(radioTrue);
        flowPanel.add(radioFalse);

        radioTrue.addValueChangeHandler(new ValueChangeHandler() {

            @Override
            public void onValueChange(final ValueChangeEvent event) {
                PropertiesUi.this.setDirty(true);
                final InlineRadio box = (InlineRadio) event.getSource();
                if (box.getValue()) {
                    param.setValue(String.valueOf(true));
                }
            }
        });
        radioFalse.addValueChangeHandler(new ValueChangeHandler() {

            @Override
            public void onValueChange(final ValueChangeEvent event) {
                PropertiesUi.this.setDirty(true);
                final InlineRadio box = (InlineRadio) event.getSource();
                if (box.getValue()) {
                    param.setValue(String.valueOf(false));
                }
            }
        });

        formGroup.add(flowPanel);

        this.fields.add(formGroup);
    }

    private void renderChoiceField(final GwtConfigParameter param, final boolean isFirstInstance,
            final FormGroup formGroup) {
        this.valid.put(param.getName(), true);

        if (isFirstInstance) {
            final FormLabel formLabel = new FormLabel();
            if (param.isRequired()) {
                formLabel.setText(param.getName() + "*");
            } else {
                formLabel.setText(param.getName());
            }
            formGroup.add(formLabel);

            if (param.getDescription() != null) {
                final HelpBlock toolTip = new HelpBlock();
                toolTip.setText(param.getDescription());
                formGroup.add(toolTip);
            }
        }

        final ListBox listBox = new ListBox();

        final Map<String, String> oMap = param.getOptions();
        int i = 0;
        boolean valueFound = false;
        for (final Map.Entry<String, String> entry : oMap.entrySet()) {
            listBox.addItem(entry.getKey());

            final boolean hasDefault = param.getDefault() != null;
            boolean setDefault = false;
            final boolean hasValue = param.getValue() != null;
            boolean setValue = false;

            if (param.getDefault() != null) {
                setDefault = param.getDefault().equals(entry.getValue());
            }
            if (param.getValue() != null) {
                setValue = param.getValue().equals(entry.getValue());
            }

            if (!valueFound) {
                if (hasDefault && setDefault) {
                    listBox.setSelectedIndex(i);
                } else if (hasValue && setValue) {
                    listBox.setSelectedIndex(i);
                    valueFound = true;
                }
            }

            i++;
        }

        listBox.addChangeHandler(new ChangeHandler() {

            @Override
            public void onChange(final ChangeEvent event) {
                PropertiesUi.this.setDirty(true);
                final ListBox box = (ListBox) event.getSource();
                param.setValue(box.getSelectedItemText());
            }
        });

        formGroup.add(listBox);

        this.fields.add(formGroup);
    }

    // passes the parameter to the corresponding method depending on the type of
    // field to be rendered
    private void renderConfigParameter(final GwtConfigParameter param, final boolean isFirstInstance,
            final FormGroup formGroup, final boolean isReadOnly) {
        final Map<String, String> options = param.getOptions();
        if ((options != null) && (options.size() > 0)) {
            this.renderChoiceField(param, isFirstInstance, formGroup);
        } else if (param.getType().equals(GwtConfigParameterType.BOOLEAN)) {
            this.renderBooleanField(param, isFirstInstance, formGroup);
        } else if (param.getType().equals(GwtConfigParameterType.PASSWORD)) {
            this.renderPasswordField(param, isFirstInstance, formGroup);
        } else {
            this.renderTextField(param, isFirstInstance, formGroup, isReadOnly);
        }
    }

    // TODO: Separate render methods for each type (ex: Boolean, String,
    // Password, etc.). See latest org.eclipse.kura.web code.
    // Iterates through all GwtConfigParameter in the selected
    // GwtConfigComponent
    public void renderForm() {
        this.fields.clear();
        for (final GwtConfigParameter param : this.m_configurableComponent.getParameters()) {
            final String[] tokens = param.getId().split("\\.");
            boolean isChannelData = (tokens.length > 2) && tokens[1].trim().equals("CH");
            final boolean isDriverField = param.getId().equals("driver.pid");

            isChannelData = isChannelData && this.isWireAsset;
            if (!isChannelData && !isDriverField) {
                if ((param.getCardinality() == 0) || (param.getCardinality() == 1) || (param.getCardinality() == -1)) {
                    final FormGroup formGroup = new FormGroup();
                    if (isDriverField) {
                        this.renderConfigParameter(param, true, formGroup, true);
                    } else {
                        this.renderConfigParameter(param, true, formGroup, false);
                    }
                } else {
                    this.renderMultiFieldConfigParameter(param);
                }
            }

        }

        if (this.isWireAsset && this.hasDriver) {
            this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                @Override
                public void onFailure(final Throwable caught) {

                    FailureHandler.handle(caught);
                }

                @Override
                public void onSuccess(final GwtXSRFToken result) {
                    PropertiesUi.this.gwtWireService.getGwtChannelDescriptor(result,
                            PropertiesUi.this.m_configurableComponent.get("driver.pid").toString(),
                            new AsyncCallback<GwtConfigComponent>() {

                        @Override
                        public void onFailure(final Throwable caught) {
                            FailureHandler.handle(caught);
                        }

                        @Override
                        public void onSuccess(final GwtConfigComponent result) {
                            PropertiesUi.this.m_driverDescriptor = result;
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
                                            PropertiesUi.this.m_driverDescriptor,
                                            PropertiesUi.this.m_configurableComponent,
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

    private void renderMultiFieldConfigParameter(final GwtConfigParameter mParam) {
        String value = null;
        final String[] values = mParam.getValues();
        boolean isFirstInstance = true;
        final FormGroup formGroup = new FormGroup();
        for (int i = 0; i < Math.min(mParam.getCardinality(), 10); i++) {
            // temporary set the param value to the current one in the array
            // use a value from the one passed in if we have it.
            value = null;
            if ((values != null) && (i < values.length)) {
                value = values[i];
            }
            mParam.setValue(value);
            this.renderConfigParameter(mParam, isFirstInstance, formGroup, false);
            if (isFirstInstance) {
                isFirstInstance = false;
            }
        }
        // restore a null current value
        mParam.setValue(null);
    }

    private void renderPasswordField(final GwtConfigParameter param, final boolean isFirstInstance,
            final FormGroup formGroup) {
        this.valid.put(param.getName(), true);

        if (isFirstInstance) {
            final FormLabel formLabel = new FormLabel();
            if (param.isRequired()) {
                formLabel.setText(param.getName() + "*");
            } else {
                formLabel.setText(param.getName());
            }
            formGroup.add(formLabel);

            if (param.getDescription() != null) {
                final HelpBlock toolTip = new HelpBlock();
                toolTip.setText(param.getDescription());
                formGroup.add(toolTip);
            }
        }

        final Input input = new Input();
        input.setType(InputType.PASSWORD);
        if (param.getValue() != null) {
            input.setText(param.getValue());
        } else {
            input.setText("");
        }

        if ((param.getMin() != null) && param.getMin().equals(param.getMax())) {
            input.setReadOnly(true);
            input.setEnabled(false);
        }

        input.addValueChangeHandler(new ValueChangeHandler<String>() {

            @Override
            public void onValueChange(final ValueChangeEvent<String> event) {
                PropertiesUi.this.setDirty(true);
                final Input box = (Input) event.getSource();
                final FormGroup group = (FormGroup) box.getParent();
                // Validation
                if (((box.getText() == null) || "".equals(box.getText().trim())) && param.isRequired()) {
                    // null in required field
                    group.setValidationState(ValidationState.ERROR);
                    box.setPlaceholder("Field is required");
                    PropertiesUi.this.valid.put(param.getName(), false);
                } else {
                    group.setValidationState(ValidationState.NONE);
                    box.setPlaceholder("");
                    param.setValue(box.getText());
                    PropertiesUi.this.valid.put(param.getName(), true);
                }
            }
        });

        formGroup.add(input);
        this.fields.add(formGroup);

    }

    // Field Render based on Type
    private void renderTextField(final GwtConfigParameter param, final boolean isFirstInstance,
            final FormGroup formGroup, final boolean isReadOnly) {

        this.valid.put(param.getName(), true);

        if (isFirstInstance) {
            final FormLabel formLabel = new FormLabel();
            if (param.isRequired()) {
                formLabel.setText(param.getName() + "*");
            } else {
                formLabel.setText(param.getName());
            }
            formGroup.add(formLabel);

            final HelpBlock tooltip = new HelpBlock();
            tooltip.setText(param.getDescription());
            formGroup.add(tooltip);
        }

        final TextBox textBox = new TextBox();
        textBox.setReadOnly(isReadOnly);
        if (param.getDescription() != null && param.getDescription().contains("\u200B\u200B\u200B\u200B\u200B")) {
            textBox.setHeight("120px");
        }

        String formattedValue = new String();
        // TODO: Probably this formatting step has no
        // sense. But it seems that, if not in
        // debug, all the browsers are able to
        // display the double value as expected
        switch (param.getType()) {
        case LONG:
            if ((param.getValue() != null) && !"".equals(param.getValue().trim())) {
                formattedValue = String.valueOf(Long.parseLong(param.getValue()));
            }
            break;
        case DOUBLE:
            if ((param.getValue() != null) && !"".equals(param.getValue().trim())) {
                formattedValue = String.valueOf(Double.parseDouble(param.getValue()));
            }
            break;
        case FLOAT:
            if ((param.getValue() != null) && !"".equals(param.getValue().trim())) {
                formattedValue = String.valueOf(Float.parseFloat(param.getValue()));
            }
            break;
        case SHORT:
            if ((param.getValue() != null) && !"".equals(param.getValue().trim())) {
                formattedValue = String.valueOf(Short.parseShort(param.getValue()));
            }
            break;
        case BYTE:
            if ((param.getValue() != null) && !"".equals(param.getValue().trim())) {
                formattedValue = String.valueOf(Byte.parseByte(param.getValue()));
            }
            break;
        case INTEGER:
            if ((param.getValue() != null) && !"".equals(param.getValue().trim())) {
                formattedValue = String.valueOf(Integer.parseInt(param.getValue()));
            }
            break;
        default:
            formattedValue = param.getValue();
            break;
        }

        if (param.getValue() != null) {
            textBox.setText(formattedValue);
        } else {
            textBox.setText("");
        }

        if ((param.getMin() != null) && param.getMin().equals(param.getMax())) {
            textBox.setReadOnly(true);
            textBox.setEnabled(false);
        }

        formGroup.add(textBox);

        textBox.addValueChangeHandler(new ValueChangeHandler<String>() {

            @Override
            public void onValueChange(final ValueChangeEvent<String> event) {
                PropertiesUi.this.setDirty(true);
                final TextBox box = (TextBox) event.getSource();
                final FormGroup group = (FormGroup) box.getParent();
                PropertiesUi.this.validate(param, box, group);
            }
        });
        this.fields.add(formGroup);
    }

    public void setDirty(final boolean flag) {
        this.dirty = flag;
        if (flag) {
            WiresPanelUi.propertiesPanelHeader.setText(
                    WiresPanelUi.getFormattedPid(this.m_configurableComponent.getFactoryId()) + " - " + this.pid + "*");
            WiresPanelUi.setDirty(true);
        } else {
            WiresPanelUi.propertiesPanelHeader.setText(
                    WiresPanelUi.getFormattedPid(this.m_configurableComponent.getFactoryId()) + " - " + this.pid);
        }
    }

    public void setNonValidated(final boolean flag) {
        this.nonValidated = flag;
        if (flag) {
            WiresPanelUi.propertiesPanelHeader.setText(
                    WiresPanelUi.getFormattedPid(this.m_configurableComponent.getFactoryId()) + " - " + this.pid + "*");
            WiresPanelUi.btnSave.setEnabled(false);
        } else if (this.nonValidatedCells.isEmpty()) {
            WiresPanelUi.propertiesPanelHeader.setText(
                    WiresPanelUi.getFormattedPid(this.m_configurableComponent.getFactoryId()) + " - " + this.pid);
            WiresPanelUi.btnSave.setEnabled(true);
        }
    }

    private void setOriginalValues(final GwtConfigComponent component) {
        for (final GwtConfigParameter parameter : component.getParameters()) {
            parameter.setValue(parameter.getValue());
        }
    }

    // Validates all the entered values
    // TODO: validation should be done like in the old web ui: cleaner approach
    private boolean validate(final GwtConfigParameter param, final TextBox box, final FormGroup group) {
        if (param.isRequired() && ((box.getText().trim() == null) || "".equals(box.getText().trim()))) {
            group.setValidationState(ValidationState.ERROR);
            this.valid.put(param.getName(), false);
            box.setPlaceholder(MSGS.formRequiredParameter());
            return false;
        }

        if ((box.getText().trim() != null) && !"".equals(box.getText().trim())) {
            if (param.getType().equals(GwtConfigParameterType.CHAR)) {
                if (box.getText().trim().length() > 1) {
                    group.setValidationState(ValidationState.ERROR);
                    this.valid.put(param.getName(), false);
                    box.setPlaceholder(
                            MessageUtils.get(Integer.toString(box.getText().trim().length()), box.getText()));
                    return false;
                }
                // TODO: why this character boxing?
                if (param.getMin() != null) {
                    if (Character.valueOf(param.getMin().charAt(0)).charValue() > Character
                            .valueOf(box.getText().trim().charAt(0)).charValue()) {
                        group.setValidationState(ValidationState.ERROR);
                        this.valid.put(param.getName(), false);
                        box.setPlaceholder(MessageUtils.get(CONFIG_MIN_VALUE,
                                Character.valueOf(param.getMin().charAt(0)).charValue()));
                        return false;
                    }
                }
                if (param.getMax() != null) {
                    if (Character.valueOf(param.getMax().charAt(0)).charValue() < Character
                            .valueOf(box.getText().trim().charAt(0)).charValue()) {
                        group.setValidationState(ValidationState.ERROR);
                        this.valid.put(param.getName(), false);
                        box.setPlaceholder(MessageUtils.get(CONFIG_MAX_VALUE,
                                Character.valueOf(param.getMax().charAt(0)).charValue()));
                        return false;
                    }
                }
            } else if (param.getType().equals(GwtConfigParameterType.STRING)) {
                int configMinValue = 0;
                int configMaxValue = 255;
                try {
                    configMinValue = Integer.parseInt(param.getMin());
                } catch (final NumberFormatException nfe) {
                    errorLogger.log(Level.SEVERE, "Configuration min value error! Applying UI defaults...");
                }
                try {
                    configMaxValue = Integer.parseInt(param.getMax());
                } catch (final NumberFormatException nfe) {
                    errorLogger.log(Level.SEVERE, "Configuration max value error! Applying UI defaults...");
                }

                if ((String.valueOf(box.getText().trim()).length()) < configMinValue) {
                    group.setValidationState(ValidationState.ERROR);
                    this.valid.put(param.getName(), false);
                    box.setPlaceholder(MessageUtils.get(CONFIG_MIN_VALUE, configMinValue));
                    return false;
                }
                if ((String.valueOf(box.getText().trim()).length()) > configMaxValue) {
                    group.setValidationState(ValidationState.ERROR);
                    this.valid.put(param.getName(), false);
                    box.setPlaceholder(MessageUtils.get(CONFIG_MAX_VALUE, configMaxValue));
                    return false;
                }
            } else {
                try {
                    // numeric value
                    if (param.getType().equals(GwtConfigParameterType.FLOAT)) {
                        if (param.getMin() != null) {
                            if (Float.valueOf(param.getMin()).floatValue() > Float.valueOf(box.getText().trim())
                                    .floatValue()) {
                                group.setValidationState(ValidationState.ERROR);
                                this.valid.put(param.getName(), false);
                                box.setPlaceholder(MessageUtils.get(CONFIG_MIN_VALUE, param.getMin()));
                                return false;
                            }
                        }
                        if (param.getMax() != null) {
                            if (Float.valueOf(param.getMax()).floatValue() < Float.valueOf(box.getText().trim())
                                    .floatValue()) {
                                group.setValidationState(ValidationState.ERROR);
                                this.valid.put(param.getName(), false);
                                box.setPlaceholder(MessageUtils.get(CONFIG_MAX_VALUE, param.getMax()));
                                return false;
                            }
                        }
                    } else if (param.getType().equals(GwtConfigParameterType.INTEGER)) {
                        if (param.getMin() != null) {
                            if (Integer.valueOf(param.getMin()).intValue() > Integer.valueOf(box.getText().trim())
                                    .intValue()) {
                                group.setValidationState(ValidationState.ERROR);
                                this.valid.put(param.getName(), false);
                                box.setPlaceholder(MessageUtils.get(CONFIG_MIN_VALUE, param.getMin()));
                                return false;
                            }
                        }
                        if (param.getMax() != null) {
                            if (Integer.valueOf(param.getMax()).intValue() < Integer.valueOf(box.getText().trim())
                                    .intValue()) {
                                group.setValidationState(ValidationState.ERROR);
                                this.valid.put(param.getName(), false);
                                box.setPlaceholder(MessageUtils.get(CONFIG_MAX_VALUE, param.getMax()));
                                return false;
                            }
                        }
                    } else if (param.getType().equals(GwtConfigParameterType.SHORT)) {
                        if (param.getMin() != null) {
                            if (Short.valueOf(param.getMin()).shortValue() > Short.valueOf(box.getText().trim())
                                    .shortValue()) {
                                group.setValidationState(ValidationState.ERROR);
                                this.valid.put(param.getName(), false);
                                box.setPlaceholder(MessageUtils.get(CONFIG_MIN_VALUE, param.getMin()));
                                return false;
                            }
                        }
                        if (param.getMax() != null) {
                            if (Short.valueOf(param.getMax()).shortValue() < Short.valueOf(box.getText().trim())
                                    .shortValue()) {
                                group.setValidationState(ValidationState.ERROR);
                                this.valid.put(param.getName(), false);
                                box.setPlaceholder(MessageUtils.get(CONFIG_MAX_VALUE, param.getMax()));
                                return false;
                            }
                        }
                    } else if (param.getType().equals(GwtConfigParameterType.BYTE)) {
                        if (param.getMin() != null) {
                            if (Byte.valueOf(param.getMin()).byteValue() > Byte.valueOf(box.getText().trim())
                                    .byteValue()) {
                                group.setValidationState(ValidationState.ERROR);
                                this.valid.put(param.getName(), false);
                                box.setPlaceholder(MessageUtils.get(CONFIG_MIN_VALUE, param.getMin()));
                                return false;
                            }
                        }
                        if (param.getMax() != null) {
                            if (Byte.valueOf(param.getMax()).byteValue() < Byte.valueOf(box.getText().trim())
                                    .byteValue()) {
                                group.setValidationState(ValidationState.ERROR);
                                this.valid.put(param.getName(), false);
                                box.setPlaceholder(MessageUtils.get(CONFIG_MAX_VALUE, param.getMax()));
                                return false;
                            }
                        }
                    } else if (param.getType().equals(GwtConfigParameterType.LONG)) {
                        if (param.getMin() != null) {
                            if (Long.valueOf(param.getMin()).longValue() > Long.valueOf(box.getText().trim())
                                    .longValue()) {
                                group.setValidationState(ValidationState.ERROR);
                                this.valid.put(param.getName(), false);
                                box.setPlaceholder(MessageUtils.get(CONFIG_MIN_VALUE, param.getMin()));
                                return false;
                            }
                        }
                        if (param.getMax() != null) {
                            if (Long.valueOf(param.getMax()).longValue() < Long.valueOf(box.getText().trim())
                                    .longValue()) {
                                group.setValidationState(ValidationState.ERROR);
                                this.valid.put(param.getName(), false);
                                box.setPlaceholder(MessageUtils.get(CONFIG_MAX_VALUE, param.getMax()));
                                return false;
                            }
                        }
                    } else if (param.getType().equals(GwtConfigParameterType.DOUBLE)) {
                        if (param.getMin() != null) {
                            if (Double.valueOf(param.getMin()).doubleValue() > Double.valueOf(box.getText().trim())
                                    .doubleValue()) {
                                group.setValidationState(ValidationState.ERROR);
                                this.valid.put(param.getName(), false);
                                box.setPlaceholder(MessageUtils.get(CONFIG_MIN_VALUE, param.getMin()));
                                return false;
                            }
                        }
                        if (param.getMax() != null) {
                            if (Double.valueOf(param.getMax()).doubleValue() < Double.valueOf(box.getText().trim())
                                    .doubleValue()) {
                                group.setValidationState(ValidationState.ERROR);
                                this.valid.put(param.getName(), false);
                                box.setPlaceholder(MessageUtils.get(CONFIG_MAX_VALUE, param.getMax()));
                                return false;
                            }
                        }
                    }
                } catch (final NumberFormatException e) {
                    group.setValidationState(ValidationState.ERROR);
                    this.valid.put(param.getName(), false);
                    box.setPlaceholder(e.getLocalizedMessage());
                    return false;
                }
            }
        }
        group.setValidationState(ValidationState.NONE);
        this.valid.put(param.getName(), true);
        return true;
    }
}
