package jif.ast;

import java.util.Iterator;
import java.util.List;

import jif.types.JifTypeSystem;
import jif.types.label.Label;
import jif.types.principal.Principal;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.PrettyPrinter;

/** An implementation of the <code>PolicyLabel</code> interface.
 */
public class ReaderPolicyLabelNode_c extends PolicyLabelNode_c
{

    public ReaderPolicyLabelNode_c(Position pos, PrincipalNode owner, List principals) {
	super(pos, owner, principals);
    }

    
    protected Label producePairLabel(JifTypeSystem ts, Principal owner, List principals) {
        return ts.pairLabel(position(), 
                            ts.readerPolicy(position(), owner, principals),
                            ts.topLabelM(position()));
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
