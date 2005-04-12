package jif.ast;

import polyglot.ext.jl.ast.*;
import jif.types.*;
import jif.types.principal.ParamPrincipal;
import jif.visit.*;
import polyglot.ast.*;
import polyglot.types.*;
import polyglot.visit.*;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.util.InternalCompilerError;
import polyglot.util.CollectionUtil;
import polyglot.frontend.Pass;
import java.util.*;

/** An implementation of the <tt>LabelIf</tt> interface. */
public class LabelIf_c extends Stmt_c implements LabelIf
{
    protected LabelExpr lhs;
    protected LabelExpr rhs;
    protected Stmt consequent;
    protected Stmt alternative;

    public LabelIf_c(Position pos, LabelExpr lhs, LabelExpr rhs, Stmt consequent, Stmt alternative) {
	super(pos);
	this.lhs = lhs;
	this.rhs = rhs;
	this.consequent = consequent;
	this.alternative = alternative;
    }

    /** Gets the lhs principal. */
    public LabelExpr lhs() {
	return this.lhs;
    }

    /** Sets the lhs principal. */
    public LabelIf lhs(LabelExpr lhs) {
	LabelIf_c n = (LabelIf_c) copy();
	n.lhs = lhs;
	return n;
    }

    /** Gets the rhs principal. */
    public LabelExpr rhs() {
	return this.rhs;
    }

    /** Sets the rhs principal. */
    public LabelIf rhs(LabelExpr rhs) {
	LabelIf_c n = (LabelIf_c) copy();
	n.rhs = rhs;
	return n;
    }

    /** Gets the consequent statement. */
    public Stmt consequent() {
	return this.consequent;
    }

    /** Sets the consequent statement. */
    public LabelIf consequent(Stmt consequent) {
	LabelIf_c n = (LabelIf_c) copy();
	n.consequent = consequent;
	return n;
    }

    /** Gets the alternative statement. */
    public Stmt alternative() {
	return this.alternative;
    }

    /** Sets the alternative statement. */
    public LabelIf alternative(Stmt alternative) {
	LabelIf_c n = (LabelIf_c) copy();
	n.alternative = alternative;
	return n;
    }

    /** Reconstructs the node. */
    protected LabelIf_c reconstruct(LabelExpr lhs, LabelExpr rhs, Stmt consequent, Stmt alternative) {
	if (lhs != this.lhs || rhs != this.rhs || consequent != this.consequent || alternative != this.alternative) {
	    LabelIf_c n = (LabelIf_c) copy();
	    n.lhs = lhs;
	    n.rhs = rhs;
	    n.consequent = consequent;
	    n.alternative = alternative;
	    return n;
	}

	return this;
    }

    /** Visits the children of the node. */
    public Node visitChildren(NodeVisitor v) {
	LabelExpr lhs = (LabelExpr) visitChild(this.lhs, v);
	LabelExpr rhs = (LabelExpr) visitChild(this.rhs, v);
	Stmt consequent = (Stmt) visitChild(this.consequent, v);
	Stmt alternative = (Stmt) visitChild(this.alternative, v);
	return reconstruct(lhs, rhs, consequent, alternative);
    }

    /** Type check the expression. */
    public Node typeCheck(TypeChecker tc) throws SemanticException {
	JifTypeSystem ts = (JifTypeSystem) tc.typeSystem();

	if (!lhs.label().label().isRuntimeRepresentable()) {
	    throw new SemanticException(
                    "A label used for a run-time test must be runtime-representable.", 
                    lhs.position());
	}

	if (!rhs.label().label().isRuntimeRepresentable()) {
	    throw new SemanticException(
	        "A label used for a run-time test must be runtime-representable.", 
		rhs.position());
	}

	return this;
    }

    public Term entry() {
        return lhs.entry();
    }

    public List acceptCFG(CFGBuilder v, List succs) {
        v.visitCFG(lhs, rhs.entry());

        if (alternative == null) {
            v.visitCFG(rhs, FlowGraph.EDGE_KEY_TRUE, consequent.entry(), 
                                FlowGraph.EDGE_KEY_FALSE, this);
            v.visitCFG(consequent, this);
        }
        else {
            v.visitCFG(rhs, FlowGraph.EDGE_KEY_TRUE, consequent.entry(),
                                FlowGraph.EDGE_KEY_FALSE, alternative.entry());
            v.visitCFG(consequent, this);
            v.visitCFG(alternative, this);
        }

        return succs;
    }
    
    public String toString() {
	return "if (" + lhs + " <= " + rhs + ") ...";
    }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write("if (");
        print(lhs, w, tr);
        w.write(" <= ");
        print(rhs, w, tr);
        w.write(")");

        printSubStmt(consequent, w, tr);

        if (alternative != null) {
            if (consequent instanceof Block) {
                // allow the "} else {" formatting
                w.write(" ");
            }
            else {
                w.allowBreak(0, " ");
            }

            w.write ("else");
            printSubStmt(alternative, w, tr);
        }
    }

    public void translate(CodeWriter w, Translator tr) {
        throw new InternalCompilerError("cannot translate " + this);
    }
}
