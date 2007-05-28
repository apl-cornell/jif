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

    protected JifContext declassifyConstraintContext(JifContext A) throws SemanticException {
        return A;
    }    

    public final Node labelCheckStmt(LabelChecker lc) throws SemanticException
    {
        final DowngradeStmt ds = (DowngradeStmt) node();

        JifContext A = lc.jifContext();
        A = (JifContext) ds.del().enterScope(A);

        Label downgradeTo = ds.label().label();
        Label downgradeFrom = null;
        boolean boundSpecified;
        if (ds.bound() != null) {
            boundSpecified = true;
            downgradeFrom = ds.bound().label();
        }
        else {
            boundSpecified = false;
            downgradeFrom = lc.typeSystem().freshLabelVariable(ds.position(), 
                                                               "downgrade_from", 
            "The label the downgrade statement is downgrading from");
        }

        PathMap initMap = initPathMap(lc);
        Label pc = lc.upperBound(A.pc(), initMap.N());

        lc.constrain(new LabelConstraint(new NamedLabel("pc", pc), 
                                         boundSpecified?LabelConstraint.LEQ:LabelConstraint.EQUAL, 
                                                 new NamedLabel("downgrade_bound", downgradeFrom),
                                                 A.labelEnv(),
                                                 ds.position(),
                                                 boundSpecified) /* report this constraint if the bound was specified*/ {
            public String msg() {
                return "The label of the program counter at this " +
                "program point is " + 
                "more restrictive than the upper bound " +
                "that this " + ds.downgradeKind() + " statement is allowed " +
                "to " + ds.downgradeKind() + ".";
            }
            public String detailMsg() {
                return "This " + ds.downgradeKind() + " statement is allowed to " +
                ds.downgradeKind() + " a program counter labeled up to " +
                namedRhs() + ". However, the label of the " +
                "program counter at this point is " +
                namedLhs() + ", which is more restrictive than " +
                "allowed.";
            }
            public String technicalMsg() {
                return "Invalid " + ds.downgradeKind() + ": PC is out of bound.";
            }                     
        }
        );

        JifContext dA = declassifyConstraintContext(A);
        checkOneDimenOnly(lc, dA, downgradeFrom, downgradeTo, ds.position());
        checkAuthority(lc, dA, downgradeFrom, downgradeTo, ds.position());
        checkAdditionalConstraints(lc, dA, downgradeFrom, downgradeTo, ds.position());

        if (!((JifOptions)JifOptions.global).nonRobustness) {
            checkRobustness(lc, dA, downgradeFrom, downgradeTo, ds.position());
        }

        JifContext bA = bodyContext(A, downgradeFrom, downgradeTo);

        Stmt body = (Stmt) lc.context(bA).labelCheck(ds.body());
        PathMap Xs = X(body);

        PathMap X = null;

        if (Xs.N() instanceof NotTaken) {
            X = Xs;
        }
        else {          
            X = Xs.N(lc.upperBound(Xs.N(), A.pc()));
        }

        return X(ds.body(body), X);
    }
    
    protected JifContext bodyContext(JifContext A, Label downgradeFrom, Label downgradeTo) {
        A = (JifContext) A.pushBlock();
        A.setPc(downgradeTo);
        A.setCurrentCodePCBound(downgradeTo);

        // add a restriction on the "callerPC" label.
        // for non-static methods, we know the this label
        // must be bounded above by the start label
        if (A.currentCode() instanceof JifProcedureInstance) {
            JifTypeSystem ts = (JifTypeSystem)A.typeSystem();
            Label callPC = ts.callSitePCLabel((JifProcedureInstance)A.currentCode());
            A.addAssertionLE(callPC, downgradeTo);
        }
        else if (!A.currentCode().flags().isStatic())  {
            JifClassType jct = (JifClassType)A.currentClass();
            A.addAssertionLE(jct.thisLabel(), downgradeTo);
        }
        return A;
    }

    protected PathMap initPathMap(LabelChecker lc) throws SemanticException {
        return lc.typeSystem().pathMap();
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

    /**
     * Check any additional constraints
     * @param lc
     * @param labelFrom
     * @param labelTo
     * @throws SemanticException 
     */
    protected void checkAdditionalConstraints(LabelChecker lc, 
            JifContext A,
            Label labelFrom, 
            Label labelTo, Position pos) 
    throws SemanticException { }
}
