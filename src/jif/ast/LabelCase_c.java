package jif.ast;

import polyglot.ext.jl.ast.*;
import jif.types.*;
import jif.visit.*;
import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.visit.*;
import java.util.*;

/** An implementation of the <code>LabelCase</code> interface. 
 */
public class LabelCase_c extends Stmt_c implements LabelCase
{
    protected Formal decl;
    protected LabelNode label;
    protected Stmt body;

    public LabelCase_c(Position pos, Formal decl, LabelNode label, Stmt body) {
	super(pos);
	this.decl = decl;
	this.label = label;
	this.body = body;

	if (label == null && decl != null) {
	    throw new InternalCompilerError("The \"else\" label case cannot have a declaration.");
	}
    }

    public Formal decl() {
	return this.decl;
    }

    public LabelCase decl(Formal decl) {
	LabelCase_c n = (LabelCase_c) copy();
	n.decl = decl;
	return n;
    }

    public LabelNode label() {
	return this.label;
    }

    public LabelCase label(LabelNode label) {
	LabelCase_c n = (LabelCase_c) copy();
	n.label = label;
	return n;
    }

    public Stmt body() {
	return this.body;
    }

    public LabelCase body(Stmt body) {
	LabelCase_c n = (LabelCase_c) copy();
	n.body = body;
	return n;
    }

    protected LabelCase_c reconstruct(Formal decl, LabelNode label, Stmt body) {
	if (decl != this.decl || label != this.label || body != this.body) {
	    LabelCase_c n = (LabelCase_c) copy();
	    n.decl = decl;
	    n.label = label;
	    n.body = body;
	    return n;
	}

	return this;
    }

    public Node visitChildren(NodeVisitor v) {
	Formal decl = (Formal) visitChild(this.decl, v);
	LabelNode label = (LabelNode) visitChild(this.label, v);
	Stmt body = (Stmt) visitChild(this.body, v);
	return reconstruct(decl, label, body);
    }

    public boolean isDefault() {
	return label == null;
    }

    public Context enterScope(Context c) {
	return c.pushBlock();
    }

    public String toString() {
	if (label == null) {
	    return "else " + body;
	}
	else {
	    return "case (" + label + ") " + body;
	}
    }

    public Term entry() {
        if (decl != null) {
            return decl.entry();
        }

        return body.entry();
    }

    public List acceptCFG(CFGBuilder v, List succs) {
        if (decl != null) {
            v.visitCFG(decl, body.entry());
        }

        v.visitCFG(body, this);

        return succs;
    }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
	if (label == null) {
            w.write("else ");
	}
	else {
            w.write("case (");

            if (decl != null) {
                print(decl, w, tr);
            }
            else {
                w.write("{");
                print(label, w, tr);
                w.write("}");
            }

            w.write(")");
        }

        printSubStmt(body, w, tr);
    }

    public void translate(CodeWriter w, Translator tr) {
        throw new InternalCompilerError("cannot translate " + this);
    }
}
