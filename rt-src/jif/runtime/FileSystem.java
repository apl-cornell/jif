package jif.runtime;

import jif.lang.*;
import java.util.*;
import java.io.IOException;

/** This class represents the file system, through which you can query
 *  and set the security labels of files.
 */
public class FileSystem
{
    /** Get the security label of <code>file</code>. */
    public static Label labelOf(String file) {
	String[] readers = readers(file);
	String owner = owner(file);

	List readerList = new LinkedList();    
	
	for (int i = 0; i < readers.length; i++) {
	    readerList.add(new NativePrincipal(readers[i]));
	}
	jif.lang.Principal op = new NativePrincipal(owner);
	return Label.policy(op, readerList);
    }
    
    /** Set the access(read) policy of <code>file</code>.  */
    public static void setPolicy(String file, PrivacyPolicy p) 
        throws IOException
    {
	jif.lang.Principal owner = p.owner();
	String[] readers = new String[p.readers().size()];
	int i = 0;
	for (Iterator iter = p.readers().iterator(); iter.hasNext(); )
	    readers[i++] = ((jif.lang.Principal) iter.next()).fullName();
	
	String os = System.getProperty("os.name");
	//in unix systems, files can belong to only one group. so reader
	//set has to be adjusted. 
	if (os.equals("Linux") || os.equals("SunOS") || os.equals("Mac OS X")) {
	    Set groups = groups(owner);
            Policy p1 = null;
            for (Iterator iter = groups.iterator(); iter.hasNext(); ) {
                jif.lang.Principal reader = (jif.lang.Principal) iter.next();
                List readerSet = new LinkedList();
                readerSet.add(reader);
                p1 = new PrivacyPolicy(owner, readerSet);
                if (p.relabelsTo(p1) && p1.relabelsTo(p)) {
                    readers = new String[1];
                    readers[0] = reader.name();
                    setPolicy(file, owner.name(), readers);
                    return;
                }
            }
            String msg = "no group corresponds to the reader set: {";
            for (i = 0; i < readers.length; i++) {
                msg += readers[i];
                if (i < readers.length - 1 ) msg += ", ";
                else msg += "}.";
            }
            
            throw new IOException(msg);
	}
	
	setPolicy(file, owner.name(), readers);
    }
    
    /** Returns the set of groups in which <code>p</code> belongs.
     */
    public static Set groups(jif.lang.Principal p) {
	Set grps = new HashSet();
	Set supers = new HashSet(p.superiors());
	
	while (!supers.isEmpty()) {
	    jif.lang.Principal one = (jif.lang.Principal) supers.iterator().next();
	    supers.addAll(one.superiors());
	    supers.remove(one);
	    grps.add(one);
	}
	
	return grps;
    }
    
    private static native void setPolicy(String file, String owner,
	    String[] readers);							      
    
    private static native String[] readers(String file);
    
    private static native String owner(String file);
    
    static {
        System.loadLibrary("jifrt");
    }
}
