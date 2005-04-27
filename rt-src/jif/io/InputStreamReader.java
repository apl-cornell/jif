/*
 * This Jif class provides a wrapper for the corresponding Java io class.
 * This is necessitated by the label parameters, which require a runtime representation.
 *
 */
package jif.io;
import java.io.*;

public class InputStreamReader extends Reader {
    java.io.InputStreamReader wrapped;

    public InputStreamReader(jif.lang.Label L) {
	super(L);
	this.jif$jif_io_InputStreamReader_L = L;
    }

    public InputStreamReader jif$io$InputStreamReader$(final InputStream in) {
	this.wrapped = new java.io.InputStreamReader(in.wrapped);
	super.wrapped = this.wrapped;
        return this;
    }

    public static boolean jif$Instanceof(final jif.lang.Label jif$L, final Object o) {
        if (o instanceof InputStreamReader) {
            InputStreamReader c = (InputStreamReader) o;
            return c.jif$jif_io_InputStreamReader_L.equivalentTo(jif$L);
        }
        return false;
    }

    public static InputStreamReader jif$cast$jif_io_InputStreamReader(final jif.lang.Label jif$L, final Object o) {
        if (jif$Instanceof(jif$L, o)) return (InputStreamReader) o;
        throw new ClassCastException();
    }

    private final jif.lang.Label jif$jif_io_InputStreamReader_L;
}
