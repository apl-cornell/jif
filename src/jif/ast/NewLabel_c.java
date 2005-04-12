package jif.ast;

import java.util.List;

import jif.types.JifTypeSystem;
import jif.types.label.Label;
import polyglot.ast.Node;
import polyglot.ast.Term;
import polyglot.ext.jl.ast.Expr_c;
import polyglot.types.SemanticException;
import polyglot.util.CodeWriter;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.*;

/** An implementation of the <code>NewLabel</code> interface. 
 */
public class NewLabel_c extends LabelExpr_c implements NewLabel
{
    public NewLabel_c(Position pos, LabelNode label) {
        super(pos, label);
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
