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

import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.driver.binary.ByteArray;

public class ByteArrayTask extends BinaryDataTask<byte[]> {

    public ByteArrayTask(ChannelRecord record, int start, int end, Mode mode) {
        super(record, start, new ByteArray(end - start), mode);
    }
}
