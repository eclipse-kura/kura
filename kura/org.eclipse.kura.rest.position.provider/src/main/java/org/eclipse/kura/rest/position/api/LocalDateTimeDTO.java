/*******************************************************************************
 * Copyright (c) 2021, 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.rest.position.api;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class LocalDateTimeDTO {

    String localDateTime;

    public LocalDateTimeDTO(LocalDateTime localDateTime) {
        this.localDateTime = localDateTime.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_INSTANT);
    }

    public String getLocalDateTime() {
        return this.localDateTime;
    }

}