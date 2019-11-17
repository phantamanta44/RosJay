package xyz.phanta.rosjay.transport.data.field.impl;

import xyz.phanta.rosjay.transport.data.field.RosDataFieldType;
import xyz.phanta.rosjay.util.deserchain.DeserChainUtils;
import xyz.phanta.rosjay.util.deserchain.DeserializationChain;
import xyz.phanta.rosjay.util.deserchain.DeserializationGenerator;

import javax.annotation.Nullable;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class FieldTypeArrayVariable<T> implements RosDataFieldType<List<T>> {

    private final RosDataFieldType<T> elementType;

    public FieldTypeArrayVariable(RosDataFieldType<T> elementType) {
        this.elementType = elementType;
    }

    @Override
    public List<T> getDefaultValue() {
        return Collections.emptyList();
    }

    @Override
    public void serializeField(List<T> value, DataOutput dest, int seqIndex) throws IOException {
        dest.writeInt(value.size());
        for (T element : value) {
            elementType.serializeField(element, dest, 0);
        }
    }

    @Nullable
    @Override
    public DeserializationChain linkDeserializer(@Nullable DeserializationChain next, Consumer<List<T>> callback) {
        return new DeserializationGenerator(next, 4, (buf, length, subNext) -> {
            //noinspection unchecked
            T[] arr = (T[])new Object[buf.getInt()];
            return DeserChainUtils.iterate(subNext, arr.length, (subSubNext, i) ->
                    elementType.linkDeserializer(subSubNext, elem -> {
                        arr[i] = elem;
                        if (i == arr.length - 1) {
                            callback.accept(Arrays.asList(arr));
                        }
                    }));
        });
    }

    @Override
    public String toString() {
        return elementType.toString() + "[]";
    }

}
