package io.github.beelzebu.coins.api.utils;

import java.util.Map;
import lombok.AllArgsConstructor;

/**
 * @author Beelzebu
 */
@AllArgsConstructor
public class CoinsEntry <K, V> implements Map.Entry<K, V> {

    private K key;
    private V value;

    @Override
    public K getKey() {
        return key;
    }

    public K setKey(K k) {
        return key = k;
    }

    @Override
    public V getValue() {
        return value;
    }

    @Override
    public V setValue(V v) {
        return value = v;
    }
}
