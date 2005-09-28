package jif.extension;

import jif.ast.ActsFor;
import jif.ast.PrincipalNode;
import jif.translate.ToJavaExt;
import jif.types.*;
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
        
        
        
	PrincipalNode actor = (PrincipalNode)lc.context(A).labelCheck(af.actor());
	PathMap Xactor = X(actor);

	A = (JifContext) A.pushBlock();
	A.setPc(Xactor.N());
	PrincipalNode granter = (PrincipalNode) lc.context(A).labelCheck(af.granter());
	PathMap Xgranter = X(granter);
        A = (JifContext) A.pop();

	PathMap X2 = Xactor.set(Path.N, ts.notTaken()).join(Xgranter);

	
        A = (JifContext) A.pushBlock();
        A.setPc(X2.N().join(X2.NV()));
        if (af.kind() == ActsFor.EQUIV) {
            A.addEquiv(actor.principal(), granter.principal());            
        }
        else {
            A.addActsFor(actor.principal(), granter.principal());            
        }
        
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
        
        PathMap X = Xactor.join(X2).join(X3).join(X4);
        
        return X(af.actor(actor).granter(granter).consequent(S1).alternative(S2), X);
    }
}
