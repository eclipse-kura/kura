/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.internal.wire.asset;

import java.util.Map;

import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.type.TypedValue;

public interface RecordFiller {

    void fill(Map<String, TypedValue<?>> envelopeProperties, ChannelRecord record);
}
