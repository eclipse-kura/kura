/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.client.util;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.Panel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.StatusCodeException;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

public class FailureHandler {

    private static final Messages MSGS = GWT.create(Messages.class);
    private static Modal popup;
    private static Label errorMessageLabel;
    private static VerticalPanel errorStackTrace;
    private static Panel stackTraceContainer;

    private FailureHandler() {
    }

    public static void handle(Throwable caught, String name) {
        if (caught instanceof StatusCodeException) {
            final StatusCodeException statusCodeException = (StatusCodeException) caught;
            if (statusCodeException.getStatusCode() == 401) {
                showErrorMessage("The session has expired", null);
                Timer timer = new Timer() {

                    @Override
                    public void run() {
                        Window.Location.reload();
                    }
                };

                timer.schedule(2000);
                return;
            }
        }
        printMessage(caught, name);
    }

    public static void handle(Throwable caught) {
        handle(caught, "");
    }

    public static void showErrorMessage(String message) {
        showErrorMessage("Warning", message, null);
    }

    public static void showErrorMessage(final String message, final StackTraceElement[] stackTrace) {
        showErrorMessage("Warning", message, stackTrace);
    }

    public static void showErrorMessage(final String title, final String message,
            final StackTraceElement[] stackTrace) {
        popup.setTitle(title);

        errorMessageLabel.setText(message);

        if (stackTrace == null) {
            stackTraceContainer.setVisible(false);
        } else {
            errorStackTrace.clear();

            for (StackTraceElement element : stackTrace) {
                Label tempLabel = new Label();
                tempLabel.setText(element.toString());
                errorStackTrace.add(tempLabel);
            }
            stackTraceContainer.setVisible(true);
        }
        popup.show();
    }

    private static void printMessage(Throwable caught, String name) {
        StringBuilder errorMessageBuilder = new StringBuilder();
        if (name != null && !"".equals(name.trim())) {
            errorMessageBuilder.append(name);
            errorMessageBuilder.append(": ");
        }

        if (caught instanceof GwtKuraException) {

            GwtKuraException gee = (GwtKuraException) caught;
            GwtKuraErrorCode code = gee.getCode();

            if (code == GwtKuraErrorCode.DUPLICATE_NAME) {
                errorMessageBuilder.append(MSGS.duplicateNameError());
            } else if (code == GwtKuraErrorCode.CONNECTION_FAILURE) {
                errorMessageBuilder.append(MSGS.connectionFailure());
            } else if (code == GwtKuraErrorCode.ILLEGAL_ARGUMENT) {
                errorMessageBuilder.append(MSGS.illegalArgumentError());
            } else if (code == GwtKuraErrorCode.ILLEGAL_NULL_ARGUMENT) {
                errorMessageBuilder.append(MSGS.illegalNullArgumentError());
            } else {
                errorMessageBuilder.append(MSGS.genericError());
            }

        } else if (caught instanceof StatusCodeException && ((StatusCodeException) caught).getStatusCode() == 0) {
            // the current operation was interrupted as the user started a new one
            // or navigated away from the page.
            // we can ignore this error and do nothing.
            return;
        } else {
            String localizedMessage = caught.getLocalizedMessage();

            if (!"".equals(localizedMessage)) {
                errorMessageBuilder.append(localizedMessage);
            } else {
                errorMessageBuilder.append(MSGS.genericError());
            }
        }

        showErrorMessage(errorMessageBuilder.toString(), caught.getStackTrace());
    }

    public static void setPopup(Modal uiElement, Label errorMessage, VerticalPanel errorStackTraceArea,
            Panel stackTraceContainerArea) {
        popup = uiElement;
        errorMessageLabel = errorMessage;
        errorStackTrace = errorStackTraceArea;
        stackTraceContainer = stackTraceContainerArea;
    }
}
