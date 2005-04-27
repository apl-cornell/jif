/*
 * This Jif class provides a wrapper for the corresponding Java io class.
 * This is necessitated by the label parameters, which require a runtime representation.
 *
 */
package jif.io;
import java.io.*;

public class PrintStream extends FilterOutputStream {
    java.io.PrintStream wrapped;

    public PrintStream jif$io$PrintStream$(final OutputStream out) {
	this.wrapped = new java.io.PrintStream(out.wrapped);
	setWrapped(this.wrapped);
	return this;
    }

    public PrintStream jif$io$PrintStream$(final OutputStream out, final boolean autoFlush) {
	this.wrapped = new java.io.PrintStream(out.wrapped, autoFlush);
	setWrapped(this.wrapped);
	return this;
    }

    public PrintStream(jif.lang.Label L, java.io.PrintStream wrapped) {
	super(L);
	this.jif$jif_io_PrintStream_L = L;
	this.wrapped = wrapped;
	setWrapped(this.wrapped);
    }

    public PrintStream(jif.lang.Label L) {
	super(L);
	this.jif$jif_io_PrintStream_L = L;
    }

    public boolean checkError() {
        return wrapped.checkError();
    }
    public void print(boolean b) {
        wrapped.print(b);
    }
    public void print(char c) {
        wrapped.print(c);
    }
    public void print(int i) {
        wrapped.print(i);
    }
    public void print(long l) {
        wrapped.print(l);
    }
    public void print(float f) {
        wrapped.print(f);
    }
    public void print(double d) {
        wrapped.print(d);
    }
    public void print(char[] s) {
        wrapped.print(s);
    }
    public void print(String s) {
        wrapped.print(s);
    }
    public void print(Object obj) {
        wrapped.print(obj);
    }
    public void println() {
        println();
    }
    public void println(boolean x) {
        wrapped.println(x);
    }
    public void println(char x) {
        wrapped.println(x);
    }
    public void println(int x) {
        wrapped.println(x);
    }
    public void println(long x) {
        wrapped.println(x);
    }
    public void println(float x) {
        wrapped.println(x);
    }
    public void println(double x) {
        wrapped.println(x);
    }
    public void println(char[] x) {
        wrapped.println(x);
    }
    public void println(String x) {
        wrapped.println(x);
    }
    public void println(Object x) {
        wrapped.println(x);
    }
    // manufactured methods
    public static boolean jif$Instanceof(final jif.lang.Label jif$L, final Object o) {
        if (o instanceof PrintStream) {
            PrintStream c = (PrintStream) o;
            return c.jif$jif_io_PrintStream_L.equivalentTo(jif$L);
        }
        return false;
    }

    public static PrintStream jif$cast$jif_io_PrintStream(final jif.lang.Label jif$L, final Object o) {
        if (jif$Instanceof(jif$L, o)) return (PrintStream) o;
        throw new ClassCastException();
    }

    private final jif.lang.Label jif$jif_io_PrintStream_L;
}
