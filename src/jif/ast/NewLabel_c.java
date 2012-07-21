package jif.ast;

import polyglot.util.CodeWriter;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.Translator;

/** An implementation of the <code>NewLabel</code> interface.
 */
public class NewLabel_c extends LabelExpr_c implements NewLabel
{
    public NewLabel_c(Position pos, LabelNode label) {
        super(pos, label);
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write("new label {");
        print(label, w, tr);
        w.write("}");
    }

    @Override
    public void translate(CodeWriter w, Translator tr) {
        throw new InternalCompilerError("cannot translate " + this);
    }

    @Override
    public String toString() {
        return "new label " + label;
    }
}
