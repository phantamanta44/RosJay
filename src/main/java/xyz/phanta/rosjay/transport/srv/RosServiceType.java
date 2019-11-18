package xyz.phanta.rosjay.transport.srv;

import org.slf4j.Logger;
import xyz.phanta.rosjay.transport.data.RosData;
import xyz.phanta.rosjay.transport.data.RosDataType;
import xyz.phanta.rosjay.transport.spec.TypeSpecResolver;
import xyz.phanta.rosjay.util.RosUtils;
import xyz.phanta.rosjay.util.id.NamespacedMap;
import xyz.phanta.rosjay.util.id.RosId;
import xyz.phanta.rosjay.util.id.RosNamespace;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class RosServiceType<REQ extends RosData<REQ>, RES extends RosData<RES>> {

    private static final Logger LOGGER = RosUtils.getGlobalInternalLogger("srvtype");

    private static final NamespacedMap<RosServiceType<?, ?>> srvTypeCache = new NamespacedMap<>();

    @SuppressWarnings("unchecked")
    @Nullable
    public static <REQ extends RosData<REQ>, RES extends RosData<RES>> RosServiceType<REQ, RES> get(RosId id) {
        return (RosServiceType<REQ, RES>)srvTypeCache.get(id);
    }

    public static <REQ extends RosData<REQ>, RES extends RosData<RES>> RosServiceType<REQ, RES> resolve(RosNamespace ns,
                                                                                                        String name,
                                                                                                        Class<REQ> reqClass,
                                                                                                        Class<RES> resClass) {
        //noinspection unchecked
        RosServiceType<REQ, RES> srvType = (RosServiceType<REQ, RES>)srvTypeCache.resolve(ns, name);
        if (srvType == null) {
            srvType = loadType(ns, name, reqClass, resClass);
            srvTypeCache.put(srvType.getId(), srvType);
        }
        return srvType;
    }

    private static <REQ extends RosData<REQ>, RES extends RosData<RES>> RosServiceType<REQ, RES> loadType(RosNamespace ns,
                                                                                                          String name,
                                                                                                          Class<REQ> reqClass,
                                                                                                          Class<RES> resClass) {
        LOGGER.debug("Resolving service type {} ({} / {}) in namespace {}...",
                name, reqClass.getCanonicalName(), resClass.getCanonicalName(), ns);
        Supplier<TypeSpecResolver.ServiceSpec> specProvider = TypeSpecResolver.getServiceSpecProvider(ns, name);
        RosDataType<REQ> reqType = RosDataType.resolve(reqClass, () -> specProvider.get().getRequestSpec());
        RosDataType<RES> resType = RosDataType.resolve(resClass, () -> specProvider.get().getResponseSpec());
        return new RosServiceType<>(reqType.getTypeSpecification().getSource().getId(), reqType, resType);
    }

    private final RosId id;
    private final RosDataType<REQ> reqDataType;
    private final RosDataType<RES> resDataType;

    private RosServiceType(RosId id, RosDataType<REQ> reqDataType, RosDataType<RES> resDataType) {
        this.id = id;
        this.reqDataType = reqDataType;
        this.resDataType = resDataType;
    }

    public RosId getId() {
        return id;
    }

    public RosDataType<REQ> getRequestType() {
        return reqDataType;
    }

    public REQ newRequest() {
        return reqDataType.newInstance();
    }

    public RosDataType<RES> getResponseType() {
        return resDataType;
    }

    public RES newResponse() {
        return resDataType.newInstance();
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RosServiceType && id.equals(((RosServiceType)obj).id);
    }

    @Override
    public String toString() {
        return id.toString();
    }

}
