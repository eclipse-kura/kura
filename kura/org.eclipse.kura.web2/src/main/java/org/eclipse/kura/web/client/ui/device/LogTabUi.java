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

import java.util.Iterator;
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
    ListBox logReaderListBox;
    @UiField
    CheckBox showStackTraceCheckbox;
    @UiField
    CheckBox showMoreInfoCheckbox;
    @UiField
    ListBox displayedRowsNumberListBox;

    private List<GwtLogEntry> logs = new LinkedList<>();
    private boolean hasLogReader = false;
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
        this.logTextArea.setWidth("95%");
        this.logTextArea.addFocusHandler(focus -> {
            LogTabUi.this.autoFollow = false;
        });

        this.displayedRowsNumberListBox.addItem("10", "10");
        this.displayedRowsNumberListBox.addItem("100", "100");
        this.displayedRowsNumberListBox.addItem("500", "500");
        this.displayedRowsNumberListBox.addItem("1000", "1000");
        this.displayedRowsNumberListBox.setSelectedIndex(1);
        this.displayedRowsNumberListBox.addChangeHandler(change -> displayLogs());

        initLogReaderListBox();

        this.showStackTraceCheckbox.setValue(false);
        this.showMoreInfoCheckbox.setValue(false);
        this.showStackTraceCheckbox.addClickHandler(click -> displayLogs());
        this.showMoreInfoCheckbox.addClickHandler(click -> displayLogs());

        LogPollService.subscribe(new LogPollService.LogListener() {

            @Override
            public void onLogsReceived(List<GwtLogEntry> entries) {
                LogTabUi.this.logs.addAll(entries);
                displayLogs();
            }

        });
    }

    @Override
    public void onAttach() {
        super.onAttach();
        if (this.hasLogReader) {
            LogPollService.getInstance().startLogPolling();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (this.hasLogReader) {
            LogPollService.getInstance().stopLogPolling();
        }
    }

    public void setSession(GwtSession currentSession) {
        this.session = currentSession;
    }

    private void initLogReaderListBox() {
        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable caught) {
                hideLogSection();
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                LogTabUi.this.gwtLogService.initLogReaders(token, new AsyncCallback<List<String>>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        hideLogSection();
                    }

                    @Override
                    public void onSuccess(List<String> pids) {
                        if (pids.isEmpty()) {
                            hideLogSection();
                        } else {
                            LogTabUi.this.hasLogReader = true;
                            LogTabUi.this.deviceLogsPanel.setVisible(true);

                            for (String pid : pids) {
                                LogTabUi.this.logReaderListBox.addItem(pid, pid);
                            }

                            LogTabUi.this.logReaderListBox.addChangeHandler(changeEvent -> displayLogs());
                        }
                    }

                });
            }

        });
    }

    private void displayLogs() {
        StringBuilder displayedText = new StringBuilder();

        int rowsToDisplay = Integer.parseInt(this.displayedRowsNumberListBox.getSelectedValue());
        Iterator<GwtLogEntry> iterator = this.logs.iterator();
        for (int i = 0; i < (this.logs.size() - rowsToDisplay); i++) {
            iterator.next();
        }

        while (iterator.hasNext()) {
            GwtLogEntry entry = iterator.next();

            if (this.logReaderListBox.getSelectedValue().equals(entry.getSourceLogReaderPid())) {
                displayedText.append(entry.getSourceRealtimeTimestamp());
                displayedText.append(" [priority: ");
                displayedText.append(entry.getPriority());
                if (this.showMoreInfoCheckbox.getValue().booleanValue()) {
                    displayedText.append(" - PID: ");
                    displayedText.append(entry.getPid());
                    displayedText.append(" - syslog ID: ");
                    displayedText.append(entry.getSyslogIdentifier());
                    displayedText.append(" - source: ");
                    displayedText.append(entry.getTransport());
                }
                displayedText.append("]\nMessage: ");
                displayedText.append(entry.getMessage());
                if (this.showStackTraceCheckbox.getValue().booleanValue() && entry.getStacktrace() != null
                        && !entry.getStacktrace().equals("undefined")) {
                    displayedText.append("\nStacktrace: ");
                    displayedText.append(entry.getStacktrace());
                }
                displayedText.append("\n");
            }
        }

        this.logTextArea.setText(displayedText.toString());
        if (this.autoFollow) {
            this.logTextArea.getElement().setScrollTop(this.logTextArea.getElement().getScrollHeight());
        }
    }

    private void hideLogSection() {
        this.hasLogReader = false;
        this.deviceLogsPanel.setVisible(false);
    }
}
