/*******************************************************************************
 * Copyright (c) 2016 Red hat Inc. and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jens Reimann <jreimann@redhat.com> initial implementation
 *******************************************************************************/

package org.eclipse.kura.linux.command.test;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.command.CommandService;
import org.eclipse.kura.linux.command.CommandServiceImpl;
import org.junit.Assert;
import org.junit.Test;

public class CommandServiceTest {
	@Test
	public void test1 () throws KuraException
	{
		test ( "echo foo", "foo");
		test ( "echo bar", "bar");
		test ( "(>&2 echo foo) && echo bar", "bar");
		test ( "(>&2 echo foo) || echo bar", "");
		test ( "(>&2 echo foo) ; echo bar ; false", "foo");
	}
	
	protected void test ( String cmd, String expectedResult ) throws KuraException
	{
		CommandService cs = new CommandServiceImpl();
		String result = cs.execute(cmd);
		Assert.assertEquals(expectedResult, result);
	}
}
