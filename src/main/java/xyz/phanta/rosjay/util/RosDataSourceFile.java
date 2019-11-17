package xyz.phanta.rosjay.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RosDataSourceFile {

    private static final Pattern FIELD_CONST_PATTERN
            = Pattern.compile("([A-Za-z\\d]+(?:/[\\w\\d]+)*(?:\\[\\d*])?)\\s*(\\w+)(?:\\s*=\\s*(.+))?");

    private final List<Element> elements;

    public RosDataSourceFile(Iterable<String> lines) {
        List<Element> elemAcc = new ArrayList<>();
        for (String line : lines) {
            if (line.equals("---")) {
                elemAcc.add(new SrvDivider());
            } else {
                Matcher m = FIELD_CONST_PATTERN.matcher(line);
                if (m.matches()) {
                    String constValue = m.group(3);
                    if (constValue != null) {
                        elemAcc.add(new ConstDecl(m.group(1), m.group(2), constValue));
                    } else {
                        elemAcc.add(new FieldDecl(m.group(1), m.group(2)));
                    }
                }
            }
        }
        this.elements = Collections.unmodifiableList(elemAcc);
    }

    public List<Element> getElements() {
        return elements;
    }

    @Override
    public String toString() {
        return elements.stream().map(Object::toString).collect(Collectors.joining("\n"));
    }

    public interface Element {
        // NO-OP
    }

    public static class FieldDecl implements Element {

        public final String typeName, name;

        FieldDecl(String typeName, String name) {
            this.typeName = typeName;
            this.name = name;
        }

        @Override
        public String toString() {
            return typeName + " " + name;
        }

    }

    public static class ConstDecl implements Element {

        public final String typeName, name, value;

        public ConstDecl(String typeName, String name, String value) {
            this.typeName = typeName;
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return typeName + " " + name + "=" + value;
        }

    }

    public static class SrvDivider implements Element {

        @Override
        public String toString() {
            return "---";
        }

    }

}
