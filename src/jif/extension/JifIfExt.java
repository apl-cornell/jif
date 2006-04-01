package jif.extension;

import jif.ast.JifUtil;
import jif.translate.ToJavaExt;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.PathMap;
import jif.types.label.Label;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.ast.*;
import polyglot.types.SemanticException;
import polyglot.util.ErrorInfo;
import polyglot.util.ErrorQueue;
import polyglot.util.InternalCompilerError;

/** The Jif extension of the <code>If</code> node. 
 * 
 *  @see polyglot.ast.If
 */
public class JifIfExt extends JifStmtExt_c
{
    public JifIfExt(ToJavaExt toJava) {
        super(toJava);
    }

    public Node labelCheckStmt(LabelChecker lc) throws SemanticException {
	If is = (If) node();

        JifTypeSystem ts = lc.jifTypeSystem();
        JifContext A = lc.jifContext();
        A = (JifContext) is.enterScope(A);

	Expr e = (Expr) lc.context(A).labelCheck(is.cond());

	PathMap Xe = X(e);
	
	A = (JifContext) A.pushBlock();
	A.setPc(Xe.NV());
        
        // extend the context with any label tests or actsfor tests
        JifContext Aextnd = extendContext(lc, A, e);

	Stmt S1 = (Stmt) lc.context(Aextnd).labelCheck(is.consequent());

        A = (JifContext) A.pop();
	PathMap X1 = X(S1);

	Stmt S2 = null;
	PathMap X2;

	if (is.alternative() != null) {
	    A = (JifContext) A.pushBlock();
	    A.setPc(Xe.NV());

	    S2 = (Stmt) lc.context(A).labelCheck(is.alternative());

            A = (JifContext) A.pop();
	    X2 = X(S2);
	}
	else {
	    // Simulate the effect of an empty statement.
	    // X0[node() := A[pc := Xe[nv][pc]]] == Xe[nv]
	    X2 = ts.pathMap();
	    X2 = X2.N(Xe.NV());
	}

	/*
	trace("Xe == " + Xe);
	trace("X1 == " + X1);
	trace("X2 == " + X2);
	*/

	PathMap X = Xe.N(ts.notTaken()).join(X1).join(X2);
	X = X.NV(ts.notTaken());
	return X(is.cond(e).consequent(S1).alternative(S2), X);
    }

    private JifContext extendContext(LabelChecker lc, JifContext A, Expr e) throws SemanticException {
        JifTypeSystem ts = lc.typeSystem();
        if (allEnvExtensionAsConjuncts(ts, e)) {
            return extendContext(lc, A, (Binary)e);
        }
        else if (someEnvExtension(ts, e)) {
            // give a warning.
            ErrorQueue eq = lc.errorQueue();
            eq.enqueue(ErrorInfo.WARNING,
                       "The Jif compiler can only reason about label tests " +
                       "and actsfor tests if they occur as conjuncts in the " +
                       "conditional of an if statement.",
                       e.position());
        }
                
        return A;
    }

    private JifContext extendContext(LabelChecker lc, JifContext A, Binary b) throws SemanticException {
        JifTypeSystem ts = lc.typeSystem();
        Binary.Operator op = b.operator();
        
        if (op == JifBinaryDel.ACTSFOR) {
            Principal actor = JifUtil.exprToPrincipal(ts, b.left(), A);
            Principal granter = JifUtil.exprToPrincipal(ts, b.right(), A);
            A.addActsFor(actor, granter);
            return A;
        }
        else if (op == JifBinaryDel.EQUIV && ts.isImplicitCastValid(b.left().type(), ts.Principal())) {
            Principal left = JifUtil.exprToPrincipal(ts, b.left(), A);
            Principal right = JifUtil.exprToPrincipal(ts, b.right(), A);
            A.addEquiv(left, right);            
            return A;
        }
        else if (op == JifBinaryDel.EQUIV && ts.isLabel(b.left().type())) {
            Label lhs = JifUtil.exprToLabel(ts, b.left(), A);
            Label rhs = JifUtil.exprToLabel(ts, b.right(), A);
            A.addAssertionLE(lhs, rhs);
            A.addAssertionLE(rhs, lhs);
            return A;
        }
        else if (op == Binary.LE && ts.isLabel(b.left().type())) {
            Label lhs = JifUtil.exprToLabel(ts, b.left(), A);
            Label rhs = JifUtil.exprToLabel(ts, b.right(), A);
            A.addAssertionLE(lhs, rhs);
            return A;
        }
        else if (b.operator() == Binary.BIT_AND || b.operator() == Binary.COND_AND) {
            if (b.left() instanceof Binary && b.right() instanceof Binary) {
                JifContext A1 = extendContext(lc, A, (Binary)b.left());
                return extendContext(lc, A1, (Binary)b.right());
            }
        }
        return A;
    }

    /**
     * Return true if and only if all label and actsfor tests appear as conjuncts  
     */
    private boolean allEnvExtensionAsConjuncts(JifTypeSystem ts, Expr e) {
        if (e instanceof Binary) {
            Binary b = (Binary)e;
            if (b.operator() == JifBinaryDel.ACTSFOR || b.operator() == JifBinaryDel.EQUIV) {
                return true;
            }
            if (b.operator() == Binary.LE && ts.isLabel(b.left().type()) && ts.isLabel(b.right().type())) {
                return true;
            }
            if (b.operator() == Binary.BIT_AND || b.operator() == Binary.COND_AND) {
                return allEnvExtensionAsConjuncts(ts, b.left()) && allEnvExtensionAsConjuncts(ts, b.right());
            }       
            // return false if the left or the right has some env extension
            return !(someEnvExtension(ts, b.left()) || someEnvExtension(ts, b.right()));
        }
        return false;
    }
    
    /**
     * Return true if and only if e contains at least one label or actsfor test  
     */
    private boolean someEnvExtension(JifTypeSystem ts, Expr e) {
        if (e instanceof Binary) {
            Binary b = (Binary)e;
            if (b.operator() == JifBinaryDel.ACTSFOR || b.operator() == JifBinaryDel.EQUIV) {
                return true;
            }
            if (b.operator() == Binary.LE && ts.isLabel(b.left().type()) && ts.isLabel(b.right().type())) {
                return true;
            }
            return someEnvExtension(ts, b.left()) || someEnvExtension(ts, b.right());
        }
        return false;
    }
    
}
