package jif.extension;

import java.util.Iterator;
import java.util.Set;

import jif.JifOptions;
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
import polyglot.util.Position;

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

	JifContext A = lc.jifContext();
        A = (JifContext) d.enterScope(A);

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
                                                   join(lc, "auth_label", authLabel),
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
        
        checkRobustDecl(lc, Xe.NV(), L, d.position());

	//_pc_ is not declassified. 
	PathMap X = Xe.NV(lc.upperBound(A.pc(), L));

	return X(d.expr(e), X);
    }

    /**
     * Check the robust declassification condition
     * @param lc
     * @param labelFrom
     * @param labelTo
     * @throws SemanticException 
     */
    protected static void checkRobustDecl(LabelChecker lc, 
                                 Label labelFrom, 
                                 Label labelTo, Position pos) 
        throws SemanticException {
        
        if (((JifOptions)JifOptions.global).noRobustness) return;
        
        JifTypeSystem jts = lc.typeSystem();
        JifContext A = lc.context();
        Label pcInteg = jts.writersToReadersLabel(pos, A.pc());
        lc.constrain(new LabelConstraint(new NamedLabel("declass_from_label", labelFrom), 
                                         LabelConstraint.LEQ, 
                                         new NamedLabel("declass_to_label", labelTo).
                                                   join(lc, "pc_integrity", pcInteg),
                                         A.labelEnv(),
                                         pos) {
                     public String msg() {
                         return "Declassification not robust: a new reader " +
                                        "may influence the decision to " +
                                        "declassify.";
                     }
                     public String detailMsg() { 
                         return "The declassification of this expression is " +
                         "not robust; at least one of principals that is " +
                         "allowed to read the information after " +
                         "declassification may be able to influence the " +
                         "decision to declassify.";
                     }
         }
         );

        Label fromInteg = jts.writersToReadersLabel(pos, labelFrom);
        lc.constrain(new LabelConstraint(new NamedLabel("declass_from_label", labelFrom), 
                                         LabelConstraint.LEQ, 
                                         new NamedLabel("declass_to_label", labelTo).
                                                   join(lc, "from_label_integrity", fromInteg),
                                         A.labelEnv(),
                                         pos) {
                     public String msg() {
                         return "Declassification not robust: a new reader " +
                                        "may influence the data to be " +
                                        "declassified.";
    }
                     public String detailMsg() { 
                         return "The declassification of this expression is " +
                         "not robust; at least one of principals that is " +
                         "allowed to read the information after " +
                         "declassification may be able to influence the " +
                         "data to be declassified.";
}
         }
         );
    }
}
