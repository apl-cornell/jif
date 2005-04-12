package jif.ast;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.hierarchy.PrincipalHierarchy;
import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.ast.Term;
import polyglot.ext.jl.ast.Stmt_c;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.*;
import polyglot.visit.*;

/** An implementation of the <code>SwitchLabel</code> interface. */ 
public class SwitchLabel_c extends Stmt_c implements SwitchLabel {
    protected Expr expr;
    protected List cases;
    protected PrincipalHierarchy ph;

    public SwitchLabel_c(Position pos, Expr expr, List cases) {
	super(pos);
	this.expr = expr;
	this.cases = TypedList.copyAndCheck(cases, LabelCase.class, true);
    }

    public SwitchLabel_c reconstruct(Expr expr, List cases) {
	if (expr != this.expr || ! CollectionUtil.equals(cases, this.cases)) {
	    SwitchLabel_c n = (SwitchLabel_c) copy();
	    n.expr = expr;
	    n.cases = cases;
	    return n;
	}
	
	return this;
    }

    public Node visitChildren(NodeVisitor v) {
	Expr expr = (Expr) visitChild(this.expr, v);
	List cases = visitList(this.cases, v);
	return reconstruct(expr, cases);
    }

    public Expr expr() {
	return this.expr;
    }

    public SwitchLabel expr(Expr expr) {
	SwitchLabel_c n = (SwitchLabel_c) copy();
	n.expr = expr;
	return n;
    }

    public List cases() {
	return this.cases;
    }

    public SwitchLabel cases(List cases) {
	SwitchLabel_c n = (SwitchLabel_c) copy();
	n.cases = cases;
	return n;
    }

    public PrincipalHierarchy ph() {
        return ph;
    }

    public SwitchLabel ph(PrincipalHierarchy ph) {
        SwitchLabel_c n = (SwitchLabel_c) copy();
        n.ph = ph.copy();
        return n;
    }
    
    public Node typeCheck(TypeChecker tc) throws SemanticException {
	JifTypeSystem ts = (JifTypeSystem) tc.typeSystem();

	boolean first = true;

	// FIXME: This should be handled in parsing.
	for (Iterator i = cases.iterator(); i.hasNext(); ) {
	    LabelCase n = (LabelCase) i.next();

	    if (n.isDefault()) {
		if (i.hasNext()) {
		    throw new SemanticException(
			"Else case of a switch label must be the last case.");
		}
		else if (first) {
		    throw new SemanticException(
			"Else case of a switch label cannot be the first case.");
		}
	    }

	    if (n.decl() != null) {
		Type t = n.decl().declType();
		Type s = expr.type();

		if (! ts.isImplicitCastValid(s, t) &&
		    ! ts.equals(s, t) &&
		    ! ts.numericConversionValid(t, expr.constantValue())) {

		    throw new SemanticException(
			"Switch label expression of type " + s +
			" cannot be assigned to variable of type " + t + ".",
			n.decl().position());
		}
	    }

	    first = false;
	}

	return this;
    }

    public Term entry() {
        return expr.entry();
    }

    public List acceptCFG(CFGBuilder v, List succs) {
        List expr_succs = new LinkedList();

        for (Iterator i = cases.iterator(); i.hasNext(); ) {
            LabelCase lc = (LabelCase) i.next();
            v.visitCFG(lc, this);
            expr_succs.add(lc.entry());
        }

        v.visitCFG(expr, FlowGraph.EDGE_KEY_OTHER, expr_succs);

        return succs;
    }

//    void eval(CodeWriter w, Translator tr) {
//	JifTypeSystem ts = (JifTypeSystem) tr.typeSystem();
//	JifContext c = (JifContext) tr.context();
//
//	// Evaluate the expression on whose label we're switching, and save the
//	// result in a fresh variable.  We do this even if we don't use the
//	// variable because we must evaluate the expression for its side
//	// effects.
//
//	// Write out the LHS of the declaration of the temporary.
//	Type t = expr.type();
//
//	w.write("final ");
//	w.write(t.translate(c));
//	w.write(" " + UniqueID.newID("t") + " = ");
//
//	// Evaluate the expression.
//	expr.del().translate(w, tr);
//	w.write(";");
//    }
    
    public String toString() {
	return "switch label (" + expr + ") ...";
    }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write("switch label (");
        printBlock(expr, w, tr);
        w.write(") {");
        w.allowBreak(4, " ");
        w.begin(0);

        for (Iterator i = cases.iterator(); i.hasNext();) {
            LabelCase s = (LabelCase) i.next();
            print(s, w, tr);
            if (i.hasNext()) {
                w.newline(0);
            }
        }

        w.end();
        w.newline(0);
        w.write("}");
    }

    public void translate(CodeWriter w, Translator tr) {
        throw new InternalCompilerError("cannot translate " + this);
    }
}
