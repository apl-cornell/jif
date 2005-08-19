package jif.extension;

import java.util.Iterator;
import java.util.Set;

import jif.ast.DeclassifyExpr;
import jif.ast.Jif_c;
import jif.translate.ToJavaExt;
import jif.types.*;
import jif.types.label.Label;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.types.SemanticException;

/** The Jif extension of the <code>DeclassifyExpr</code> node. 
 * 
 *  @see jif.ast.DeclassifyExpr
 */
public class JifDeclassifyExprExt extends Jif_c
{
    public JifDeclassifyExprExt(ToJavaExt toJava) {
        super(toJava);
    }

    public Node labelCheck(LabelChecker lc) throws SemanticException {
	DeclassifyExpr d = (DeclassifyExpr) node();

	JifTypeSystem ts = lc.jifTypeSystem();
	JifContext A = lc.jifContext();
        A = (JifContext) d.enterScope(A, null);

	Expr e = (Expr) lc.context(A).labelCheck(d.expr());
	PathMap Xe = X(e);

	Label L = d.label().label();

	Label authLabel = A.authLabel();
        
        lc.constrain(new LabelConstraint(new NamedLabel("expr.nv", Xe.NV()), 
                                         LabelConstraint.LEQ, 
                                         new NamedLabel("declass_bound", d.bound().label()),
                                         A.labelEnv(),
                                         d.position()) {
                     public String msg() {
                         return "The label of the expression to declassify is " + 
                                "more restrictive than label of data that " +
                                "the declassify expression is allowed to declassify.";
                     }
                     public String detailMsg() {
                         return "This declassify expression is allowed to " +
                                 "declassify information labeled up to " +
                                 namedRhs() + ". However, the label of the " +
                                 "expression to declassify is " +
                                 namedLhs() + ", which is more restrictive than " +
                                 "allowed.";
                     }
                     public String technicalMsg() {
                         return "Invalid declassify: NV of the " + 
                                "expression is out of bound.";
                     }                     
         }
         );
	
        final JifContext constraintA = A;
        lc.constrain(new LabelConstraint(new NamedLabel("expr.nv", Xe.NV()), 
                                         LabelConstraint.LEQ, 
                                         new NamedLabel("declass_to_label", L).
                                                   join("auth_label", authLabel),
                                         A.labelEnv(),
                                         d.position()) {
                     public String msg() {
                         return "The method does not have sufficient " +
                                "authority to declassify this expression.";
                     }
                     public String detailMsg() { 
                         StringBuffer sb = new StringBuffer();
                         Set authorities = constraintA.authority();
                         if (authorities.isEmpty()) {
                             sb.append("no principals");
                         }
                         else {
                             sb.append("the following principals: ");
                         }
                         for (Iterator iter = authorities.iterator(); iter.hasNext() ;) {
                             Principal p = (Principal)iter.next();
                             sb.append(p.toString());
                             if (iter.hasNext()) {
                                 sb.append(", ");
                             }
                         }
                         
                          
                         return "The expression to declassify has label " + 
                                namedRhs()+ ", and the expression " +
                                "should be declassified to label " +
                                "declass_to_label. However, the method has " +
                                "the authority of " + sb.toString() + ". " +
                                "The authority of other principals is " +
                                "required to perform the declassification.";
                     }
                     public String technicalMsg() {
                         return "Invalid declassify: the method does " +
                                "not have sufficient authorities.";
                     }                     
         }
         );

	//_pc_ is not declassified. 
	PathMap X = Xe.NV(A.pc().join(L));

	return X(d.expr(e), X);
    }
}
