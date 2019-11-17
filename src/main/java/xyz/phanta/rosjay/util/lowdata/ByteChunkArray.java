package xyz.phanta.rosjay.util.lowdata;

import java.io.DataOutput;
import java.io.IOException;

public class ByteChunkArray {

    private final byte[][] chunks;
    private final int length;

    public ByteChunkArray(byte[]... chunks) {
        this.chunks = chunks;
        int lengthAcc = 0;
        for (byte[] chunk : chunks) {
            lengthAcc += chunk.length;
        }
        this.length = lengthAcc;
    }

    public ByteChunkArray(ByteChunkArray... cons) {
        int chunkCount = 0;
        int lengthAcc = 0;
        for (ByteChunkArray bca : cons) {
            chunkCount += bca.chunks.length;
            lengthAcc += bca.length;
        }
        this.chunks = new byte[chunkCount][];
        this.length = lengthAcc;
        chunkCount = 0;
        for (ByteChunkArray bca : cons) {
            for (byte[] chunk : bca.chunks) {
                chunks[chunkCount++] = chunk;
            }
        }
    }

    public int getLength() {
        return length;
    }

    public void write(DataOutput dest) throws IOException {
        for (byte[] chunk : chunks) {
            dest.write(chunk);
        }
    }

}
