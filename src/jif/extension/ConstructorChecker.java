package jif.extension;

import jif.types.JifClassType;
import jif.types.JifContext;
import polyglot.types.ClassType;
import polyglot.types.SemanticException;

/** A tool to label check constructors. 
 */
public class ConstructorChecker
{
    public void checkConstructorAuthority(ClassType t, JifContext A)
	throws SemanticException
    {
        if (t instanceof JifClassType) {
            JifClassType ct = (JifClassType)t;

            // Check the authority
            if (!A.actsFor(A.authority(), ct.constructorCallAuthority())) 
                throw new SemanticException("Calling context does not "
                    + "have enough authority to construct \"" + t + "\".");
        }
	else {
	    // Class is a vanilla Java class, so the calling context
	    // vacuously has enough authority to construct the object.
	}
    }
}
