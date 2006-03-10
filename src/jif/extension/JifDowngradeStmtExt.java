package jif.extension;

import jif.JifOptions;
import jif.ast.DowngradeStmt;
import jif.translate.ToJavaExt;
import jif.types.*;
import jif.types.label.Label;
import jif.types.label.NotTaken;
import jif.visit.LabelChecker;
import polyglot.ast.Node;
import polyglot.ast.Stmt;
import polyglot.types.SemanticException;
import polyglot.util.Position;

/** The Jif extension of the <code>DeclassifyStmt</code> node. 
 * 
 *  @see jif.ast.DeclassifyStmt
 */
public abstract class JifDowngradeStmtExt extends JifStmtExt_c
{
    public JifDowngradeStmtExt(ToJavaExt toJava) {
        super(toJava);
    }

    public final Node labelCheckStmt(LabelChecker lc) throws SemanticException
    {
	DowngradeStmt ds = (DowngradeStmt) node();

	JifContext A = lc.jifContext();
        A = (JifContext) ds.enterScope(A);

	Label L = ds.label().label();

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

        checkOneDimenOnly(lc, A, A.pc(), L, ds.position());
        
        checkAuthority(lc, A, A.pc(), L, ds.position());
        
        if (!((JifOptions)JifOptions.global).noRobustness) {
            checkRobustness(lc, A, A.pc(), L, ds.position());
        }

	A = (JifContext) A.pushBlock();
	A.setPc(L);
	A.setCurrentCodePCBound(L);

        // add a restriction on the "callerPC" label.
        if (!A.currentCode().flags().isStatic())  {
            // for non-static methods, we know the this label
            // must be bounded above by the start label
            if (A.currentCode() instanceof JifProcedureInstance) {
                JifTypeSystem ts = (JifTypeSystem)A.typeSystem();
                Label callPC = ts.callSitePCLabel((JifProcedureInstance)A.currentCode());
                A.addAssertionLE(callPC, L);
            }
            else {
                JifClassType jct = (JifClassType)A.currentClass();
                A.addAssertionLE(jct.thisLabel(), L);
            }
        }

	Stmt body = (Stmt) lc.context(A).labelCheck(ds.body());
	PathMap Xs = X(body);

        A = (JifContext) A.pop();

        PathMap X = null;
        
        if (Xs.N() instanceof NotTaken) {
            X = Xs;
        }
        else {          
            X = Xs.set(Path.N, lc.upperBound(Xs.N(), A.pc()));
        }
	
	return X(ds.body(body), X);
    }
    /**
     * Check that only the integrity/confidentiality is downgraded, and not
     * the other dimension.
     * @param lc
     * @param labelFrom
     * @param labelTo
     * @throws SemanticException 
     */
    protected abstract void checkOneDimenOnly(LabelChecker lc, 
                                           JifContext A,
                                           Label labelFrom, 
                                           Label labelTo, Position pos) 
            throws SemanticException;

    /**
     * Check the authority condition
     * @param lc
     * @param labelFrom
     * @param labelTo
     * @throws SemanticException 
     */
    protected abstract void checkAuthority(LabelChecker lc, 
                                           JifContext A,
                                           Label labelFrom, 
                                           Label labelTo, Position pos) 
            throws SemanticException;

    /**
     * Check the robustness condition
     * @param lc
     * @param labelFrom
     * @param labelTo
     * @throws SemanticException 
     */
    protected abstract void checkRobustness(LabelChecker lc, 
                                            JifContext A,
                                            Label labelFrom, 
                                            Label labelTo, Position pos) 
        throws SemanticException;
    
}
