package xyz.phanta.rosjay.transport.msg;

import org.slf4j.Logger;
import xyz.phanta.rosjay.rospkg.std_msgs.Header;
import xyz.phanta.rosjay.transport.data.RosData;
import xyz.phanta.rosjay.transport.data.RosDataType;
import xyz.phanta.rosjay.transport.data.field.RosDataFieldTypeManager;
import xyz.phanta.rosjay.transport.spec.TypeSpecResolver;
import xyz.phanta.rosjay.util.RosUtils;
import xyz.phanta.rosjay.util.id.NamespacedMap;
import xyz.phanta.rosjay.util.id.RosId;
import xyz.phanta.rosjay.util.id.RosNamespace;

import javax.annotation.Nullable;

public class RosMessageType<T extends RosData<T>> {

    private static final Logger LOGGER = RosUtils.getGlobalInternalLogger("msgtype");

    private static final NamespacedMap<RosMessageType<?>> msgTypeCache = new NamespacedMap<>();

    @SuppressWarnings("unchecked")
    @Nullable
    public static <T extends RosData<T>> RosMessageType<T> get(RosId id) {
        return (RosMessageType<T>)msgTypeCache.get(id);
    }

    public static <T extends RosData<T>> RosMessageType<T> resolve(RosNamespace ns, String name, Class<T> dataClass) {
        //noinspection unchecked
        RosMessageType<T> msgType = (RosMessageType<T>)msgTypeCache.resolve(ns, name);
        if (msgType == null) {
            msgType = loadType(ns, name, dataClass);
            msgTypeCache.put(msgType.getId(), msgType);
        }
        return msgType;
    }

    private static <T extends RosData<T>> RosMessageType<T> loadType(RosNamespace ns, String name, Class<T> dataClass) {
        LOGGER.debug("Resolving message type {} ({}) in namespace {}...", name, dataClass.getCanonicalName(), ns);
        RosDataType<T> dataType = RosDataType.resolve(dataClass, TypeSpecResolver.getMessageSpecProvider(ns, name));
        RosMessageType<T> msgType = new RosMessageType<>(dataType.getTypeSpecification().getSource().getId(), dataType);
        if (dataClass != Header.class) {
            RosDataFieldTypeManager.registerMessageType(msgType);
        }
        return msgType;
    }

    private final RosId id;
    private final RosDataType<T> dataType;

    private RosMessageType(RosId id, RosDataType<T> dataType) {
        this.id = id;
        this.dataType = dataType;
    }

    public RosId getId() {
        return id;
    }

    public RosDataType<T> getDataType() {
        return dataType;
    }

    public T newInstance() {
        return dataType.newInstance();
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RosMessageType && id.equals(((RosMessageType)obj).id);
    }

    @Override
    public String toString() {
        return id.toString();
    }

}
