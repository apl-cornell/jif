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

/** An implementation of the <tt>ActsFor</tt> interface. */
public class ActsFor_c extends Stmt_c implements ActsFor
{
    protected PrincipalNode actor;
    protected PrincipalNode granter;
    protected Stmt consequent;
    protected Stmt alternative;

    public ActsFor_c(Position pos, PrincipalNode actor, PrincipalNode granter, Stmt consequent, Stmt alternative) {
	super(pos);
	this.actor = actor;
	this.granter = granter;
	this.consequent = consequent;
	this.alternative = alternative;
    }

    /** Gets the actor principal. */
    public PrincipalNode actor() {
	return this.actor;
    }

    /** Sets the actor principal. */
    public ActsFor actor(PrincipalNode actor) {
	ActsFor_c n = (ActsFor_c) copy();
	n.actor = actor;
	return n;
    }

    /** Gets the granter principal. */
    public PrincipalNode granter() {
	return this.granter;
    }

    /** Sets the granter principal. */
    public ActsFor granter(PrincipalNode granter) {
	ActsFor_c n = (ActsFor_c) copy();
	n.granter = granter;
	return n;
    }

    /** Gets the consequent statement. */
    public Stmt consequent() {
	return this.consequent;
    }

    /** Sets the consequent statement. */
    public ActsFor consequent(Stmt consequent) {
	ActsFor_c n = (ActsFor_c) copy();
	n.consequent = consequent;
	return n;
    }

    /** Gets the alternative statement. */
    public Stmt alternative() {
	return this.alternative;
    }

    /** Sets the alternative statement. */
    public ActsFor alternative(Stmt alternative) {
	ActsFor_c n = (ActsFor_c) copy();
	n.alternative = alternative;
	return n;
    }

    /** Reconstructs the node. */
    protected ActsFor_c reconstruct(PrincipalNode actor, PrincipalNode granter, Stmt consequent, Stmt alternative) {
	if (actor != this.actor || granter != this.granter || consequent != this.consequent || alternative != this.alternative) {
	    ActsFor_c n = (ActsFor_c) copy();
	    n.actor = actor;
	    n.granter = granter;
	    n.consequent = consequent;
	    n.alternative = alternative;
	    return n;
	}

	return this;
    }

    /** Visits the children of the node. */
    public Node visitChildren(NodeVisitor v) {
	PrincipalNode actor = (PrincipalNode) visitChild(this.actor, v);
	PrincipalNode granter = (PrincipalNode) visitChild(this.granter, v);
	Stmt consequent = (Stmt) visitChild(this.consequent, v);
	Stmt alternative = (Stmt) visitChild(this.alternative, v);
	return reconstruct(actor, granter, consequent, alternative);
    }

    /** Type check the expression. */
    public Node typeCheck(TypeChecker tc) throws SemanticException {
	JifTypeSystem ts = (JifTypeSystem) tc.typeSystem();

	if (actor.principal() instanceof ParamPrincipal) {
	    throw new SemanticException(
		"A principal parameter cannot be inspected at run time.",
		actor.position());
	}

	if (granter.principal() instanceof ParamPrincipal) {
	    throw new SemanticException(
		"A principal parameter cannot be inspected at run time.",
		granter.position());
	}

	return this;
    }

    public Term entry() {
        return actor.entry();
    }

    public List acceptCFG(CFGBuilder v, List succs) {
        v.visitCFG(actor, granter.entry());

        if (alternative == null) {
            v.visitCFG(granter, FlowGraph.EDGE_KEY_TRUE, consequent.entry(), 
                                FlowGraph.EDGE_KEY_FALSE, this);
            v.visitCFG(consequent, this);
        }
        else {
            v.visitCFG(granter, FlowGraph.EDGE_KEY_TRUE, consequent.entry(),
                                FlowGraph.EDGE_KEY_FALSE, alternative.entry());
            v.visitCFG(consequent, this);
            v.visitCFG(alternative, this);
        }

        return succs;
    }
    
    public String toString() {
	return "actsFor (" + actor + ", " + granter + ") ...";
    }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write("actsFor(");
        print(actor, w, tr);
        w.write(", ");
        print(granter, w, tr);
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
