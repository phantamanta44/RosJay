package xyz.phanta.rosjay.transport.spec;

import xyz.phanta.rosjay.transport.data.field.RosDataField;
import xyz.phanta.rosjay.util.RosDataSourceFile;
import xyz.phanta.rosjay.util.id.RosId;
import xyz.phanta.rosjay.util.RosUtils;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class DataTypeSpecification {

    private final Source source;
    private final List<RosDataField<?>> dataFields;

    public DataTypeSpecification(Source source, List<RosDataField<?>> dataFields) {
        this.source = source;
        this.dataFields = Collections.unmodifiableList(dataFields);
    }

    public Source getSource() {
        return source;
    }

    public List<RosDataField<?>> getDataFields() {
        return dataFields;
    }

    public static class Source {

        private final RosId id;
        private final Path path;
        private final List<String> originalText;
        private final RosDataSourceFile sourceText;
        private final List<Source> deps;
        private final String normalizedText, md5Sum;

        Source(RosId id, Path path, List<String> originalText, RosDataSourceFile sourceText) {
            this.id = id;
            this.path = path;
            this.originalText = Collections.unmodifiableList(originalText);
            this.sourceText = sourceText;

            TypeSpecResolver.LOGGER.trace("Collecting dependencies for {}...", id);
            this.deps = RosUtils.collectDeps(this);

            TypeSpecResolver.LOGGER.trace("Computing properties for {}...", id);
            this.normalizedText = RosUtils.computeNormalizedSource(this);
            this.md5Sum = RosUtils.computeSourceMd5(this);
            TypeSpecResolver.LOGGER.trace("Computed MD5 {} for {}.", md5Sum, id);
        }

        public RosId getId() {
            return id;
        }

        public Path getPath() {
            return path;
        }

        public List<String> getOriginalText() {
            return originalText;
        }

        public RosDataSourceFile getSourceText() {
            return sourceText;
        }

        public List<Source> getDependencies() {
            return deps;
        }

        public String getNormalizedText() {
            return normalizedText;
        }

        public String getMd5Sum() {
            return md5Sum;
        }

        @Override
        public int hashCode() {
            return path.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Source && path.equals(((Source)obj).path);
        }

        @Override
        public String toString() {
            return id.toString() + " (" + path.toString() + ")";
        }

    }

}
