package xyz.phanta.rosjay.transport.data;

import xyz.phanta.rosjay.transport.data.field.RosDataField;
import xyz.phanta.rosjay.util.RosUtils;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

class RosDataDelegateFactory<T extends RosData<T>> implements Supplier<T> {

    private final RosDataType<T> dataType;
    private final Class<T>[] dataClass;
    private final List<RosDataField<?>> propertyList;
    private final Map<Method, DataDelegateAccessor> propertyMap = new HashMap<>();

    RosDataDelegateFactory(RosDataType<T> dataType, List<RosDataField<?>> propertyList) {
        this.dataType = dataType;
        //noinspection unchecked
        this.dataClass = new Class[] { dataType.getDataClass() };
        this.propertyList = propertyList;
        for (Method method : dataClass[0].getDeclaredMethods()) {
            if (method.getName().startsWith("get")) {
                propertyMap.put(method,
                        new DataDelegateGetter(RosUtils.pascalToSnake(method.getName().substring(3))));
            } else if (method.getName().startsWith("set")) {
                propertyMap.put(method,
                        new DataDelegateSetter(RosUtils.pascalToSnake(method.getName().substring(3))));
            }
        }
    }

    RosDataType<T> getDataType() {
        return dataType;
    }

    Class<T> getDataClass() {
        return dataClass[0];
    }

    List<RosDataField<?>> getProperties() {
        return propertyList;
    }

    RosDataField<?> getProperty(int index) {
        return propertyList.get(index);
    }

    Map<Method, DataDelegateAccessor> getDelegateMap() {
        return propertyMap;
    }

    @Nullable
    DataDelegateAccessor lookUpDelegateAccessor(Method method) {
        return propertyMap.get(method);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get() {
        return (T)Proxy.newProxyInstance(RosDataDelegateFactory.class.getClassLoader(), dataClass, new RosDataDelegate<>(this));
    }

    static abstract class DataDelegateAccessor {

        final String propName;

        DataDelegateAccessor(String propName) {
            this.propName = propName;
        }

    }

    static class DataDelegateGetter extends DataDelegateAccessor {

        DataDelegateGetter(String propName) {
            super(propName);
        }

        Object getValue(Map<String, Object> delegateProps) {
            return delegateProps.getOrDefault(propName, null);
        }

    }

    static class DataDelegateSetter extends DataDelegateAccessor {

        DataDelegateSetter(String propName) {
            super(propName);
        }

        void setValue(Map<String, Object> delegateProps, Object value) {
            delegateProps.put(propName, value);
        }

    }

}
