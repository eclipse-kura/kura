package org.eclipse.kura.utils;

import java.util.List;
import java.util.Map;

import org.eclipse.kura.annotation.Nullable;

/**
 * Extension of the {@code Map} interface that stores multiple values.
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @param <K>
 *                the key type
 * @param <V>
 *                the value element type
 */
public interface MultiValueMap<K, V> extends Map<K, List<V>> {

    /**
     * Return the first value for the given key.
     * 
     * @param key
     *                the key
     * @return the first value for the specified key, or {@code null} if none
     */
    @Nullable
    V getFirst(K key);

    /**
     * Add the given single value to the current list of values for the given key.
     * 
     * @param key
     *                  the key
     * @param value
     *                  the value to be added
     */
    void add(K key, @Nullable V value);

    /**
     * Add all the values of the given list to the current list of values for the given key.
     * 
     * @param key
     *                   they key
     * @param values
     *                   the values to be added
     * @since 5.0
     */
    void addAll(K key, List<? extends V> values);

    /**
     * Add all the values of the given {@code MultiValueMap} to the current values.
     * 
     * @param values
     *                   the values to be added
     * @since 5.0
     */
    void addAll(MultiValueMap<K, V> values);

    /**
     * {@link #add(Object, Object) Add} the given value, only when the map does not
     * {@link #containsKey(Object) contain} the given key.
     * 
     * @param key
     *                  the key
     * @param value
     *                  the value to be added
     * @since 5.2
     */
    default void addIfAbsent(K key, @Nullable V value) {
        if (!containsKey(key)) {
            add(key, value);
        }
    }

    /**
     * Set the given single value under the given key.
     * 
     * @param key
     *                  the key
     * @param value
     *                  the value to set
     */
    void set(K key, @Nullable V value);

    /**
     * Set the given values under.
     * 
     * @param values
     *                   the values.
     */
    void setAll(Map<K, V> values);

    /**
     * Return a {@code Map} with the first values contained in this {@code MultiValueMap}.
     * 
     * @return a single value representation of this map
     */
    Map<K, V> toSingleValueMap();

}