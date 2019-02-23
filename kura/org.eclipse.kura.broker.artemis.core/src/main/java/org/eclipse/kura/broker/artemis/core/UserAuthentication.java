/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.broker.artemis.core;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public final class UserAuthentication {

    public static class User {

        private String name;
        private String password;
        private Set<String> roles;

        public User(final String name, final String password, final Set<String> roles) {
            this.name = requireNonNull(name);
            this.password = requireNonNull(password);
            this.roles = Collections.unmodifiableSet(new HashSet<>(requireNonNull(roles)));
        }

        public String getName() {
            return this.name;
        }

        public String getPassword() {
            return this.password;
        }

        public Set<String> getRoles() {
            return this.roles;
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

    public static class Builder {

        private String defaultUser;

        private Set<User> users = new HashSet<>();

        public Builder defaultUser(final String defaultUser) {
            this.defaultUser = defaultUser;
            return this;
        }

        public Builder addUser(final String name, final String password, final Set<String> roles) {
            return addUser(new User(name, password, roles));
        }

        public Builder addUser(final User user) {
            this.users.add(requireNonNull(user));
            return this;
        }

        public Builder parseUsers(final String string) {

            if (string == null || string.isEmpty()) {
                return this;
            }

            final Properties p = new Properties();
            try {
                p.load(new StringReader(string));
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }

            for (final String key : p.stringPropertyNames()) {
                final String value = p.getProperty(key);
                final String[] toks = value.split("\\|", 2);

                if (toks.length == 2) {
                    final String password = toks[0];

                    final String[] roles = toks[1].split("\\s*,\\s*");
                    addUser(key, password, new HashSet<>(Arrays.asList(roles)));
                }
            }

            return this;
        }

        public UserAuthentication build() {
            return new UserAuthentication(this.defaultUser, this.users);
        }
    }

    private String defaultUser;
    private Set<User> users;

    private UserAuthentication(final String defaultUser, final Set<User> users) {
        this.defaultUser = defaultUser;
        this.users = Collections.unmodifiableSet(users);
    }

    public String getDefaultUser() {
        return this.defaultUser;
    }

    public Set<User> getUsers() {
        return this.users;
    }

}
