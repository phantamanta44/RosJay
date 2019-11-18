package xyz.phanta.rosjay.transport.spec;

import org.slf4j.Logger;
import xyz.phanta.rosjay.transport.data.field.RosDataField;
import xyz.phanta.rosjay.util.RosDataSourceFile;
import xyz.phanta.rosjay.util.RosUtils;
import xyz.phanta.rosjay.util.id.NamespacedMap;
import xyz.phanta.rosjay.util.id.RosId;
import xyz.phanta.rosjay.util.id.RosNamespace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class TypeSpecResolver {

    static final Logger LOGGER = RosUtils.getGlobalInternalLogger("typespec");

    private static final NamespacedMap<DataTypeSpecification> msgCache = new NamespacedMap<>();
    private static final NamespacedMap<ServiceSpec> srvCache = new NamespacedMap<>();

    public static DataTypeSpecification getMessageSpec(RosNamespace ns, String name) {
        DataTypeSpecification spec = msgCache.resolve(ns, name);
        if (spec == null) {
            DataTypeSpecification.Source source = resolveSpecSource(ns, name, "message", "msg");
            spec = new DataTypeSpecification(source, source.getSourceText().getElements().stream()
                    .filter(e -> e instanceof RosDataSourceFile.FieldDecl)
                    .map(f -> RosDataField.resolve(ns, (RosDataSourceFile.FieldDecl)f))
                    .collect(Collectors.toList()));
            msgCache.put(source.getId(), spec);
        }
        return spec;
    }

    public static Supplier<DataTypeSpecification> getMessageSpecProvider(RosNamespace ns, String name) {
        return () -> getMessageSpec(ns, name);
    }

    public static ServiceSpec getServiceSpec(RosNamespace ns, String name) {
        ServiceSpec spec = srvCache.resolve(ns, name);
        if (spec == null) {
            DataTypeSpecification.Source source = resolveSpecSource(ns, name, "service", "srv");
            List<RosDataField<?>> dataFields = new ArrayList<>();
            DataTypeSpecification reqSpec = null;
            for (RosDataSourceFile.Element element : source.getSourceText().getElements()) {
                if (element instanceof RosDataSourceFile.SrvDivider) {
                    if (reqSpec == null) {
                        reqSpec = new DataTypeSpecification(source, dataFields);
                        dataFields = new ArrayList<>();
                    } else {
                        throw new IllegalStateException("Service specification has multiple req/res dividers!");
                    }
                } else if (element instanceof RosDataSourceFile.FieldDecl) {
                    dataFields.add(RosDataField.resolve(ns, (RosDataSourceFile.FieldDecl)element));
                }
            }
            if (reqSpec == null) {
                throw new IllegalStateException("Service specification has no req/res divider!");
            }
            spec = new ServiceSpec(reqSpec, new DataTypeSpecification(source, dataFields));
            srvCache.put(source.getId(), spec);
        }
        return spec;
    }

    public static Supplier<ServiceSpec> getServiceSpecProvider(RosNamespace ns, String name) {
        return () -> getServiceSpec(ns, name);
    }

    public static class ServiceSpec {

        private final DataTypeSpecification reqSpec, resSpec;

        ServiceSpec(DataTypeSpecification reqSpec, DataTypeSpecification resSpec) {
            this.reqSpec = reqSpec;
            this.resSpec = resSpec;
        }

        public DataTypeSpecification getRequestSpec() {
            return reqSpec;
        }

        public DataTypeSpecification getResponseSpec() {
            return resSpec;
        }

    }

    private static DataTypeSpecification.Source resolveSpecSource(RosNamespace ns, String name, String typeName, String typeId) {
        LOGGER.trace("Attempting to resolve {} spec {} in namespace {}...", typeName, name, ns);
        RosId id = ns.resolveId(name);
        Path specFile = RosUtils.findFile(getFileName(id, typeId));
        if (specFile == null) {
            id = RosId.resolveGlobal(name);
            specFile = RosUtils.findFile(getFileName(id, typeId));
            if (specFile == null) {
                throw new NoSuchElementException("Could not find " + typeName + " specification: " + name);
            }
        }

        LOGGER.trace("Parsing {} spec {} for {}...", typeName, specFile, id);
        List<String> originalText = readFile(specFile);
        RosDataSourceFile sourceText = new RosDataSourceFile(RosUtils.sanitizeSpecFile(originalText));
        return new DataTypeSpecification.Source(id, specFile, originalText, sourceText);
    }

    private static List<String> readFile(Path path) {
        try {
            return Files.readAllLines(path);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read file: " + path);
        }
    }

    private static String getFileName(RosId id, String objType) {
        return id.getNamespace().toUnrootedString() + "/" + objType + "/" + id.getName() + "." + objType;
    }

}
