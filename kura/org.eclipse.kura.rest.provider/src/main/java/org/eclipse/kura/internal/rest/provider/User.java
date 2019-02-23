/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
 
package org.eclipse.kura.internal.rest.provider;

import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.kura.configuration.Password;

public class User implements Principal {

    private final String name;
    private final Password password;
    private final Set<String> roles;

    public User(String name, Password password, Set<String> roles) {
        super();
        this.name = name;
        this.password = password;
        this.roles = roles;
    }

    @Override
    public String getName() {
        return name;
    }

    public Password getPassword() {
        return password;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public static Map<String, User> fromOptions(RestServiceOptions options) {
        final String[] userNames = trimAll(options.getUserNames());
        final String[] passwords = options.getPasswords();
        final String[] roles = options.getRoles();

        final int userCount = Math.min(userNames.length, roles.length);
        final Map<String, User> result = new HashMap<>();

        for (int i = 0; i < userCount; i++) {
            final String userName = userNames[i];
            final String rolesList = roles[i];

            if (userName == null || rolesList == null) {
                continue;
            }

            String password = getPassword(passwords, i);

            result.put(userName, new User(userName, new Password(password),
                    Arrays.stream(trimAll(rolesList.split(";"))).collect(Collectors.toSet())));
        }

        return result;
    }

    private static String getPassword(String[] passwords, int index) {
        String password = null;
        if (index < passwords.length) {
            password = passwords[index];
        }
        if (password == null) {
            password = "";
        }
        return password;
    }

    private static String[] trimAll(final String[] strings) {
        for (int j = 0; j < strings.length; j++) {
            strings[j] = strings[j].trim();
        }
        return strings;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        User other = (User) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }
}
