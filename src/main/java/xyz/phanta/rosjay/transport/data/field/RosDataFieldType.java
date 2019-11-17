package xyz.phanta.rosjay.transport.data.field;

import xyz.phanta.rosjay.util.deserchain.DeserializationChain;

import javax.annotation.Nullable;
import java.io.DataOutput;
import java.io.IOException;
import java.util.function.Consumer;

public interface RosDataFieldType<T> {

    T getDefaultValue();

    void serializeField(T value, DataOutput dest, int seqIndex) throws IOException;

    @Nullable
    DeserializationChain linkDeserializer(@Nullable DeserializationChain next, Consumer<T> callback);

}
