/*
 * This Jif class provides a wrapper for the corresponding Java io class.
 * This is necessitated by the label parameters, which require a runtime representation.
 *
 */
package jif.io;
import java.io.*;

public class FilterOutputStream extends OutputStream {
    java.io.FilterOutputStream wrapped;

    protected FilterOutputStream(jif.lang.Label L) {
	super(L);
	this.jif$jif_io_FilterOutputStream_L = L;
    }

    public FilterOutputStream jif$io$FilterOutputStream$(final OutputStream out) {
        this.setWrapped(new java.io.FilterOutputStream(out.wrapped));
        return this;
    }

    protected void setWrapped(java.io.FilterOutputStream wrapped) {
        this.wrapped = wrapped;
	super.wrapped = this.wrapped;
    }
    public static boolean jif$Instanceof(final jif.lang.Label jif$L, final Object o) {
        if (o instanceof FilterOutputStream) {
            FilterOutputStream c = (FilterOutputStream) o;
            return c.jif$jif_io_FilterOutputStream_L.equivalentTo(jif$L);
        }
        return false;
    }

    public static FilterOutputStream jif$cast$jif_io_FilterOutputStream(final jif.lang.Label jif$L, final Object o) {
        if (jif$Instanceof(jif$L, o)) return (FilterOutputStream) o;
        throw new ClassCastException();
    }

    private final jif.lang.Label jif$jif_io_FilterOutputStream_L;
}
