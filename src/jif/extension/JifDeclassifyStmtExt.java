package jif.extension;

import java.util.Iterator;
import java.util.Set;

import jif.ast.DeclassifyStmt;
import jif.translate.ToJavaExt;
import jif.types.*;
import jif.types.label.Label;
import jif.types.label.NotTaken;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.ast.Node;
import polyglot.ast.Stmt;
import polyglot.types.SemanticException;

/** The Jif extension of the <code>DeclassifyStmt</code> node. 
 * 
 *  @see jif.ast.DeclassifyStmt
 */
public class JifDeclassifyStmtExt extends JifStmtExt_c
{
    public JifDeclassifyStmtExt(ToJavaExt toJava) {
        super(toJava);
    }

    public Node labelCheckStmt(LabelChecker lc) throws SemanticException
    {
	DeclassifyStmt ds = (DeclassifyStmt) node();

	JifContext A = lc.jifContext();
        A = (JifContext) ds.enterScope(A, null);

	Label L = ds.label().label();

	Label auth = A.authLabel();
        
        lc.constrain(new LabelConstraint(new NamedLabel("pc", A.pc()), 
                                         LabelConstraint.LEQ, 
                                         new NamedLabel("declass_bound", ds.bound().label()),
                                         A.labelEnv(),
                                         ds.position()) {
                     public String msg() {
                         return "The label of the program counter at this " +
                                "program point is " + 
                                "more restrictive than the upper bound " +
                                "that this declassify statement is allowed " +
                                "to declassify.";
                     }
                     public String detailMsg() {
                         return "This declassify statement is allowed to " +
                                 "declassify a program counter labeled up to " +
                                 namedRhs() + ". However, the label of the " +
                                 "program counter at this point is " +
                                 namedLhs() + ", which is more restrictive than " +
                                 "allowed.";
                     }
                     public String technicalMsg() {
                         return "Invalid declassify: PC is out of bound.";
                     }                     
         }
         );

        final JifContext constraintA = A;
        lc.constrain(new LabelConstraint(new NamedLabel("pc", A.pc()), 
                                         LabelConstraint.LEQ, 
                                         new NamedLabel("declass_to_label", L).
                                                   join("auth_label", auth),
                                         A.labelEnv(),
                                         ds.position()) {
                     public String msg() {
                         return "The method does not have sufficient " +
                                "authority to declassify the program counter " +
                                "at this point.";
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
                         
                          
                         return "The program counter at this point has label " + 
                                namedRhs()+ ", and " +
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

	A = (JifContext) A.pushBlock();
	A.setPc(L);
	A.setEntryPC(L);

	Stmt body = (Stmt) lc.context(A).labelCheck(ds.body());
	PathMap Xs = X(body);

        A = (JifContext) A.pop();

        PathMap X = null;
        
        if (Xs.N() instanceof NotTaken) {
            X = Xs;
        }
        else {          
            X = Xs.set(Path.N, Xs.N().join(A.pc()));
        }
	
	return X(ds.body(body), X);
    }
}
