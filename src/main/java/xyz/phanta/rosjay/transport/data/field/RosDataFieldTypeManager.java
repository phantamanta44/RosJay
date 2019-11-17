package xyz.phanta.rosjay.transport.data.field;

import xyz.phanta.rosjay.transport.data.RosData;
import xyz.phanta.rosjay.transport.data.field.impl.*;
import xyz.phanta.rosjay.rospkg.std_msgs.Header;
import xyz.phanta.rosjay.transport.msg.RosMessageType;
import xyz.phanta.rosjay.util.id.NamespacedMap;
import xyz.phanta.rosjay.util.id.RosNamespace;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class RosDataFieldTypeManager {

    private static final Map<String, RosDataFieldType<?>> primitiveFieldTypes = new HashMap<>();
    private static final NamespacedMap<RosDataFieldType<?>> namespacedFieldTypes = new NamespacedMap<>();

    static {
        primitiveFieldTypes.put("bool", FieldTypeBool.TYPE);
        primitiveFieldTypes.put("int8", FieldTypeInt8.TYPE);
        primitiveFieldTypes.put("byte", FieldTypeInt8.TYPE);
        primitiveFieldTypes.put("uint8", FieldTypeInt8.TYPE);
        primitiveFieldTypes.put("char", FieldTypeInt8.TYPE);
        primitiveFieldTypes.put("int16", FieldTypeInt16.TYPE);
        primitiveFieldTypes.put("uint16", FieldTypeInt16.TYPE);
        primitiveFieldTypes.put("int32", FieldTypeInt32.TYPE);
        primitiveFieldTypes.put("uint32", FieldTypeInt32.TYPE);
        primitiveFieldTypes.put("int64", FieldTypeInt64.TYPE);
        primitiveFieldTypes.put("uint64", FieldTypeInt64.TYPE);
        primitiveFieldTypes.put("float32", FieldTypeFloat32.TYPE);
        primitiveFieldTypes.put("float64", FieldTypeFloat64.TYPE);
        primitiveFieldTypes.put("string", FieldTypeString.TYPE);
        primitiveFieldTypes.put("time", FieldTypeTime.TYPE);
        primitiveFieldTypes.put("duration", FieldTypeDuration.TYPE);
        namespacedFieldTypes.put(RosNamespace.ROOT.resolveId("Header"), FieldTypeHeader.TYPE);
        namespacedFieldTypes.put(Header.TYPE.getId(), FieldTypeHeader.TYPE);
    }

    public static RosDataFieldType<?> getType(RosNamespace namespace, String name) {
        RosDataFieldType<?> fieldType = primitiveFieldTypes.get(name);
        if (fieldType != null) {
            return fieldType;
        }
        fieldType = namespacedFieldTypes.resolve(namespace, name);
        if (fieldType != null) {
            return fieldType;
        }
        throw new NoSuchElementException("Could not resolve data field type: " + name);
    }

    public static boolean isPrimitiveType(String typeName) {
        return primitiveFieldTypes.containsKey(typeName);
    }

    public static <T extends RosData<T>> void registerMessageType(RosMessageType<T> msgType) {
        namespacedFieldTypes.put(msgType.getId(), new FieldTypeMessage<>(msgType));
    }

}
