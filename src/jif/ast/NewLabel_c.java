package jif.ast;

import polyglot.ext.jl.ast.*;
import jif.types.*;
import jif.types.label.Label;
import jif.visit.*;
import polyglot.ast.*;
import polyglot.types.*;
import polyglot.visit.*;
import polyglot.util.*;
import java.util.*;

/** An implementation of the <code>NewLabel</code> interface. 
 */
public class NewLabel_c extends Expr_c implements NewLabel
{
    protected LabelNode label;

    public NewLabel_c(Position pos, LabelNode label) {
	super(pos);
	this.label = label;
    }

    public LabelNode label() {
	return this.label;
    }

    public NewLabel label(LabelNode label) {
	NewLabel_c n = (NewLabel_c) copy();
	n.label = label;
	return n;
    }
    
    protected NewLabel_c reconstruct(LabelNode label) {
	if (label != this.label) {
	    NewLabel_c n = (NewLabel_c) copy();
	    n.label = label;
	    return n;
	}

	return this;
    }

    public Node visitChildren(NodeVisitor v) {
	LabelNode label = (LabelNode) visitChild(this.label, v);
	return reconstruct(label);
    }

    public Node typeCheck(TypeChecker tc) throws SemanticException {
	JifTypeSystem ts = (JifTypeSystem) tc.typeSystem();
	Label l = label.label();	
	if (!l.isRuntimeRepresentable()) {
	    throw new SemanticException("new label expression must contain a run-time " +
					"representable label.", position());
	}
	
	return type(ts.Label());
    }

    public Term entry() {
        return this;
    }

    public List acceptCFG(CFGBuilder v, List succs) {
        return succs;
    }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write("new label {");
        print(label, w, tr);
        w.write("}");
    }

    public void translate(CodeWriter w, Translator tr) {
        throw new InternalCompilerError("cannot translate " + this);
    }

    public String toString() {
	return "new label " + label;
    }
}
