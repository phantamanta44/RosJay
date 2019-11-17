package xyz.phanta.rosjay.transport.spec;

import org.slf4j.Logger;
import xyz.phanta.rosjay.transport.data.field.RosDataField;
import xyz.phanta.rosjay.util.*;
import xyz.phanta.rosjay.util.id.NamespacedMap;
import xyz.phanta.rosjay.util.id.RosId;
import xyz.phanta.rosjay.util.id.RosNamespace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class TypeSpecResolver {

    static final Logger LOGGER = RosUtils.getGlobalInternalLogger("typespec");

    private static final NamespacedMap<DataTypeSpecification> msgCache = new NamespacedMap<>();
    private static final NamespacedMap<DataTypeSpecification> srvReqCache = new NamespacedMap<>();
    private static final NamespacedMap<DataTypeSpecification> srvResCache = new NamespacedMap<>();

    public static DataTypeSpecification getMessageSpec(RosNamespace ns, String name) {
        DataTypeSpecification spec = msgCache.resolve(ns, name);
        if (spec == null) {
            LOGGER.trace("Attempting to resolve message spec {} in namespace {}...", name, ns);
            RosId id = ns.resolveId(name);
            Path msgFile = RosUtils.findFile(getFileName(id, "msg"));
            if (msgFile == null) {
                id = RosNamespace.ROOT.resolveId(name);
                msgFile = RosUtils.findFile(getFileName(id, "msg"));
                if (msgFile == null) {
                    throw new NoSuchElementException("Could not find message specification: " + name);
                }
            }

            LOGGER.trace("Parsing message spec {} for {}...", msgFile, id);
            List<String> originalText = readFile(msgFile);
            RosDataSourceFile sourceText = new RosDataSourceFile(RosUtils.sanitizeSpecFile(originalText));
            spec = new DataTypeSpecification(new DataTypeSpecification.Source(id, msgFile, originalText, sourceText),
                    sourceText.getElements().stream()
                            .filter(e -> e instanceof RosDataSourceFile.FieldDecl)
                            .map(f -> RosDataField.resolve(ns, (RosDataSourceFile.FieldDecl)f))
                            .collect(Collectors.toList()));
            msgCache.put(id, spec);
        }
        return spec;
    }

    public static Supplier<DataTypeSpecification> getMessageSpecProvider(RosNamespace ns, String name) {
        return () -> getMessageSpec(ns, name);
    }

    // TODO service req/res spec resolvers

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
