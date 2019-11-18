package xyz.phanta.rosjay.node;

import org.slf4j.Logger;
import xyz.phanta.jxmlrpc.data.XmlRpcData;
import xyz.phanta.rosjay.util.id.NamespacedMap;
import xyz.phanta.rosjay.util.id.RosId;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ParameterManager {

    private final RosNode owner;
    private final Logger internalLogger;
    private final NamespacedMap<Set<Consumer<? extends XmlRpcData>>> paramCallbacks = NamespacedMap.concurrent();

    ParameterManager(RosNode owner) {
        this.owner = owner;
        this.internalLogger = owner.getChildInternalLogger("param");
    }

    @Nullable
    public <T extends XmlRpcData> T get(RosId paramId) {
        try {
            internalLogger.debug("Retrieving parameter {}...", paramId);
            //noinspection unchecked
            return (T)owner.getRosMaster().getParam(paramId);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to retrieve parameter!", e);
        }
    }

    public <T extends XmlRpcData> T get(RosId paramId, Supplier<T> defaultFactory) {
        T paramValue = get(paramId);
        return paramValue != null ? paramValue : defaultFactory.get();
    }

    @Nullable
    public RosId resolveKey(String paramName) {
        try {
            internalLogger.debug("Looking up parameter key for {}...", paramName);
            return owner.getRosMaster().searchParam(paramName);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to look up parameter key!", e);
        }
    }

    @Nullable
    public <T extends XmlRpcData> T resolve(String paramName) {
        RosId paramKey = resolveKey(paramName);
        return paramKey == null ? null : get(paramKey);
    }

    public <T extends XmlRpcData> T resolve(String paramName, Supplier<T> defaultFactory) {
        T paramValue = resolve(paramName);
        return paramValue != null ? paramValue : defaultFactory.get();
    }

    public <T extends XmlRpcData> void addCallback(RosId paramKey, Consumer<T> callback) {
        paramCallbacks.computeIfAbsent(paramKey, () -> {
            try {
                internalLogger.debug("Registering param subscription to {}...", paramKey);
                //noinspection unchecked
                callback.accept((T)owner.getRosMaster().subscribeParam(paramKey));
            } catch (IOException e) {
                internalLogger.warn("Encountered error while registering parameter subscription!", e);
            }
            return new HashSet<>();
        }).add(callback);
    }

    public void removeCallback(RosId paramKey, Consumer<? extends XmlRpcData> callback) {
        Set<Consumer<? extends XmlRpcData>> callbacks = paramCallbacks.get(paramKey);
        if (callbacks != null) {
            callbacks.remove(callback);
            if (callbacks.isEmpty()) {
                paramCallbacks.remove(paramKey);
                try {
                    internalLogger.debug("Unregistering param subscription to {}...", paramKey);
                    owner.getRosMaster().unsubscribeParam(paramKey);
                } catch (IOException e) {
                    internalLogger.warn("Encountered error while unregistering parameter subscription!", e);
                }
            }
        }
    }

    void notifyParamUpdate(RosId paramKey, XmlRpcData value) {
        Set<Consumer<? extends XmlRpcData>> callbacks = paramCallbacks.get(paramKey);
        if (callbacks != null) {
            for (Consumer callback : callbacks) {
                //noinspection unchecked
                callback.accept(value);
            }
        }
    }

    void kill() {
        for (Map.Entry<RosId, Set<Consumer<? extends XmlRpcData>>> paramSub : paramCallbacks.entrySet()) {
            try {
                internalLogger.debug("Closing subscription {}...", paramSub.getKey());
                owner.getRosMaster().unsubscribeParam(paramSub.getKey());
            } catch (Exception e) {
                internalLogger.warn("Failed to clean up parameter subscription!", e);
            }
        }
    }

}
