package jif.extension;

import jif.ast.LabelExpr;
import jif.ast.LabelIf;
import jif.translate.ToJavaExt;
import jif.types.*;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.PathMap;
import jif.types.label.AccessPath;
import jif.types.label.DynamicLabel;
import jif.types.label.Label;
import jif.types.principal.DynamicPrincipal;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.ast.Stmt;
import polyglot.types.SemanticException;

/** The Jif extension of the <code>ActsFor</code> node. 
 */
public class JifLabelIfExt extends JifStmtExt_c
{
    public JifLabelIfExt(ToJavaExt toJava) {
        super(toJava);
    }
    
    /** Label check the <code>actsFor</code> statement.
     *  Refer to Figure 4.25
     */
    public Node labelCheckStmt(LabelChecker lc) throws SemanticException
    {
        LabelIf labelIf = (LabelIf) node();
        
        JifContext A = lc.jifContext();
        A = (JifContext) labelIf.enterScope(A, null);
        JifTypeSystem ts = lc.jifTypeSystem();
        

	LabelExpr lhs = (LabelExpr) lc.context(A).labelCheck(labelIf.lhs());
	PathMap Xl = X(lhs);

	A = (JifContext) A.pushBlock();
	A.setPc(Xl.N());
	LabelExpr rhs = (LabelExpr) lc.context(A).labelCheck(labelIf.rhs());
	PathMap Xr = X(rhs);
        A = (JifContext) A.pop();

	PathMap X2 = Xl.N(ts.notTaken()).join(Xr);



	A = (JifContext) A.pushBlock();
        
        A.setPc(X2.N().join(X2.NV()));
        
        A = (JifContext) A.pushBlock();
        // add the assertion that the conditional is true to check
        // the consequent.
        A.addAssertionLE(lhs.label().label(), rhs.label().label());
        Stmt S1 = (Stmt) lc.context(A).labelCheck(labelIf.consequent());
        PathMap X3 = X(S1);
        
        A = (JifContext) A.pop();
        
        PathMap X4; 
        
        Stmt S2 = null;
        
        if (labelIf.alternative() != null) {
            S2 = (Stmt) lc.context(A).labelCheck(labelIf.alternative());
            X4 = X(S2);
        }
        else {
            X4 = ts.pathMap().N(A.pc());
        }
        
        A = (JifContext) A.pop();
        
        PathMap X = X2.join(X3).join(X4);
        
        return X(labelIf.consequent(S1).alternative(S2), X);
    }
}
