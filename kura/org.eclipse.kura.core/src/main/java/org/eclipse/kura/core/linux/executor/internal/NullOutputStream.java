/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Stefan Bischof
 *******************************************************************************/
package org.eclipse.kura.core.linux.executor.internal;

import java.io.IOException;
import java.io.OutputStream;

//when JDK 11 use `java.io.OutputStream.nullOutputStream();`
public class NullOutputStream extends OutputStream{

	@Override
	public void write(int b) throws IOException {
		
	}
	@Override
	public void write(byte[] b) throws IOException {

	}
	@Override
	public void write(byte[] b, int off, int len) throws IOException {

	}

}