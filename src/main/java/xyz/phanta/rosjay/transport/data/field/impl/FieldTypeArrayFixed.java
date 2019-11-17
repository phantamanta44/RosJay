package xyz.phanta.rosjay.transport.data.field.impl;

import xyz.phanta.rosjay.transport.data.field.RosDataFieldType;
import xyz.phanta.rosjay.util.deserchain.DeserChainUtils;
import xyz.phanta.rosjay.util.deserchain.DeserializationChain;

import javax.annotation.Nullable;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class FieldTypeArrayFixed<T> implements RosDataFieldType<List<T>> { // FIXME no idea if this is correct

    private final RosDataFieldType<T> elementType;
    private final int size;

    public FieldTypeArrayFixed(RosDataFieldType<T> elementType, int size) {
        this.elementType = elementType;
        this.size = size;
    }

    @Override
    public List<T> getDefaultValue() {
        //noinspection unchecked
        T[] arr = (T[])new Object[size];
        for (int i = 0; i < size; i++) {
            arr[i] = elementType.getDefaultValue();
        }
        return Arrays.asList(arr);
    }

    @Override
    public void serializeField(List<T> value, DataOutput dest, int seqIndex) throws IOException {
        for (int i = 0; i < size; i++) {
            elementType.serializeField(value.get(i), dest, 0);
        }
    }

    @Nullable
    @Override
    public DeserializationChain linkDeserializer(@Nullable DeserializationChain next, Consumer<List<T>> callback) {
        //noinspection unchecked
        T[] arr = (T[])new Object[size];
        return DeserChainUtils.iterate(next, size, (subNext, i) -> elementType.linkDeserializer(subNext, elem -> {
            arr[i] = elem;
            if (i == arr.length - 1) {
                callback.accept(Arrays.asList(arr));
            }
        }));
    }

    @Override
    public String toString() {
        return elementType.toString() + "[" + size + "]";
    }

}
