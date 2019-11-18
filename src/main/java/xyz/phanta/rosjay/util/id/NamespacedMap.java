package xyz.phanta.rosjay.util.id;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class NamespacedMap<T> {

    public static <T> NamespacedMap<T> concurrent() {
        //noinspection Convert2MethodRef
        return new NamespacedMap<>(() -> new ConcurrentHashMap<>());
    }

    private final BackingMapFactory<T> backingMapFactory;
    private final Map<RosId, T> backing;

    public NamespacedMap() {
        this(HashMap::new);
    }

    public NamespacedMap(BackingMapFactory<T> backingMapFactory) {
        this.backingMapFactory = backingMapFactory;
        this.backing = backingMapFactory.createBackingMap();
    }

    public void put(RosId id, T value) {
        backing.put(id, value);
    }

    @Nullable
    public T get(RosId id) {
        return backing.get(id);
    }

    @Nullable
    public T resolve(RosNamespace ns, String name) {
        T value = backing.get(ns.resolveId(name));
        return value != null ? value : backing.get(RosId.resolveGlobal(name));
    }

    public boolean containsKey(RosId id) {
        return backing.containsKey(id);
    }

    public boolean containsMatch(RosNamespace ns, String name) {
        return backing.containsKey(ns.resolveId(name)) || backing.containsKey(RosId.resolveGlobal(name));
    }

    public void remove(RosId id) {
        backing.remove(id);
    }

    public void clear() {
        backing.clear();
    }

    public int getSize() {
        return backing.size();
    }

    public boolean isEmpty() {
        return backing.isEmpty();
    }

    public Set<Map.Entry<RosId, T>> entrySet() {
        return backing.entrySet();
    }

    public T computeIfAbsent(RosId id, Supplier<T> factory) {
        T value = get(id);
        if (value == null) {
            value = factory.get();
            put(id, value);
        }
        return value;
    }

    @Override
    public int hashCode() {
        return backing.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof NamespacedMap && backing.equals(((NamespacedMap)obj).backing);
    }

    @Override
    public String toString() {
        return backing.toString();
    }

    @FunctionalInterface
    public interface BackingMapFactory<T> {

        Map<RosId, T> createBackingMap();

    }

}
