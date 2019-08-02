/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.firewall;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.AbstractServicesUi;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.ui.Tab;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.client.util.FilterBuilder;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtComponentService;
import org.eclipse.kura.web.shared.service.GwtComponentServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.FieldSet;
import org.gwtbootstrap3.client.ui.Form;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalHeader;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;

public class IdsTabUi extends AbstractServicesUi implements Tab {

    private static IdsTabUiUiBinder uiBinder = GWT.create(IdsTabUiUiBinder.class);

    interface IdsTabUiUiBinder extends UiBinder<Widget, IdsTabUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);

    private static final String SERVICES_FILTER = FilterBuilder
            .of("objectClass=org.eclipse.kura.security.LoginDosProtectionService");

    private boolean dirty;
    private boolean initialized;
    private GwtConfigComponent originalConfig;

    @UiField
    Button apply;
    @UiField
    Button reset;

    @UiField
    FieldSet fields;
    @UiField
    Form form;

    @UiField
    Modal notificationModal;
    @UiField
    ModalHeader notificationModalHeader;
    @UiField
    ModalBody notificationModalBody;
    @UiField
    Button cancelButton;
    @UiField
    Button applyButton;

    public IdsTabUi() {
        initWidget(uiBinder.createAndBindUi(this));
        this.initialized = false;

        this.apply.setText(MSGS.apply());
        this.apply.addClickHandler(event -> apply());

        this.reset.setText(MSGS.reset());
        this.reset.addClickHandler(event -> reset());

        this.apply.setEnabled(false);
        this.reset.setEnabled(false);

    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    @Override
    public void setDirty(boolean b) {
        this.dirty = b;
        if (this.dirty && this.initialized) {
            this.apply.setEnabled(true);
            this.reset.setEnabled(true);
        }
    }

    @Override
    public boolean isValid() {
        return true;
    }

    private void apply() {

        if (isDirty()) {
            this.notificationModalHeader.setTitle(MSGS.confirm());

            this.notificationModalBody
                    .add(new Span(MSGS.deviceConfigConfirmation(this.configurableComponent.getComponentName())));

            this.cancelButton.setText(MSGS.noButton());
            this.cancelButton.addClickHandler(event -> this.notificationModal.hide());

            this.applyButton.setText(MSGS.yesButton());
            this.applyButton.addClickHandler(event -> {
                EntryClassUi.showWaitModal();
                try {
                    getUpdatedConfiguration();
                } catch (Exception ex) {
                    EntryClassUi.hideWaitModal();
                    FailureHandler.handle(ex);
                    return;
                }
                this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(ex);
                    }

                    @Override
                    public void onSuccess(GwtXSRFToken token) {
                        IdsTabUi.this.gwtComponentService.updateComponentConfiguration(token,
                                IdsTabUi.this.configurableComponent, new AsyncCallback<Void>() {

                                    @Override
                                    public void onFailure(Throwable caught) {
                                        EntryClassUi.hideWaitModal();
                                        FailureHandler.handle(caught);
                                        errorLogger.log(Level.SEVERE,
                                                caught.getLocalizedMessage() != null ? caught.getLocalizedMessage()
                                                        : caught.getClass().getName(),
                                                caught);
                                    }

                                    @Override
                                    public void onSuccess(Void result) {
                                        IdsTabUi.this.notificationModal.hide();
                                        logger.info(MSGS.info() + ": " + MSGS.deviceConfigApplied());
                                        IdsTabUi.this.apply.setEnabled(false);
                                        IdsTabUi.this.reset.setEnabled(false);
                                        setDirty(false);
                                        IdsTabUi.this.originalConfig = IdsTabUi.this.configurableComponent;
                                        EntryClassUi.hideWaitModal();
                                    }
                                });

                    }
                });
            });

            this.notificationModal.show();
            this.cancelButton.setFocus(true);
        }

    }

    private GwtConfigComponent getUpdatedConfiguration() {
        Iterator<Widget> it = this.fields.iterator();
        while (it.hasNext()) {
            Widget w = it.next();
            if (w instanceof FormGroup) {
                FormGroup fg = (FormGroup) w;
                fillUpdatedConfiguration(fg);
            }
        }
        return this.configurableComponent;
    }

    @Override
    protected void reset() {
        if (isDirty()) {
            this.notificationModalHeader.setTitle(MSGS.confirm());

            this.notificationModalBody.add(new Span(MSGS.deviceConfigDirty()));

            this.cancelButton.setText(MSGS.noButton());
            this.cancelButton.addClickHandler(event -> this.notificationModal.hide());
            this.applyButton.setText(MSGS.yesButton());
            this.applyButton.addClickHandler(event -> {
                this.notificationModal.hide();
                restoreConfiguration(this.originalConfig);
                renderForm();
                this.apply.setEnabled(false);
                this.reset.setEnabled(false);
                setDirty(false);
            });
            this.notificationModal.show();
            this.cancelButton.setFocus(true);
        }
    }

    @Override
    protected void renderForm() {
        this.fields.clear();
        for (GwtConfigParameter param : this.configurableComponent.getParameters()) {
            if (param.getCardinality() == 0 || param.getCardinality() == 1 || param.getCardinality() == -1) {
                FormGroup formGroup = new FormGroup();
                renderConfigParameter(param, true, formGroup);
            } else {
                renderMultiFieldConfigParameter(param);
            }
        }
        this.initialized = true;
    }

    @Override
    protected void renderBooleanField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
        super.renderBooleanField(param, isFirstInstance, formGroup);
        this.fields.add(formGroup);
    }

    @Override
    public void refresh() {
        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                IdsTabUi.this.gwtComponentService.findComponentConfigurations(token, SERVICES_FILTER,
                        new AsyncCallback<List<GwtConfigComponent>>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                EntryClassUi.hideWaitModal();
                                FailureHandler.handle(caught);
                            }

                            @Override
                            public void onSuccess(List<GwtConfigComponent> gwtConfigComps) {
                                if (!gwtConfigComps.isEmpty()) {
                                    IdsTabUi.this.originalConfig = gwtConfigComps.get(0);
                                    restoreConfiguration(IdsTabUi.this.originalConfig);
                                    IdsTabUi.this.fields.clear();

                                    renderForm();

                                    setDirty(false);
                                    IdsTabUi.this.apply.setEnabled(false);
                                    IdsTabUi.this.reset.setEnabled(false);
                                }
                            }
                        });
            }
        });

    }
}