package org.eclipse.kura.example.wire.math.singleport;

import java.util.Map;

public class MapUtil {

    private MapUtil() {
    }

    public static <T> void computeIncrement(final Map<T, int[]> map, final T key) {
        final int[] newCount = new int[] { 0 };
        final int[] oldCount = map.put(key, newCount);
        if (oldCount != null) {
            newCount[0] = oldCount[0] + 1;
        }
    }

    public static <T> void computeDecrement(final Map<T, int[]> map, final T key) {
        final int[] newCount = new int[] { 0 };
        final int[] oldCount = map.put(key, newCount);
        if (!(oldCount == null || oldCount[0] == 0)) {
            newCount[0] = oldCount[0] - 1;
        } else {
            map.remove(key);
        }
    }
}
