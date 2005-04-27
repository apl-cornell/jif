/*
 * This Jif class provides a wrapper for the corresponding Java io class.
 * This is necessitated by the label parameters, which require a runtime representation.
 *
 */
package jif.io;
import java.io.*;

public class InputStream {
    java.io.InputStream wrapped;

    protected InputStream(jif.lang.Label L) {
        this.jif$jif_io_InputStream_L = L;
    }

    public InputStream(jif.lang.Label L, java.io.InputStream wrapped) {
        this.jif$jif_io_InputStream_L = L;
        this.wrapped = wrapped;
    }

    public InputStream jif$io$InputStream$() {
        return this;
    }

    public final int read() throws IOException {
        return wrapped.read();
    }
    public final int read(byte[] b) throws IOException {
        return wrapped.read(b);
    }
    public final int read(byte[] b, int off, int len) throws IOException {
        return wrapped.read(b, off, len);

    }
    public final long skip(long n) throws IOException {
        return wrapped.skip(n);

    }
    public final int available() throws IOException {
        return wrapped.available();

    }
    public final void close() throws IOException {
        wrapped.close();

    }
    public final void mark(int readlimit) {
        wrapped.mark(readlimit);

    }
    public final void reset() throws IOException {
        wrapped.reset();

    }
    public final boolean markSupported() {
        return wrapped.markSupported();

    }
    public static boolean jif$Instanceof(final jif.lang.Label jif$L, final Object o) {
        if (o instanceof InputStream) {
            InputStream c = (InputStream) o;
            return c.jif$jif_io_InputStream_L.equivalentTo(jif$L);
        }
        return false;
    }

    public static InputStream jif$cast$jif_io_InputStream(final jif.lang.Label jif$L, final Object o) {
        if (jif$Instanceof(jif$L, o)) return (InputStream) o;
        throw new ClassCastException();
    }

    private final jif.lang.Label jif$jif_io_InputStream_L;

}
