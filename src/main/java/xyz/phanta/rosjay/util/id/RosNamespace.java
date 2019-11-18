package xyz.phanta.rosjay.util.id;

import javax.annotation.Nullable;
import java.util.NoSuchElementException;
import java.util.Objects;

public class RosNamespace {

    public static final RosNamespace ROOT = new RosNamespace();

    public static RosNamespace resolveGlobal(String path) {
        return ROOT.resolveNamespace(path);
    }

    @Nullable
    private final RosNamespace parent;
    private final String pathNode;

    private RosNamespace() {
        this.parent = null;
        this.pathNode = "";
    }

    private RosNamespace(RosNamespace parent, String pathNode) {
        this.parent = parent;
        this.pathNode = pathNode;
    }

    public RosNamespace getParent() {
        if (parent == null) {
            throw new NoSuchElementException("Already at root namespace!");
        }
        return parent;
    }

    public RosNamespace resolveNamespace(String path) {
        RosNamespace ns = this;
        if (path.startsWith("/")) {
            ns = ROOT;
            path = path.substring(1);
        }
        for (String pathNode : path.split("/")) {
            if (!pathNode.isEmpty()) {
                ns = new RosNamespace(ns, pathNode);
            }
        }
        return ns;
    }

    public RosId resolveId(String name) {
        if (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1);
        }
        int nameIndex = name.lastIndexOf('/');
        return nameIndex == -1 ? new RosId(this, name)
                : new RosId(resolveNamespace(name.substring(0, nameIndex)), name.substring(nameIndex + 1));
    }

    @Override
    public int hashCode() {
        return parent == null ? super.hashCode() : (parent.hashCode() ^ pathNode.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RosNamespace)) {
            return false;
        }
        RosNamespace o = (RosNamespace)obj;
        return pathNode.equals(o.pathNode) && Objects.equals(parent, o.parent);
    }

    public void concatString(StringBuilder accum) {
        if (parent != null) {
            parent.concatString(accum);
            accum.append("/").append(pathNode);
        }
    }

    @Override
    public String toString() {
        if (parent == null) {
            return "/";
        }
        StringBuilder sb = new StringBuilder();
        concatString(sb);
        return sb.toString();
    }

    public String toUnrootedString() {
        return toString().substring(1);
    }

}
