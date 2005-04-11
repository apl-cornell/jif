package jif.extension;

import jif.ast.ActsFor;
import jif.translate.ToJavaExt;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.PathMap;
import jif.types.label.*;
import jif.types.label.AccessPath;
import jif.types.label.AccessPathRoot;
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
        
        PathMap X1;
        if (p1 instanceof DynamicPrincipal) {
            // Get the information given by the successful evaluation of the access path            
            AccessPath path = ((DynamicPrincipal) p1).path();
            X1 = path.labelcheck(A);
        }
        else {
            X1 = ts.pathMap().N(A.pc());            
        }
        
        A = (JifContext) A.pushBlock();
        A.setPc(X1.N());
        
        PathMap X2;        
        if (p2 instanceof DynamicPrincipal) {
            // Get the information given by the successful evaluation of the access path            
            AccessPath path = ((DynamicPrincipal) p2).path();
            X2 = path.labelcheck(A);
        }
        else {
            X2 = ts.pathMap().N(A.pc());            
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
