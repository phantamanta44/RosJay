package xyz.phanta.rosjay.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.phanta.jxmlrpc.data.XmlRpcArray;
import xyz.phanta.jxmlrpc.data.XmlRpcData;
import xyz.phanta.jxmlrpc.data.XmlRpcInt;
import xyz.phanta.jxmlrpc.data.XmlRpcString;
import xyz.phanta.rosjay.transport.data.RosData;
import xyz.phanta.rosjay.transport.data.field.RosDataFieldTypeManager;
import xyz.phanta.rosjay.transport.spec.DataTypeSpecification;
import xyz.phanta.rosjay.transport.spec.TypeSpecResolver;
import xyz.phanta.rosjay.util.lowdata.LEDataOutputStream;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

public class RosUtils {

    // logging

    private static final Logger GLOBAL_LOGGER = LoggerFactory.getLogger("ros");

    public static Logger getGlobalInternalLogger(String path) {
        return LoggerFactory.getLogger(GLOBAL_LOGGER.getName() + "." + path);
    }

    // ros package data

    private static final List<Path> rosPackagePaths = Arrays.stream(System.getenv("ROS_PACKAGE_PATH").split(File.pathSeparator))
            .map(Paths::get)
            .collect(Collectors.toList());

    @Nullable
    public static Path findFile(String fileName) {
        GLOBAL_LOGGER.trace("Attempting to resolve file {}...", fileName);
        return rosPackagePaths.stream()
                .map(path -> path.resolve(fileName))
                .filter(Files::exists)
                .findFirst().orElse(null);
    }

    private static final Set<DataTypeSpecification.Source> collectingSources = new HashSet<>();

    public static List<DataTypeSpecification.Source> collectDeps(DataTypeSpecification.Source root) {
        if (collectingSources.contains(root)) {
            throw new IllegalStateException("Cyclic dependency detected in data type specification: " + root.getId());
        }
        collectingSources.add(root);
        try {
            List<DataTypeSpecification.Source> deps = new ArrayList<>();
            Set<DataTypeSpecification.Source> seen = new HashSet<>();
            for (RosDataSourceFile.Element elem : root.getSourceText().getElements()) {
                String typeName;
                if (elem instanceof RosDataSourceFile.FieldDecl) {
                    typeName = ((RosDataSourceFile.FieldDecl)elem).typeName;
                } else if (elem instanceof RosDataSourceFile.ConstDecl) {
                    typeName = ((RosDataSourceFile.ConstDecl)elem).typeName;
                } else {
                    continue;
                }
                typeName = stripArrayType(typeName);
                if (!RosDataFieldTypeManager.isPrimitiveType(typeName)) {
                    if (typeName.equals("Header")) {
                        typeName = "std_msgs/Header";
                    }
                    DataTypeSpecification.Source depSrc = TypeSpecResolver
                            .getMessageSpec(root.getId().getNamespace(), typeName).getSource();
                    if (!seen.contains(depSrc)) {
                        deps.add(depSrc);
                        seen.add(depSrc);
                    }
                    for (DataTypeSpecification.Source subDep : depSrc.getDependencies()) {
                        if (!seen.contains(subDep)) {
                            deps.add(subDep);
                            seen.add(subDep);
                        }
                    }
                }
            }
            return deps;
        } finally {
            collectingSources.remove(root);
        }
    }

    private static final String SRC_CONCAT_DELIM
            = "================================================================================\nMSG: ";

    public static String computeNormalizedSource(DataTypeSpecification.Source root) {
        StringBuilder sb = new StringBuilder(String.join("\n", root.getOriginalText()));
        for (DataTypeSpecification.Source dep : root.getDependencies()) {
            sb.append(SRC_CONCAT_DELIM).append(dep.getId()).append("\n").append(String.join("\n", dep.getOriginalText()));
        }
        return sb.toString();
    }

    public static String computeSourceMd5(DataTypeSpecification.Source root) {
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to acquire MD5 computer!");
        }
        boolean first = true;
        for (RosDataSourceFile.Element element : root.getSourceText().getElements()) {
            if (element instanceof RosDataSourceFile.SrvDivider) {
                first = true;
                continue;
            } else if (first) {
                first = false;
            } else {
                md5.update((byte)'\n');
            }
            String typeName, fieldName, suffix = null;
            if (element instanceof RosDataSourceFile.FieldDecl) {
                RosDataSourceFile.FieldDecl fieldDecl = (RosDataSourceFile.FieldDecl)element;
                typeName = fieldDecl.typeName;
                fieldName = fieldDecl.name;
            } else { // must be constant declaration
                RosDataSourceFile.ConstDecl constDecl = (RosDataSourceFile.ConstDecl)element;
                typeName = constDecl.typeName;
                fieldName = constDecl.name;
                suffix = "=" + constDecl.value;
            }
            String baseTypeName = stripArrayType(typeName);
            if (RosDataFieldTypeManager.isPrimitiveType(baseTypeName)) {
                md5.update(typeName.getBytes(StandardCharsets.US_ASCII));
            } else {
                if (typeName.equals("Header")) {
                    typeName = "std_msgs/Header";
                }
                md5.update(TypeSpecResolver.getMessageSpec(root.getId().getNamespace(), typeName)
                        .getSource().getMd5Sum().getBytes(StandardCharsets.US_ASCII));
            }
            md5.update((byte)' ');
            md5.update(fieldName.getBytes(StandardCharsets.US_ASCII));
            if (suffix != null) {
                md5.update(suffix.getBytes(StandardCharsets.US_ASCII));
            }
        }
        return encodeHex(md5.digest());
    }

    private static String stripArrayType(String typeName) {
        int bracketIndex = typeName.indexOf('[');
        return bracketIndex == -1 ? typeName : typeName.substring(0, bracketIndex).trim();
    }

    public static String stripComment(String line) {
        line = line.trim();
        if (line.startsWith("string ") && line.indexOf('=') != -1) {
            return line;
        }
        int commentIndex = line.indexOf('#');
        if (commentIndex != -1) {
            line = line.substring(0, commentIndex);
        }
        return line.trim();
    }

    public static List<String> sanitizeSpecFile(List<String> lines) {
        return lines.stream()
                .map(RosUtils::stripComment)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
    }

    // xmlrpc tools

    @SuppressWarnings("unchecked")
    public static <T extends XmlRpcData> T unwrapRpcResult(XmlRpcData response) {
        return (T)((XmlRpcArray<?>)response).get(2);
    }

    public static XmlRpcArray<?> buildRpcResult(XmlRpcData result) {
        return buildRpcResult(1, "", result);
    }

    public static XmlRpcArray<?> buildRpcResult(int code, String statusMessage, XmlRpcData result) {
        return XmlRpcArray.of(new XmlRpcInt(code), new XmlRpcString(statusMessage), result);
    }

    // other utils

    public static URI buildAddressUri(String ip, int port) {
        return URI.create("http://" + ip + ":" + port);
    }

    public static String pascalToSnake(String pascalString) {
        if (pascalString.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder().append(Character.toLowerCase(pascalString.charAt(0)));
        for (int i = 1; i < pascalString.length(); i++) {
            char c = pascalString.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append('_').append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    public static String encodeHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(HEX_DIGITS[(b >>> 4) & 0x0F]).append(HEX_DIGITS[(b & 0x0F)]);
        }
        return sb.toString();
    }

    public static byte[] serializeDataPacket(RosData<?> data, int seqIndex) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        data.serializeData(new LEDataOutputStream(buf), seqIndex);
        return buf.toByteArray();
    }

}
