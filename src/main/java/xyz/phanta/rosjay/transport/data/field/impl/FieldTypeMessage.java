package xyz.phanta.rosjay.transport.data.field.impl;

import xyz.phanta.rosjay.transport.data.RosData;
import xyz.phanta.rosjay.transport.data.field.RosDataFieldType;
import xyz.phanta.rosjay.transport.msg.RosMessageType;
import xyz.phanta.rosjay.util.deserchain.DeserializationChain;
import xyz.phanta.rosjay.util.deserchain.DeserializationComposer;

import javax.annotation.Nullable;
import java.io.DataOutput;
import java.io.IOException;
import java.util.function.Consumer;

public class FieldTypeMessage<T extends RosData<T>> implements RosDataFieldType<T> {

    private final RosMessageType<T> messageType;

    public FieldTypeMessage(RosMessageType<T> messageType) {
        this.messageType = messageType;
    }

    @Override
    public T getDefaultValue() {
        return messageType.newInstance();
    }

    @Override
    public void serializeField(T value, DataOutput dest, int seqIndex) throws IOException {
        value.serializeData(dest, seqIndex);
    }

    @Nullable
    @Override
    public DeserializationChain linkDeserializer(@Nullable DeserializationChain next, Consumer<T> callback) {
        T instance = messageType.newInstance();
        return new DeserializationComposer(next, instance::linkDeserializer, (buf, length) -> callback.accept(instance));
    }

    @Override
    public String toString() {
        return messageType.toString();
    }

}
