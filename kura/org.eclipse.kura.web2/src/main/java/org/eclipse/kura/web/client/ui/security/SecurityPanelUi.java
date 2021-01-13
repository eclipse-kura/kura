/*******************************************************************************
 * Copyright (c) 2020, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.client.ui.security;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.AlertDialog;
import org.eclipse.kura.web.client.ui.ServicesUi;
import org.eclipse.kura.web.client.ui.ServicesUi.ValidationResult;
import org.eclipse.kura.web.client.ui.ServicesUi.Validator;
import org.eclipse.kura.web.client.ui.Tab;
import org.eclipse.kura.web.client.util.request.RequestQueue;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.service.GwtComponentService;
import org.eclipse.kura.web.shared.service.GwtComponentServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityService;
import org.eclipse.kura.web.shared.service.GwtSecurityServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.eclipse.kura.web2.ext.WidgetFactory;
import org.gwtbootstrap3.client.ui.Anchor;
import org.gwtbootstrap3.client.ui.NavTabs;
import org.gwtbootstrap3.client.ui.TabContent;
import org.gwtbootstrap3.client.ui.TabListItem;
import org.gwtbootstrap3.client.ui.TabPane;
import org.gwtbootstrap3.client.ui.html.Paragraph;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IndexedPanel;
import com.google.gwt.user.client.ui.Widget;

public class SecurityPanelUi extends Composite {

    private static SecurityPanelUiUiBinder uiBinder = GWT.create(SecurityPanelUiUiBinder.class);
    private static final Logger logger = Logger.getLogger(SecurityPanelUi.class.getSimpleName());
    private static final Messages MSGS = GWT.create(Messages.class);

    private final GwtSecurityServiceAsync gwtSecurityService = GWT.create(GwtSecurityService.class);
    private final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);
    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);

    interface SecurityPanelUiUiBinder extends UiBinder<Widget, SecurityPanelUi> {
    }

    GwtSession session;

    @UiField
    CertificateListTabUi certificateListPanel;
    @UiField
    TabPane httpServicePanel;
    @UiField
    TabPane consolePanel;
    @UiField
    SecurityTabUi securityPanel;

    @UiField
    TabListItem certificateList;
    @UiField
    TabListItem httpService;
    @UiField
    TabListItem console;
    @UiField
    TabListItem security;

    @UiField
    TabContent tabContent;
    @UiField
    NavTabs navTabs;

    @UiField
    HTMLPanel securityIntro;

    @UiField
    AlertDialog alertDialog;

    private TabListItem selected = certificateList;

    public SecurityPanelUi() {
        logger.log(Level.FINER, "Initiating SecurityPanelUI...");

        initWidget(uiBinder.createAndBindUi(this));
        Paragraph description = new Paragraph();
        description.setText(MSGS.securityIntro());
        this.securityIntro.add(description);

        AsyncCallback<Boolean> callback = new AsyncCallback<Boolean>() {

            @Override
            public void onFailure(Throwable caught) {
                SecurityPanelUi.this.security.setVisible(false);
            }

            @Override
            public void onSuccess(Boolean result) {
                SecurityPanelUi.this.security.setVisible(result);
            }
        };
        this.gwtSecurityService.isSecurityServiceAvailable(callback);

        this.certificateList.addClickHandler(addDirtyCheck(new Tab.RefreshHandler(this.certificateListPanel)));
        this.httpService.addClickHandler(
                addDirtyCheck(e -> this.loadServiceConfig("org.eclipse.kura.http.server.manager.HttpService",
                        httpServicePanel, Optional.of(getHttpServiceOptionsValidator()))));
        this.console.addClickHandler(addDirtyCheck(e -> this.loadServiceConfig("org.eclipse.kura.web.Console",
                consolePanel, Optional.of(getConsoleOptionsValidator()))));
        this.security.addClickHandler(addDirtyCheck(new Tab.RefreshHandler(this.securityPanel)));
    }

    public void loadServiceConfig(final String pid, final TabPane panel,
            final Optional<ServicesUi.Validator> validator) {
        RequestQueue.submit(c -> gwtXSRFService.generateSecurityToken(c.callback(
                token -> gwtComponentService.findFilteredComponentConfiguration(token, pid, c.callback(result -> {
                    for (GwtConfigComponent config : result) {
                        panel.clear();
                        panel.add(new ServicesUi(config, Optional.empty(), validator));
                    }
                })))));
    }

    public void load() {
        this.certificateListPanel.refresh();
    }

    public void setSession(GwtSession currentSession) {
        this.session = currentSession;
    }

    public boolean isDirty() {
        boolean certListDirty = this.certificateListPanel.isDirty();
        boolean securityDirty = this.securityPanel.isDirty();
        boolean webConsoleDirty = isServicesUiDirty(this.consolePanel);
        boolean httpServiceDirty = isServicesUiDirty(this.httpServicePanel);

        return certListDirty || httpServiceDirty || securityDirty || webConsoleDirty;
    }

    public void addTab(final String name, final WidgetFactory widgetFactory) {

        final TabPane tabPane = new TabPane();
        tabPane.setId("__extension__" + name);

        final TabListItem item = new TabListItem(name);
        item.setDataTarget("#__extension__" + name);

        item.addClickHandler(e -> {
            tabPane.clear();
            tabPane.add(widgetFactory.buildWidget());
        });

        this.navTabs.add(item);
        this.tabContent.add(tabPane);
    }

    public void setDirty(boolean b) {
        this.certificateListPanel.setDirty(b);
        this.securityPanel.setDirty(b);
        getServicesUi(this.httpServicePanel).ifPresent(u -> u.setDirty(b));
        getServicesUi(this.consolePanel).ifPresent(u -> u.setDirty(b));
    }

    private ClickHandler addDirtyCheck(final ClickHandler handler) {
        return e -> {
            final TabListItem newSelection = (TabListItem) ((Anchor) e.getSource()).getParent();
            if (isDirty()) {
                this.alertDialog.show(MSGS.deviceConfigDirty(), accepted -> {
                    if (accepted) {
                        setDirty(false);
                        handler.onClick(e);
                        selected = newSelection;
                    } else {
                        selected.showTab();
                    }
                });
            } else {
                handler.onClick(e);
                selected = newSelection;
            }
        };
    }

    private static boolean isEnabledBooleanParameter(final GwtConfigParameter param, final String prefix) {
        return param.getId().startsWith(prefix) && "true".equals(param.getValue());
    }

    private static boolean isEmptyParameter(final GwtConfigParameter param, final String prefix) {
        return param.getId().startsWith(prefix) && (param.getValue() == null || param.getValue().trim().isEmpty());
    }

    private Validator getConsoleOptionsValidator() {
        return config -> {
            final List<ValidationResult> result = new ArrayList<>();
            if (!config.getParameters().stream().anyMatch(p -> isEnabledBooleanParameter(p, "auth.method"))) {
                result.add(ValidationResult.warning(MSGS.securityAllAuthMethodsDisabled()));
            }
            return result;
        };
    }

    private Validator getHttpServiceOptionsValidator() {
        return config -> {
            final List<ValidationResult> result = new ArrayList<>();

            boolean isHttpEnabled = false;
            boolean isHttpsEnabled = false;
            boolean isHttpsClientAuthEnabled = false;
            boolean haveHttpsKeystorePath = true;
            boolean haveHttpsKeystorePassword = true;
            final Set<Integer> ports = new HashSet<>();

            for (final GwtConfigParameter p : config.getParameters()) {
                if (isEnabledBooleanParameter(p, "http.enabled")) {
                    isHttpEnabled = true;
                }
                if (isEnabledBooleanParameter(p, "https.enabled")) {
                    isHttpsEnabled = true;
                }
                if (isEnabledBooleanParameter(p, "https.client.auth.enabled")) {
                    isHttpsClientAuthEnabled = true;
                }
                if (p.getId().endsWith("port")) {
                    try {
                        ports.add(Integer.parseInt(p.getValue()));
                    } catch (final Exception e) {
                        // do nothing
                    }
                }
                if (isEmptyParameter(p, "https.keystore.path")) {
                    haveHttpsKeystorePath = false;
                }
                if (isEmptyParameter(p, "https.keystore.password")) {
                    haveHttpsKeystorePassword = false;
                }
            }

            if (!isHttpEnabled && !isHttpsEnabled && !isHttpsClientAuthEnabled) {
                result.add(ValidationResult.warning(MSGS.securityAllConnectorsDisabled()));
            }

            if (ports.size() != 3) {
                result.add(ValidationResult.error(MSGS.securityPortConflictOrInvalidPort()));
            }

            if ((isHttpsEnabled || isHttpsClientAuthEnabled)
                    && (!haveHttpsKeystorePath || !haveHttpsKeystorePassword)) {
                result.add(ValidationResult.error(MSGS.securityHttpsEnabledButKeystoreParametersMissing()));
            }

            return result;
        };
    }

    public Optional<ServicesUi> getServicesUi(final IndexedPanel parent) {

        for (int i = 0; i < parent.getWidgetCount(); i++) {
            final Widget child = parent.getWidget(i);

            if (child instanceof ServicesUi) {
                return Optional.of((ServicesUi) child);
            }
        }

        return Optional.empty();
    }

    public boolean isServicesUiDirty(final IndexedPanel parent) {
        return getServicesUi(parent).map(ServicesUi::isDirty).orElse(false);
    }
}
