package jif.extension;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import jif.ast.LabelCase;
import jif.ast.SwitchLabel;
import jif.translate.ToJavaExt;
import jif.types.*;
import jif.types.hierarchy.PrincipalHierarchy;
import jif.types.label.Label;
import jif.visit.LabelChecker;
import polyglot.ast.*;
import polyglot.types.SemanticException;

/** Jif extension of the <code>SwitchLabel</code> node.
 *  
 *  @see jif.ast.SwitchLabel
 */
public class JifSwitchLabelExt extends JifStmtExt_c
{
    public JifSwitchLabelExt(ToJavaExt toJava) {
        super(toJava);
    }

    public Node labelCheckStmt(LabelChecker lc) throws SemanticException
    {
	SwitchLabel ss = (SwitchLabel) node();

        JifTypeSystem ts = lc.jifTypeSystem();
        JifContext A = lc.jifContext();
        PrincipalHierarchy ph = A.ph();
        
	A = (JifContext) ss.enterScope(A);

	Expr e = (Expr) lc.context(A).labelCheck(ss.expr());
	PathMap Xe = X(e);

	PathMap Xjoin = ts.pathMap();
	Label pc = Xe.N();

	List cases = new LinkedList();

	for (Iterator iter = ss.cases().iterator(); iter.hasNext(); ) {
	    LabelCase si = (LabelCase) iter.next();

	    if (si.label() != null) {
		Label Li = si.label().label();

                final LabelCase fsi = si;
                final String branchName = (fsi.decl() != null)
                	? ("branch " + fsi.decl().type() + " " + fsi.decl().name())
                    : (fsi.label() == null?("else branch"):("branch " + fsi.label().toString()));
                final String varName = (fsi.decl() != null)
                	? (" variable " + fsi.decl().name())
                    : ("");

                	lc.constrain(new LabelConstraint(new NamedLabel("Xe.nv", "label of the result of the switch expression", Xe.NV()),
                                             LabelConstraint.LEQ,
                                             new NamedLabel("Lc", "label of case variable " + varName, Li).
                                                       join("runtime_label", ts.runtimeLabel()).
                                                       join("PC", "label of program counter just before " + branchName, A.pc()),
                                             A.labelEnv(),
                                             si.position()) {
                             public String msg() { 
                                 return "The label of the switch expression " +
                                        "is more restrictive than the label " +
                                        "of the " + branchName;
                             }
                             public String detailMsg() { 
                                 return "The label of the switch expression " +
                                        "is " + namedLhs() + ", which is more " +
                                        "restrictive than the label " +
                                        "of the " + branchName + ". " +
                                        "This means that this branch of the " +
                                        "switch label statement will never be " +
                                        "executed.";
                             }
                             public String technicalMsg() {
                                 return "the static part of the path NV of " +
                                 "the switch label expression is more " +
                                 "restrictive than the static part of " +
                                 "the label of branch <" + fsi + ">.";
                             }
                         }
                         );

                //@@@@@ this ast will be going soon anyway... just get it to compile
                    // pc = pc.join(Xe.NV().join(Li).labelOf());
                pc = pc.join(Xe.NV().join(Li));
	    }

	    A = (JifContext) A.pushBlock();
	    A.setPc(pc);

	    Formal decl = null;

	    if (si.decl() != null) {
		decl = (Formal) lc.context(A).labelCheck(si.decl());
	    }

	    Stmt body = (Stmt) lc.context(A).labelCheck(si.body());
	    PathMap Xi = X(body);

            A = (JifContext) A.pop();

	    Xjoin = Xjoin.join(Xi);

	    si = si.body(body).decl(decl);
	    si = (LabelCase) X(si, Xi);

	    cases.add(si);
	}

	PathMap X = Xe.NV(ts.bottomLabel(ss.position())).join(Xjoin);
	return X(ss.expr(e).cases(cases).ph(ph), X);
    }
}
