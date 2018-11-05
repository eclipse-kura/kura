/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/

package org.eclipse.kura.driver.block.task;

import java.nio.charset.StandardCharsets;

import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.driver.binary.ByteArray;
import org.eclipse.kura.driver.binary.adapter.StringData;

public class StringTask extends BinaryDataTask<String> {

    public StringTask(ChannelRecord record, int start, int end, Mode mode) {
        super(record, start, new StringData(new ByteArray(end - start), StandardCharsets.US_ASCII), mode);
    }
}