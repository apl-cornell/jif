package jif.extension;

import jif.ast.Jif;
import jif.ast.Jif_c;
import jif.translate.ToJavaExt;
import jif.types.JifContext;
import jif.types.PathMap;
import jif.visit.LabelChecker;
import polyglot.ast.*;
import polyglot.types.SemanticException;

/** The Jif extension of the <code>Assign</code> node. 
 */
public abstract class JifAssignExt extends Jif_c
{
    public JifAssignExt(ToJavaExt toJava) {
        super(toJava);
    }

    SubtypeChecker subtypeChecker = new SubtypeChecker();

    public Node labelCheck(LabelChecker lc) throws SemanticException {
	Assign a = (Assign) node();

	JifContext A = lc.jifContext();
        A = (JifContext) a.enterScope(A);

	Assign checked = (Assign)labelCheckLHS(lc);

        // Only need subtype constraints for "=" operator.  No other
	// assignment operator works with class types.
	if (a.operator() == Assign.ASSIGN) {
	    // Must check that the RHS is a subtype of the LHS.
	    // Most of this is done in typeCheck, but if lhs and rhs are
	    // instantitation types, we must add constraints for the labels.
	    subtypeChecker.addSubtypeConstraints(lc, a.position(),
					         checked.left().type(),
					         checked.right().type());
	}
        
        return checked;
    }
    
    protected abstract Node labelCheckLHS(LabelChecker lc) throws SemanticException;
}
