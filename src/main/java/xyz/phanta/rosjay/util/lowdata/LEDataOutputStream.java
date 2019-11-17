package xyz.phanta.rosjay.util.lowdata;

import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class LEDataOutputStream extends OutputStream implements DataOutput {

    private final OutputStream backing;

    public LEDataOutputStream(OutputStream backing) {
        this.backing = backing;
    }

    @Override
    public void write(int b) throws IOException {
        backing.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        backing.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        backing.write(b, off, len);
    }

    @Override
    public void writeBoolean(boolean v) throws IOException {
        backing.write(v ? 1 : 0);
    }

    @Override
    public void writeByte(int v) throws IOException {
        backing.write(v);
    }

    @Override
    public void writeShort(int v) throws IOException {
        for (int i = 0; i < 2; i++) {
            backing.write((v >>> (8 * i)) & 0xFF);
        }
    }

    @Deprecated
    @Override
    public void writeChar(int v) throws IOException {
        // assume UTF-16; this method should probably never be used
        writeShort(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        for (int i = 0; i < 4; i++) {
            backing.write((v >>> (8 * i)) & 0xFF);
        }
    }

    @Override
    public void writeLong(long v) throws IOException {
        for (int i = 0; i < 8; i++) {
            backing.write((byte)((v >>> (8 * i)) & 0xFF));
        }
    }

    @Override
    public void writeFloat(float v) throws IOException {
        writeInt(Float.floatToRawIntBits(v));
    }

    @Override
    public void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToRawLongBits(v));
    }

    @Override
    public void writeBytes(String s) throws IOException {
        backing.write(s.getBytes(StandardCharsets.US_ASCII));
    }

    @Deprecated
    @Override
    public void writeChars(String s) throws IOException {
        // assumes UTF-16; this method should probably never be used
        for (char c : s.toCharArray()) {
            writeChar(c);
        }
    }

    @Deprecated
    @Override
    public void writeUTF(String s) throws IOException {
        // specifically formatted for java data streams; should probably never be used
        writeShort(s.length());
        backing.write(s.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void flush() throws IOException {
        backing.flush();
    }

    @Override
    public void close() throws IOException {
        backing.close();
    }

}
