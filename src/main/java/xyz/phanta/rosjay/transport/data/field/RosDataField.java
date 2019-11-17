package xyz.phanta.rosjay.transport.data.field;

import xyz.phanta.rosjay.transport.data.field.impl.FieldTypeArrayFixed;
import xyz.phanta.rosjay.transport.data.field.impl.FieldTypeArrayVariable;
import xyz.phanta.rosjay.util.RosDataSourceFile;
import xyz.phanta.rosjay.util.id.RosNamespace;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RosDataField<T> {

    private static final Pattern DATA_TYPE_PATTERN = Pattern.compile("([A-Za-z\\d]+(?:/[\\w\\d]+)*)(\\[\\d*])?");

    public static RosDataField<?> resolve(RosNamespace ns, RosDataSourceFile.FieldDecl fieldDecl) {
        Matcher m = DATA_TYPE_PATTERN.matcher(fieldDecl.typeName);
        if (m.matches()) {
            RosDataFieldType<?> fieldType = RosDataFieldTypeManager.getType(ns, m.group(1));
            String arrDef = m.group(2);
            if (arrDef != null) {
                arrDef = arrDef.substring(1, arrDef.length() - 1);
                fieldType = arrDef.isEmpty() ? new FieldTypeArrayVariable<>(fieldType)
                        : new FieldTypeArrayFixed<>(fieldType, Integer.parseInt(arrDef));
            }
            return new RosDataField<>(fieldDecl.name, fieldType);
        }
        throw new IllegalArgumentException("Malformed data field declaration: " + fieldDecl);
    }

    private final String name;
    private final RosDataFieldType<T> type;

    public RosDataField(String name, RosDataFieldType<T> type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public RosDataFieldType<T> getType() {
        return type;
    }

    @Override
    public String toString() {
        return type.toString() + " " + name;
    }

}
