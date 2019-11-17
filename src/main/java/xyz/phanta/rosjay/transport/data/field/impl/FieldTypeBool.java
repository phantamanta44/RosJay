package xyz.phanta.rosjay.transport.data.field.impl;

import xyz.phanta.rosjay.transport.data.field.RosDataFieldType;
import xyz.phanta.rosjay.util.deserchain.DeserializationChain;
import xyz.phanta.rosjay.util.deserchain.DeserializationLink;

import javax.annotation.Nullable;
import java.io.DataOutput;
import java.io.IOException;
import java.util.function.Consumer;

public class FieldTypeBool implements RosDataFieldType<Boolean> {

    public static final FieldTypeBool TYPE = new FieldTypeBool();

    private FieldTypeBool() {
        // NO-OP
    }

    @Override
    public Boolean getDefaultValue() {
        return false;
    }

    @Override
    public void serializeField(Boolean value, DataOutput dest, int seqIndex) throws IOException {
        dest.write(value ? (byte)1 : (byte)0);
    }

    @Nullable
    @Override
    public DeserializationChain linkDeserializer(@Nullable DeserializationChain next, Consumer<Boolean> callback) {
        return new DeserializationLink(next, 1, (buf, length) -> callback.accept(buf.get() != 0));
    }

    @Override
    public String toString() {
        return "bool";
    }

}
