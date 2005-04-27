/*
 * This Jif class provides a wrapper for the corresponding Java io class.
 * This is necessitated by the label parameters, which require a runtime representation.
 *
 */
package jif.io;
import java.io.*;

public abstract class Reader {
    java.io.Reader wrapped;

    protected Reader(jif.lang.Label L) {
        this.jif$jif_io_Reader_L = L;
    }

    protected Reader jif$io$Reader$() {
        return this;
    }

    public final int read() throws IOException {
        return wrapped.read();
    }
    public final int read(char[] cbuf) throws IOException {
        return wrapped.read(cbuf);
    }
    public final int read(char[] cbuf, int off, int len) throws IOException {
        return wrapped.read(cbuf, off, len);
    }
    public final long skip(long n) throws IOException {
        return wrapped.skip(n);
    }
    public final boolean ready() throws IOException {
        return wrapped.ready();
    }
    public final boolean markSupported() {
        return wrapped.markSupported();
    }
    public final void mark(int readAheadLimit) throws IOException {
        wrapped.mark(readAheadLimit);
    }
    public final void reset() throws IOException {
        wrapped.reset();
    }
    public final void close() throws IOException {
        wrapped.close();
    }

    // manufactured methods
    public static boolean jif$Instanceof(final jif.lang.Label jif$L, final Object o) {
        if (o instanceof Reader) {
            Reader c = (Reader) o;
            return c.jif$jif_io_Reader_L.equivalentTo(jif$L);
        }
        return false;
    }

    public static Reader jif$cast$jif_io_Reader(final jif.lang.Label jif$L, final Object o) {
        if (jif$Instanceof(jif$L, o)) return (Reader) o;
        throw new ClassCastException();
    }

    private final jif.lang.Label jif$jif_io_Reader_L;
}
