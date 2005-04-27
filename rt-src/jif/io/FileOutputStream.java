/*
 * This Jif class provides a wrapper for the corresponding Java io class.
 * This is necessitated by the label parameters, which require a runtime representation.
 *
 */
package jif.io;
import java.io.*;

public class FileOutputStream extends OutputStream
{
    java.io.FileOutputStream wrapped;

    public FileOutputStream(jif.lang.Label L, java.io.FileOutputStream wrapped) {
	this(L);
	this.wrapped = wrapped;
	super.wrapped = this.wrapped;
    }

    public FileOutputStream(jif.lang.Label L) {
	super(L);
	this.jif$jif_io_FileOutputStream_L = L;
    }

    private FileOutputStream jif$io$FileOutputStream$() {
        return this;
    }


    public final FileDescriptor getFD() throws IOException {
        return wrapped.getFD();
    }
    public static boolean jif$Instanceof(final jif.lang.Label jif$L, final Object o) {
        if (o instanceof FileOutputStream) {
            FileOutputStream c = (FileOutputStream) o;
            return c.jif$jif_io_FileOutputStream_L.equivalentTo(jif$L);
        }
        return false;
    }

    public static FileOutputStream jif$cast$jif_io_FileOutputStream(final jif.lang.Label jif$L, final Object o) {
        if (jif$Instanceof(jif$L, o)) return (FileOutputStream) o;
        throw new ClassCastException();
    }

    private final jif.lang.Label jif$jif_io_FileOutputStream_L;
}
