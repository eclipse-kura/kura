/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc
 *******************************************************************************/

package org.eclipse.kura.linux.command;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.command.CommandService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CommandServiceTest {

    private CommandService service;

    @Before
    public void setup() {
        this.service = new CommandServiceImpl();
    }

    @Test
    public void testNull() throws KuraException {
        assertExecute(null, "<empty command>");
    }

    @Test
    public void testSimple1() throws KuraException {
        assertExecute("echo -n foo", "foo");
        assertExecute("echo -n bar", "bar");
        assertExecute("(>&2 echo -n foo) && echo -n bar", "bar");
        assertExecute("(>&2 echo -n foo) || echo -n bar", "");
        assertExecute("(>&2 echo -n foo) ; echo -n bar ; false", "foo");
    }

    @Test
    public void testCommandNotFound() throws KuraException {
        // expect not failure, just a shell specific error message
        this.service.execute("command-not-found");
    }

    private void assertExecute(String command, String expected) throws KuraException {
        final String result = this.service.execute(command);
        Assert.assertEquals("Command output", expected, result);
    }
}
