package xyz.phanta.rosjay.transport.data.field.impl;

import xyz.phanta.rosjay.transport.data.field.RosDataFieldType;
import xyz.phanta.rosjay.util.deserchain.DeserializationChain;
import xyz.phanta.rosjay.util.deserchain.DeserializationGenerator;
import xyz.phanta.rosjay.util.deserchain.DeserializationLink;

import javax.annotation.Nullable;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class FieldTypeString implements RosDataFieldType<String> {

    public static final FieldTypeString TYPE = new FieldTypeString();

    private FieldTypeString() {
        // NO-OP
    }

    @Override
    public String getDefaultValue() {
        return "";
    }

    @Override
    public void serializeField(String value, DataOutput dest, int seqIndex) throws IOException {
        byte[] data = value.getBytes(StandardCharsets.US_ASCII);
        dest.writeInt(data.length);
        dest.write(data);
    }

    @Nullable
    @Override
    public DeserializationChain linkDeserializer(@Nullable DeserializationChain next, Consumer<String> callback) {
        return new DeserializationGenerator(next, 4, (buf, length, subNext) ->
                new DeserializationLink(subNext, buf.getInt(), (subBuf, subLength) -> {
                    byte[] strBytes = new byte[subLength];
                    subBuf.get(strBytes);
                    callback.accept(new String(strBytes, StandardCharsets.US_ASCII));
                }));
    }

    @Override
    public String toString() {
        return "string";
    }

}
