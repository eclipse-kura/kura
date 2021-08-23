/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.core.internal.linux.executor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.exec.CommandLine;
import org.mockito.ArgumentMatcher;

public class CommandLineMatcher extends ArgumentMatcher<CommandLine> {

    String executable;
    List<String> arguments;

    CommandLineMatcher(String executable, String[] arguments) {
        this.executable = executable;
        this.arguments = new ArrayList<>();
        Arrays.asList(arguments).forEach(this.arguments::add);
    }

    @Override
    public String toString() {
        return executable + " " + String.join(" ", arguments);
    }

    @Override
    public boolean matches(Object argument) {
        boolean matched = false;
        if (argument instanceof CommandLine) {
            CommandLine cl = (CommandLine) argument;
            if (this.executable.equals(cl.getExecutable()) && this.arguments.size() == cl.getArguments().length
                    && this.arguments.containsAll(Arrays.asList(cl.getArguments()))) {
                matched = true;
            }
        }
        return matched;
    }
}
