package xyz.phanta.rosjay.tcpros;

import xyz.phanta.rosjay.util.lowdata.ByteChunkArray;
import xyz.phanta.rosjay.util.lowdata.LEDataOutputStream;

import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class TcpRosHeader {

    private static final byte[] BYTE_ARR_EQUALS = { (byte)'=' };

    private final Map<String, String> fields = new HashMap<>();

    public void putField(String name, String value) {
        fields.put(name, value);
    }

    public void clear() {
        fields.clear();
    }

    public void write(DataOutput dest) throws IOException {
        ByteChunkArray[] fieldBcaArr = new ByteChunkArray[fields.size()];
        int totalSize = 0;
        int i = 0;
        for (Map.Entry<String, String> field : fields.entrySet()) {
            ByteChunkArray fieldBca = writeField(field.getKey(), field.getValue());
            totalSize += fieldBca.getLength() + 4;
            fieldBcaArr[i++] = fieldBca;
        }
        dest.writeInt(totalSize);
        for (ByteChunkArray bca : fieldBcaArr) {
            dest.writeInt(bca.getLength());
            bca.write(dest);
        }
    }

    public void writeQuietly(OutputStream dest) {
        try {
            write(new LEDataOutputStream(dest));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write TCPROS header!", e);
        }
    }

    private static ByteChunkArray writeField(String name, String value) {
        return new ByteChunkArray(
                name.getBytes(StandardCharsets.US_ASCII), BYTE_ARR_EQUALS, value.getBytes(StandardCharsets.US_ASCII));
    }

}
