package io.github.beelzebu.coins.api.utils;

import java.util.Collection;
import java.util.LinkedHashSet;
import lombok.NoArgsConstructor;

/**
 * @author Beelzebu
 */
@NoArgsConstructor
public class CoinsSet <E> extends LinkedHashSet<E> {

    public CoinsSet(Collection<? extends E> collection) {
        super(collection);
    }

    public CoinsSet<E> getFirst(int amount) {
        CoinsSet<E> set = new CoinsSet<>();
        for (E e : this) {
            if (set.size() < amount) {
                set.add(e);
            } else {
                break;
            }
        }
        return set;
    }
}
