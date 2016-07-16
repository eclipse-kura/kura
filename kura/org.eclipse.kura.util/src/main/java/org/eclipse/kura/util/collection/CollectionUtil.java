/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.util.collection;

import static org.eclipse.kura.Preconditions.checkCondition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.UtilMessages;

/**
 * The Class CollectionUtil contains all necessary static factory methods to
 * deal with different collection instances
 */
public final class CollectionUtil {

	/** Localization Resource. */
	private static final UtilMessages s_message = LocalizationAdapter.adapt(UtilMessages.class);

	/** Constructor */
	private CollectionUtil() {
		// Static Factory Methods container. No need to instantiate.
	}

	/**
	 * Creates a <i>mutable</i>, empty {@code ArrayList} instance.
	 *
	 * @param <E>
	 *            the element type
	 * @return empty ArrayList instance
	 */
	public static <E> List<E> newArrayList() {
		return new ArrayList<E>();
	}

	/**
	 * Creates an {@code ArrayList} instance backed by an array with the
	 * specified initial size; simply delegates to
	 * {@link ArrayList#ArrayList(int)}.
	 *
	 * @param <E>
	 *            the element type
	 * @param initialArraySize
	 *            the initial capacity
	 * @return empty {@code ArrayList} instance with the provided capacity
	 */
	public static <E> List<E> newArrayListWithCapacity(final int initialArraySize) {
		checkCondition(initialArraySize < 0, s_message.initialArraySize());
		return new ArrayList<E>(initialArraySize);
	}

	/**
	 * Creates a <i>mutable</i>, empty {@code ConcurrentHashMap} instance.
	 *
	 * @param <K>
	 *            the key type
	 * @param <V>
	 *            the value type
	 * @return a new, empty {@code ConcurrentHashMap}
	 */
	public static <K, V> ConcurrentMap<K, V> newConcurrentHashMap() {
		return new ConcurrentHashMap<K, V>();
	}

	/**
	 * Creates a <i>mutable</i>, empty {@code ConcurrentHashMap} instance.
	 *
	 * @param <K>
	 *            the key type
	 * @param <V>
	 *            the value type
	 * @param map
	 *            the map to contain
	 * @return a new, empty {@code ConcurrentHashMap}
	 */
	public static <K, V> ConcurrentMap<K, V> newConcurrentHashMap(final Map<K, V> map) {
		return new ConcurrentHashMap<K, V>(map);
	}

	/**
	 * Creates an empty {@code CopyOnWriteArrayList} instance.
	 *
	 * @param <E>
	 *            the element type
	 * @return a new, empty {@code CopyOnWriteArrayList}
	 */
	public static <E> List<E> newCopyOnWriteArrayList() {
		return new CopyOnWriteArrayList<E>();
	}

	/**
	 * Creates a <i>mutable</i>, empty {@code HashMap} instance.
	 *
	 * @param <K>
	 *            the key type
	 * @param <V>
	 *            the value type
	 * @return a new, empty {@code HashMap}
	 */
	public static <K, V> Map<K, V> newHashMap() {
		return new HashMap<K, V>();
	}

	/**
	 * Creates a <i>mutable</i> {@code HashMap} instance with the same mappings
	 * as the specified map.
	 *
	 * @param <K>
	 *            the key type
	 * @param <V>
	 *            the value type
	 * @param map
	 *            map the mappings to be inserted
	 * @return a new {@code HashMap}
	 */
	public static <K, V> Map<K, V> newHashMap(final Map<? extends K, ? extends V> map) {
		return new HashMap<K, V>(map);
	}

	/**
	 * Creates a <i>mutable</i>, initially empty {@code HashSet} instance.
	 *
	 * @param <E>
	 *            the element type
	 * @return a new, empty {@code HashSet}
	 */
	public static <E> Set<E> newHashSet() {
		return new HashSet<E>();
	}

	/**
	 * Creates a <i>mutable</i>, empty, insertion-ordered {@code LinkedHashMap}
	 * instance.
	 *
	 * @param <K>
	 *            the key type
	 * @param <V>
	 *            the value type
	 * @return a new, empty {@code LinkedHashMap}
	 */
	public static <K, V> Map<K, V> newLinkedHashMap() {
		return new LinkedHashMap<K, V>();
	}

	/**
	 * Creates a <i>mutable</i>, empty {@code LinkedList} instance (for Java 6
	 * and earlier).
	 *
	 * @param <E>
	 *            the element type
	 * @return the Linked List
	 */
	public static <E> List<E> newLinkedList() {
		return new LinkedList<E>();
	}

	/**
	 * Creates a <i>mutable</i>, empty {@code TreeMap} instance using the
	 * natural ordering of its elements.
	 *
	 * @param <K>
	 *            the key type
	 * @param <V>
	 *            the value type
	 * @return a new, empty {@code TreeMap}
	 */
	public static <K extends Comparable<K>, V> Map<K, V> newTreeMap() {
		return new TreeMap<K, V>();
	}

}
