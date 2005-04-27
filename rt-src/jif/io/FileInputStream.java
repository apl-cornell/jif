/*
 * This Jif class provides a wrapper for the corresponding Java io class.
 * This is necessitated by the label parameters, which require a runtime representation.
 *
 */
package jif.io;
import java.io.*;

public class FileInputStream extends InputStream
{
    java.io.FileInputStream wrapped;

    public FileInputStream(jif.lang.Label L, java.io.FileInputStream wrapped) {
	this(L);
	this.wrapped = wrapped;
	super.wrapped = this.wrapped;
    }

    public FileInputStream(jif.lang.Label L) {
	super(L);
	this.jif$jif_io_FileInputStream_L = L;
    }

    private FileInputStream jif$io$FileInputStream$() {
        return this;
    }

    public final FileDescriptor getFD()  throws IOException {
        return wrapped.getFD();
    }
    public static boolean jif$Instanceof(final jif.lang.Label jif$L, final Object o) {
        if (o instanceof FileInputStream) {
            FileInputStream c = (FileInputStream) o;
            return c.jif$jif_io_FileInputStream_L.equivalentTo(jif$L);
        }
        return false;
    }

    public static FileInputStream jif$cast$jif_io_FileInputStream(final jif.lang.Label jif$L, final Object o) {
        if (jif$Instanceof(jif$L, o)) return (FileInputStream) o;
        throw new ClassCastException();
    }

    private final jif.lang.Label jif$jif_io_FileInputStream_L;
}
