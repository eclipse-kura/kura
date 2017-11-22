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
package org.eclipse.kura.web.client.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.gwtbootstrap3.client.ui.Modal;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.StatusCodeException;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

public class FailureHandler {

    private static final Messages MSGS = GWT.create(Messages.class);
    private static Logger logger = Logger.getLogger("ErrorLogger");
    private static Modal popup;
    private static Label errorMessageLabel;
    private static VerticalPanel errorStackTrace;

    private FailureHandler() {
    }

    public static void handle(Throwable caught, String name) {
        printMessage(caught, name);
    }

    public static void handle(String caught) {
        printMessage("");
    }

    // add methode
    private static void printMessage(String error) {
        StringBuilder errorMessageBuilder = new StringBuilder();
        errorMessageBuilder.append(error);
    }

    // add methode
    public static void handle(Throwable caught) {
        printMessage(caught, "");
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

            switch (code) {
            case DUPLICATE_NAME:
                errorMessageBuilder.append(MSGS.duplicateNameError());
                break;
            case CONNECTION_FAILURE:
                errorMessageBuilder.append(MSGS.connectionFailure());
                break;
            case ILLEGAL_ARGUMENT:
                errorMessageBuilder.append(MSGS.illegalArgumentError());
                break;
            case ILLEGAL_NULL_ARGUMENT:
                errorMessageBuilder.append(MSGS.illegalNullArgumentError());
                break;

            case CANNOT_REMOVE_LAST_ADMIN:
            case ILLEGAL_ACCESS:
            case INVALID_USERNAME_PASSWORD:
            case INVALID_RULE_QUERY:
            case INTERNAL_ERROR:
            case OVER_RULE_LIMIT:
            case UNAUTHENTICATED:
            case WARNING:
            case CURRENT_ADMIN_PASSWORD_DOES_NOT_MATCH:
            case OPERATION_NOT_SUPPORTED:
            case SERVICE_NOT_ENABLED:
            default:
                errorMessageBuilder.append(MSGS.genericError());
                break;
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

        logger.log(Level.INFO, errorMessageBuilder.toString(), caught);
        errorMessageLabel.setText(errorMessageBuilder.toString());

        errorStackTrace.clear();
        for (StackTraceElement element : caught.getStackTrace()) {
            Label tempLabel = new Label();
            tempLabel.setText(element.toString());
            errorStackTrace.add(tempLabel);
        }

        popup.show();
    }

    public static void setPopup(Modal uiElement, Label errorMessage, VerticalPanel errorStackTraceArea) {
        popup = uiElement;
        errorMessageLabel = errorMessage;
        errorStackTrace = errorStackTraceArea;
    }
}
