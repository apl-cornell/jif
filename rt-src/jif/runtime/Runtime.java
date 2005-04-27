package jif.runtime;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;

import jif.io.*;
import jif.lang.*;

/**
 * The runtime interface between Jif programs and the underlying system.
 */
public class Runtime {
    private Principal dynp;

    static PrincipalManager pm;

    private Runtime(Principal p) {
	this.dynp = p;
    }

    /**
     * Gets a <code>Runtime</code> object parameterized with the
     * principal <code>p</code>.
     */
    public static Runtime getRuntime(Principal p)
        throws SecurityException
    {
	//check if the current user can act for p
	Principal user = user(p);
	if (! pm.actsFor(user, p)) {
	    throw new SecurityException("The current user does not act for "
		    + p.name() + ".");
	}
	return new Runtime(p);
    }

    /** Get the current user  */
    public static Principal user(Principal parameter) {
	String username = currentUser();
	return (new NativePrincipal(username));
    }

    /**
     * Returns <code>{p:}</code> as the default label, where <code>p</code> is
     * the principal parameter of this <code>Runtime</code> object.
     */
    private Label defaultLabel()
    {
        return Label.policy(dynp, new LinkedList());
    }

    /**
     * Opens a file output stream to write a file with the specific <code>name</code>.
     *
     * @param name     the file name
     * @param append   if true, then bytes will be written to the end of the file
     *                 rather than the beginning
     * @param l        the label parameter of the resulting <code>FileOutputStream</code>
     *
     * @exception  FileNotFoundException
     *      if the file exists but is a directory rather than a regular file,
     *      does not exist but cannot be created, or cannot be opened for any
     *      other reason.
     *
     * @exception  SecurityException
     *      if <code>l</code> is unable to relabel to the Jif label derived from
     *      the ACL of the file.
     */
    public FileOutputStream openFileWrite(String name, boolean append,
	    Label L) throws IOException, SecurityException
    {
	File f = new File(name);
	boolean existed = f.exists();

	if (existed) {
	    Label acLabel = FileSystem.labelOf(name);
	    if (! L.relabelsTo(acLabel))
		throw new SecurityException("The file " + name
			+ "doesn't have sufficient access restrictions.");
	}

	FileOutputStream fos = new FileOutputStream(L, new java.io.FileOutputStream(name, append));

	if (!existed) {
	    fos.flush();
	    FileSystem.setPolicy(name, (PrivacyPolicy) L.policy());
	}

	return fos;
    }

    /** Opens a file input stream for reading from the file with the specific
     *  <code>name</code>.
     *
     *  @param name     the file name
     *  @param l        the the label parameter of the resulting <code>FileInputStream</code>
     *
     *  @exception  SecurityException
     *      if <code>l</code> is less restrictive than the Jif label derived from
     *      the ACL of the file.
     */
    public FileInputStream openFileRead(String name, Label L)
	throws FileNotFoundException, SecurityException
    {
        Label acLabel = FileSystem.labelOf(name);
        if (acLabel.relabelsTo(L)) {
	    return new FileInputStream(L, new java.io.FileInputStream(name));
	}

        throw new SecurityException("The file has more restrictive access "
		+ "control permissions than " + L.toString());
    }

    /**
     * Gets the standard error output.
     * The output channel is parameterized by <code>l</code>.
     */
    public PrintStream stderr(Label l) {
        if ( l.relabelsTo(defaultLabel()) )
	    return new PrintStream(l, System.err);

	throw new SecurityException("The standard error output is not "
		+ "sufficiently secure. ");
    }

    /**
     * Gets the standard output.
     * This output channel is parameterized by <code>l</code>.
     */
    public PrintStream stdout(Label l) {
        if ( l.relabelsTo(defaultLabel()) )
            return new PrintStream(defaultLabel(), System.out);
    else
        return null;
    }

    /**
     * Gets the standard input.
     * This input channel is parameterized by <code>l</code>.
     */
    public InputStream stdin(Label l) {
        if ( defaultLabel().relabelsTo(l) )
	    return new InputStream(l, System.in);
        else
            return null;
    }

    /**
     * Get the standard output parameterized by the default label, which
     * has only one reader: the principal of this <code>Runtime</code> object.
     */
    public PrintStream out() {
        return new PrintStream(defaultLabel(), System.out);
    }

    /**
     * Get the standard input parameterized by the default label, which
     * has only one reader: the principal of this <code>Runtime</code> object.
     */
    public InputStream in() {
        return new InputStream(defaultLabel(), System.in);
    }

    /**
     * Get the standard error output parameterized by the default label, which
     * has only one reader: the principal of this <code>Runtime</code> object.
     */
    public PrintStream err() {
        return new PrintStream(defaultLabel(), System.err);
    }

    public static native String currentUser();

    public static boolean acts_for(Principal p1, Principal p2)
    throws SecurityException
    {
	return pm.actsFor(p1, p2);
    }
    static {
	System.loadLibrary("jifrt");
	String pmAlg = System.getProperty("PrincipalManager");
	pm = PMFactory.create(pmAlg); //FIXME
    }
}
