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
 *******************************************************************************/
package org.eclipse.kura.core.keystore.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MappingCollection<T, U> implements Collection<U> {

    private final Collection<T> source;
    private final Function<T, U> func;

    public MappingCollection(final Collection<T> source, final Function<T, U> func) {
        this.source = source;
        this.func = func;
    }

    @Override
    public int size() {
        return source.size();
    }

    @Override
    public boolean isEmpty() {
        return source.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return source.stream().map(func).collect(Collectors.toList()).contains(o);
    }

    @Override
    public Iterator<U> iterator() {
        final Iterator<T> iter = source.iterator();
        return new Iterator<U>() {

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public U next() {
                return func.apply(iter.next());
            }
        };
    }

    @Override
    public Object[] toArray() {
        return source.stream().map(func).collect(Collectors.toList()).toArray();
    }

    @Override
    public <V> V[] toArray(V[] a) {
        return source.stream().map(func).collect(Collectors.toList()).toArray(a);
    }

    @Override
    public boolean add(U e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends U> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

}