/*
 * This Jif class provides a wrapper for the corresponding Java io class.
 * This is necessitated by the label parameters, which require a runtime representation.
 *
 */
package jif.io;
import java.io.*;

public class BufferedReader extends Reader {
    java.io.BufferedReader wrapped;

    public BufferedReader(jif.lang.Label L) {
        super(L);
        this.jif$jif_io_BufferedReader_L = L;
    }

    public BufferedReader jif$io$BufferedReader$(final Reader in, final int sz) {
	this.wrapped = new java.io.BufferedReader(in.wrapped, sz);
	super.wrapped = this.wrapped;
        return this;
    }

    public BufferedReader jif$io$BufferedReader$(final Reader in) {
	this.wrapped = new java.io.BufferedReader(in.wrapped);
	super.wrapped = this.wrapped;
        return this;
    }


    public String readLine() throws IOException {
        return wrapped.readLine();
    }
    public static boolean jif$Instanceof(final jif.lang.Label jif$L, final Object o) {
        if (o instanceof BufferedReader) {
            BufferedReader c = (BufferedReader) o;
            return c.jif$jif_io_BufferedReader_L.equivalentTo(jif$L);
        }
        return false;
    }

    public static BufferedReader jif$cast$jif_io_BufferedReader(final jif.lang.Label jif$L, final Object o) {
        if (jif$Instanceof(jif$L, o)) return (BufferedReader) o;
        throw new ClassCastException();
    }

    private final jif.lang.Label jif$jif_io_BufferedReader_L;
}
