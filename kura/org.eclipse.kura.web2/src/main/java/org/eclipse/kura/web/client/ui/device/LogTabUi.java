/*******************************************************************************
 * Copyright (c) 2019, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.client.ui.device;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.kura.web.Console;
import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.client.util.LogPollService;
import org.eclipse.kura.web.shared.model.GwtLogEntry;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtLogService;
import org.eclipse.kura.web.shared.service.GwtLogServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.CheckBox;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.Panel;
import org.gwtbootstrap3.client.ui.Row;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

public class LogTabUi extends Composite {

    private static LogTabUiUiBinder uiBinder = GWT.create(LogTabUiUiBinder.class);

    private static final Messages MSGS = GWT.create(Messages.class);
    private static final String SERVLET_URL = Console.ADMIN_ROOT + '/' + GWT.getModuleName() + "/log";

    @SuppressWarnings("unused")
    private GwtSession session;

    interface LogTabUiUiBinder extends UiBinder<Widget, LogTabUi> {
    }

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtLogServiceAsync gwtLogService = GWT.create(GwtLogService.class);

    @UiField
    Button execute;
    @UiField
    FormPanel logForm;
    @UiField
    FormLabel logLabel;
    @UiField
    Panel deviceLogsPanel;
    @UiField
    Row controlsRow;
    @UiField
    TextArea logTextArea;
    @UiField
    ListBox logProviderListBox;
    @UiField
    CheckBox showStackTraceCheckbox;
    @UiField
    CheckBox showMoreInfoCheckbox;

    private final List<GwtLogEntry> logs = new LinkedList<>();
    private boolean hasLogProvider = false;
    private boolean autoFollow = true;

    public LogTabUi() {
        initWidget(uiBinder.createAndBindUi(this));

        this.logLabel.setText(MSGS.logDownload());

        this.execute.setText(MSGS.download());
        this.execute.addClickHandler(
                event -> LogTabUi.this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        FailureHandler.handle(ex);
                    }

                    @Override
                    public void onSuccess(GwtXSRFToken token) {
                        LogTabUi.this.logForm.submit();
                    }
                }));

        this.logForm.setEncoding(FormPanel.ENCODING_MULTIPART);
        this.logForm.setMethod(FormPanel.METHOD_GET);
        this.logForm.setAction(SERVLET_URL);

        this.logTextArea.setVisibleLines(30);
        this.logTextArea.setReadOnly(true);
        this.logTextArea.addFocusHandler(focus -> LogTabUi.this.autoFollow = false);

        initLogProviderListBox();

        this.showStackTraceCheckbox.setValue(true);
        this.showMoreInfoCheckbox.setValue(false);
        this.showStackTraceCheckbox.addClickHandler(click -> displayLogs());
        this.showMoreInfoCheckbox.addClickHandler(click -> displayLogs());

        LogPollService.subscribe(entries -> {
            LogTabUi.this.logs.addAll(entries);
            displayLogs();
        });
    }

    @Override
    public void onAttach() {
        super.onAttach();

        if (this.hasLogProvider) {
            LogPollService.startLogPolling();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (this.hasLogProvider) {
            LogPollService.stopLogPolling();
        }
    }

    public void setSession(GwtSession currentSession) {
        this.session = currentSession;
    }

    private void initLogProviderListBox() {
        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable caught) {
                hideLogSection();
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                LogTabUi.this.gwtLogService.initLogProviders(token, new AsyncCallback<List<String>>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        hideLogSection();
                    }

                    @Override
                    public void onSuccess(List<String> pids) {
                        if (pids.isEmpty()) {
                            hideLogSection();
                        } else {
                            LogTabUi.this.hasLogProvider = true;
                            LogTabUi.this.deviceLogsPanel.setVisible(true);

                            for (String pid : pids) {
                                LogTabUi.this.logProviderListBox.addItem(pid, pid);
                            }

                            LogTabUi.this.logProviderListBox.addChangeHandler(changeEvent -> displayLogs());
                        }
                    }

                });
            }

        });
    }

    private void displayLogs() {
        StringBuilder displayedText = new StringBuilder();

        for (GwtLogEntry entry : this.logs) {
            if (this.logProviderListBox.getSelectedValue().equals(entry.getSourceLogProviderPid())) {
                displayedText.append(entry.prettyPrint(this.showMoreInfoCheckbox.getValue().booleanValue(),
                        this.showStackTraceCheckbox.getValue().booleanValue()));
            }
        }

        this.logTextArea.setText(displayedText.toString());
        if (this.autoFollow) {
            this.logTextArea.getElement().setScrollTop(this.logTextArea.getElement().getScrollHeight());
        }
    }

    private void hideLogSection() {
        this.hasLogProvider = false;
        this.deviceLogsPanel.setVisible(false);
    }
}
