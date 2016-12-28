/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.kura.camel.type;

import java.util.Date;
import java.util.Map;

import org.apache.camel.Converter;
import org.eclipse.kura.message.KuraPayload;

@Converter
public class TypeConverter {

    @Converter
    public static KuraPayload fromMap(final Map<String, ?> data) {
        if (data == null) {
            return null;
        }

        final KuraPayload result = new KuraPayload();
        result.setTimestamp(new Date());

        for (final Map.Entry<String, ?> entry : data.entrySet()) {
            result.addMetric(entry.getKey(), entry.getValue());
        }

        return result;
    }
}
