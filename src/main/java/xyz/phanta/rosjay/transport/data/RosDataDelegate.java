package xyz.phanta.rosjay.transport.data;

import xyz.phanta.rosjay.transport.data.field.RosDataField;
import xyz.phanta.rosjay.transport.data.field.impl.FieldTypeHeader;
import xyz.phanta.rosjay.rospkg.std_msgs.Header;
import xyz.phanta.rosjay.util.deserchain.DeserializationChain;

import javax.annotation.Nullable;
import java.io.DataOutput;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

class RosDataDelegate<T extends RosData<T>> implements InvocationHandler {

    private static final Map<Method, MethodProxy> METHOD_PROXIES = new HashMap<>();

    private final RosDataDelegateFactory<T> factory;
    private final Map<String, Object> properties = new HashMap<>();

    RosDataDelegate(RosDataDelegateFactory<T> factory) {
        this.factory = factory;
        for (RosDataField prop : factory.getProperties()) {
            properties.put(prop.getName(), prop.getType().getDefaultValue());
        }
    }

    @Nullable
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        MethodProxy methodProxy = METHOD_PROXIES.get(method);
        if (methodProxy != null) {
            return methodProxy.invoke(this, args);
        }
        RosDataDelegateFactory.DataDelegateAccessor accessor = factory.lookUpDelegateAccessor(method);
        if (accessor == null) {
            throw new UnsupportedOperationException("Unsupported method invocation: " + method.getName());
        } else if (accessor instanceof RosDataDelegateFactory.DataDelegateSetter) {
            ((RosDataDelegateFactory.DataDelegateSetter)accessor).setValue(properties, args[0]);
            return null;
        } else { // must be getter
            return ((RosDataDelegateFactory.DataDelegateGetter)accessor).getValue(properties);
        }
    }

    static {
        try {
            METHOD_PROXIES.put(RosData.class.getDeclaredMethod("getDataType"),
                    (instance, args) -> instance.factory.getDataType());
            METHOD_PROXIES.put(RosData.class.getDeclaredMethod("serializeData", DataOutput.class, Integer.TYPE),
                    (instance, args) -> {
                        DataOutput dest = (DataOutput)args[0];
                        for (int i = 0; i < instance.factory.getProperties().size(); i++) {
                            RosDataField prop = instance.factory.getProperty(i);
                            if (prop.getType() instanceof FieldTypeHeader && i == 0) {
                                Header header = (Header)instance.properties.get(prop.getName());
                                header.setSeq((int)args[1]);
                                header.setStamp(Instant.now());
                                FieldTypeHeader.TYPE.serializeField(header, dest, 0);
                            } else {
                                //noinspection unchecked
                                prop.getType().serializeField(instance.properties.get(prop.getName()), dest, 0);
                            }
                        }
                        return null;
                    });
            METHOD_PROXIES.put(RosData.class.getDeclaredMethod("linkDeserializer", DeserializationChain.class),
                    (instance, args) -> {
                        DeserializationChain deser = (DeserializationChain)args[0];
                        for (int i = instance.factory.getProperties().size() - 1; i >= 0; i--) {
                            RosDataField<?> prop = instance.factory.getProperty(i);
                            deser = prop.getType()
                                    .linkDeserializer(deser, value -> instance.properties.put(prop.getName(), value));
                        }
                        return deser;
                    });
            METHOD_PROXIES.put(Object.class.getDeclaredMethod("hashCode"),
                    (instance, args) -> instance.properties.hashCode());
            METHOD_PROXIES.put(Object.class.getDeclaredMethod("equals", Object.class),
                    (instance, args) -> {
                        Object o = args[0];
                        if (!instance.factory.getDataClass().isInstance(o)) {
                            return false;
                        }
                        for (Map.Entry<Method, RosDataDelegateFactory.DataDelegateAccessor> propEntry
                                : instance.factory.getDelegateMap().entrySet()) {
                            RosDataDelegateFactory.DataDelegateAccessor accessor = propEntry.getValue();
                            try {
                                if (accessor instanceof RosDataDelegateFactory.DataDelegateGetter
                                        && !((RosDataDelegateFactory.DataDelegateGetter)accessor).getValue(instance.properties)
                                        .equals(propEntry.getKey().invoke(o))) {
                                    return false;
                                }
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                return false;
                            }
                        }
                        return true;
                    });
            METHOD_PROXIES.put(Object.class.getDeclaredMethod("toString"),
                    (instance, args) -> instance.factory.getDataClass().getSimpleName() + " " + instance.properties);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Failed to initialize ROS data delegate reflections!", e);
        }
    }

    @FunctionalInterface
    private interface MethodProxy {

        @Nullable
        Object invoke(RosDataDelegate<?> instance, Object[] args) throws Throwable;

    }

}
