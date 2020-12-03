/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.driver.block.task;

import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.driver.binary.ByteArray;

public class ByteArrayTask extends BinaryDataTask<byte[]> {

    public ByteArrayTask(ChannelRecord record, int start, int end, Mode mode) {
        super(record, start, new ByteArray(end - start), mode);
    }
}
