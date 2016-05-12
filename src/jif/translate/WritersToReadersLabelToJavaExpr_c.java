package jif.translate;

import jif.types.label.Label;
import jif.types.label.WritersToReadersLabel;

import polyglot.ast.Expr;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class WritersToReadersLabelToJavaExpr_c extends LabelToJavaExpr_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Expr toJava(Label label, JifToJavaRewriter rw)
            throws SemanticException {
        WritersToReadersLabel L = (WritersToReadersLabel) label;

        return rw.qq().parseExpr("%E.writersToReaders()", rw.labelToJava(L.label()));
    }
}
