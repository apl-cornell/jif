package jif.extension;

import java.util.List;

import jif.types.*;
import jif.types.principal.Principal;
import polyglot.types.ClassType;
import polyglot.types.SemanticException;
import polyglot.util.Position;

/** A tool to label check constructors. 
 */
public class ConstructorChecker
{
    public void checkConstructorAuthority(ClassType t, JifContext A, Position pos)
    throws SemanticException
    {
        if (t instanceof JifClassType && !checkAuthority((JifClassType)t, A)) {
            throw new SemanticDetailedException(
                    "Calling context does not "
                            + "have enough authority to construct \"" + t
                            + "\".",
                    "In order to construct an instance of class "
                            + t
                            + ", the calling context must have the authority of "
                            + "the following principal(s): "
                            + principalListString(((JifClassType)t).constructorCallAuthority())
                            + ".", pos);
        }
    }
    public void checkStaticMethodAuthority(JifMethodInstance mi, JifContext A, Position pos)
    throws SemanticException
    {
        ClassType t = mi.container().toClass();
        if (t instanceof JifClassType && !checkAuthority((JifClassType)t, A)) {
            throw new SemanticDetailedException("Calling context does not "
                    + "have enough authority to invoke the static method " + 
                    mi.signature() + " of class " + t
                    + ".", 
                "In order to call a static method of class " + t
                    + ", the calling context must have the authority of "
                    + "the following principal(s): "
                    + principalListString(((JifClassType)t).constructorCallAuthority()) + ".",
                    pos);
        }
    }

    private boolean checkAuthority(JifClassType t, JifContext A) {
        // Check the authority
        return A.actsFor(A.authority(), t.constructorCallAuthority());
    }
    
    private String principalListString(List principals) {
        int size = principals.size();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < size; i++) {
            Principal p = (Principal)principals.get(i);
            sb.append(p);
            if (i == size-2) {
                sb.append(" and ");
            }
            else if (i < size-2) {
                sb.append(", ");                
            }
        }
        return sb.toString();
    }
}
