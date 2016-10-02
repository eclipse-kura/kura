/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.test;

import static org.junit.Assert.assertEquals;

import org.eclipse.kura.comm.CommURI;
import org.junit.Test;

public class CommURITest
{
	@Test
	public void testSyntax() 
		throws Exception
	{
		CommURI commUri = new CommURI.Builder("/dev/ttyUSB0").withBaudRate(4800).build();
		System.out.println(commUri);
		
		CommURI commUri1 = CommURI.parseString(commUri.toString());
		System.out.println(commUri1);
		
		assertEquals(commUri1.toString(), commUri.toString());
	}
}
