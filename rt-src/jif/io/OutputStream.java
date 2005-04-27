/*
 * This Jif class provides a wrapper for the corresponding Java io class.
 * This is necessitated by the label parameters, which require a runtime representation.
 *
 */
package jif.io;
import java.io.*;

public abstract class OutputStream {
    java.io.OutputStream wrapped;

    protected OutputStream(jif.lang.Label L) {
        this.jif$jif_io_OutputStream_L = L;
    }
    public OutputStream jif$io$OutputStream$() {
        return this;
    }

    public final void write(int b) throws IOException {
        wrapped.write(b);
    }
    public final void write(byte[] b) throws IOException {
        wrapped.write(b);
    }
    public final void write(byte[] b, int off, int len) throws IOException {
        wrapped.write(b, off, len);
    }
    public final void flush() throws IOException {
        wrapped.flush();
    }
    public final void close() throws IOException {
        wrapped.close();
    }
    public static boolean jif$Instanceof(final jif.lang.Label jif$L, final Object o) {
        if (o instanceof OutputStream) {
            OutputStream c = (OutputStream) o;
            return c.jif$jif_io_OutputStream_L.equivalentTo(jif$L);
        }
        return false;
    }

    public static OutputStream jif$cast$jif_io_OutputStream(final jif.lang.Label jif$L, final Object o) {
        if (jif$Instanceof(jif$L, o)) return (OutputStream) o;
        throw new ClassCastException();
    }

    private final jif.lang.Label jif$jif_io_OutputStream_L;

}
