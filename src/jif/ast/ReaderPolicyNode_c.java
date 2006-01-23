package jif.ast;

import java.util.Iterator;
import java.util.List;

import jif.types.JifTypeSystem;
import jif.types.label.Policy;
import jif.types.principal.Principal;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.PrettyPrinter;

/** An implementation of the <code>PolicyLabel</code> interface.
 */
public class ReaderPolicyNode_c extends PolicyNode_c
{

    public ReaderPolicyNode_c(Position pos, PrincipalNode owner, List principals) {
	super(pos, owner, principals);
    }

    
    protected Policy producePolicy(JifTypeSystem ts, Principal owner, List principals) {
        return ts.readerPolicy(position(), owner, principals);
    }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        print(owner, w, tr);

        w.write(": ");

	for (Iterator i = this.principals.iterator(); i.hasNext(); ) {
	    PrincipalNode n = (PrincipalNode) i.next();
            print(n, w, tr);
            if (i.hasNext()) {
                w.write(";");
                w.allowBreak(0, " ");
            }
        }
    }
}