package jif.extension;

import jif.ast.ActsFor;
import jif.translate.ToJavaExt;
import jif.types.*;
import jif.types.label.Label;
import jif.types.principal.DynamicPrincipal;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.ast.Node;
import polyglot.ast.Stmt;
import polyglot.types.SemanticException;

/** The Jif extension of the <code>ActsFor</code> node. 
 */
public class JifActsForExt extends JifStmtExt_c
{
    public JifActsForExt(ToJavaExt toJava) {
        super(toJava);
    }

    /** Label check the <code>actsFor</code> statement.
     *  Refer to Figure 4.25
     */
    public Node labelCheckStmt(LabelChecker lc) throws SemanticException
    {
	ActsFor af = (ActsFor) node();

        JifContext A = lc.jifContext();
        A = (JifContext) af.enterScope(A);
        JifTypeSystem ts = lc.jifTypeSystem();

        Principal p1 = af.actor().principal();
        Principal p2 = af.granter().principal();

        PathMap X1 = ts.pathMap().N(A.pc());
        PathMap X2 = ts.pathMap().N(A.pc());

        if (p1 instanceof DynamicPrincipal) {
            // <pr-dynamic uid L> was <var final principal{L} uid>
            // Simulate the effect of the variable lookup.
            Label L = ((DynamicPrincipal) p1).label();
            X1 = X1.NV(L.join(A.pc()));
        }

        A = (JifContext) A.pushBlock();
        A.setPc(X1.N());

        if (p2 instanceof DynamicPrincipal) {
            // <pr-dynamic uid L> was <var final principal{L} uid>
            // Simulate the effect of the variable lookup.
            Label L = ((DynamicPrincipal) p2).label();
            X2 = X2.NV(L.join(A.pc()));
        }

        A = (JifContext) A.pop();

        A = (JifContext) A.pushBlock();

        A.setPc(X1.NV().join(X2.NV()));

        A = (JifContext) A.pushBlock();
        A.addActsFor(p1, p2);

        Stmt S1 = (Stmt) lc.context(A).labelCheck(af.consequent());
        PathMap X3 = X(S1);

        A = (JifContext) A.pop();

        PathMap X4; 

        Stmt S2 = null;

        if (af.alternative() != null) {
            S2 = (Stmt) lc.context(A).labelCheck(af.alternative());
            X4 = X(S2);
        }
        else {
            X4 = ts.pathMap().N(A.pc());
        }

        A = (JifContext) A.pop();

        PathMap X = X1.join(X2).join(X3).join(X4);

        return X(af.consequent(S1).alternative(S2), X);
    }
}
