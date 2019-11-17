package xyz.phanta.rosjay.transport.data;

import xyz.phanta.rosjay.transport.spec.DataTypeSpecification;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class RosDataType<T extends RosData<T>> {

    private static final Map<Class, RosDataType> dataTypeCache = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static <T extends RosData<T>> RosDataType<T> resolve(Class<T> dataClass,
                                                                Supplier<DataTypeSpecification> typeSpecProvider) {
        return dataTypeCache.computeIfAbsent(dataClass, k -> new RosDataType(dataClass, typeSpecProvider.get()));
    }

    private final Class<T> dataClass;
    private final DataTypeSpecification typeSpec;
    private final Supplier<T> factory;

    private RosDataType(Class<T> dataClass, DataTypeSpecification typeSpec) {
        this.dataClass = dataClass;
        this.typeSpec = typeSpec;
        this.factory = new RosDataDelegateFactory<>(this, typeSpec.getDataFields());
    }

    public Class<T> getDataClass() {
        return dataClass;
    }

    public DataTypeSpecification getTypeSpecification() {
        return typeSpec;
    }

    public T newInstance() {
        return factory.get();
    }

}
