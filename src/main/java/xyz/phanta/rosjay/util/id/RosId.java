package xyz.phanta.rosjay.util.id;

public class RosId {

    private final RosNamespace namespace;
    private final String name;

    RosId(RosNamespace namespace, String name) {
        this.namespace = namespace;
        this.name = name;
    }

    public RosNamespace getNamespace() {
        return namespace;
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return namespace.hashCode() ^ name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RosId)) {
            return false;
        }
        RosId o = (RosId)obj;
        return name.equals(o.name) && namespace.equals(o.namespace);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        namespace.concatString(sb);
        return sb.append("/").append(name).toString();
    }

    public String toUnrootedString() {
        return toString().substring(1);
    }

}
