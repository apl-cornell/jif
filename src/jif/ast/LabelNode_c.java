package jif.ast;

import jif.types.Param;
import jif.types.label.Label;
import polyglot.ast.Node_c;
import polyglot.util.CodeWriter;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.Translator;

/** An implementation of the <code>LabelNode</code> interface. 
 */
public abstract class LabelNode_c extends Node_c implements LabelNode
{
    private Label label;

    public LabelNode_c(Position pos) {
        super(pos);
    }

    protected LabelNode_c(Position pos, Label label) {
        super(pos);
        this.label = label;
    }

    public boolean isDisambiguated() {
        return label != null && label.isCanonical() && super.isDisambiguated();
    }

    public Label label() {
	return label;
    }

    public LabelNode label(Label label) {
	LabelNode_c n = (LabelNode_c) copy();
	n.label = label;
	return n;
    }

    public Param parameter() {
	return label();
    }

    public String toString() {
	if (label != null) {
	    return label.toString();
	}
	else {
	    return "<unknown-label>";
	}
    }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write(this.toString());
    }
    
    public final void translate(CodeWriter w, Translator tr) {
        throw new InternalCompilerError("cannot translate " + this);
    }
    
}
