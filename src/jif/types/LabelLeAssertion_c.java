package jif.types;

import jif.translate.JifToJavaRewriter;
import jif.translate.LabelLeAssertionToJavaExpr;
import jif.types.label.Label;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;
import polyglot.types.TypeObject_c;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

public class LabelLeAssertion_c extends TypeObject_c
        implements LabelLeAssertion {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected LabelLeAssertionToJavaExpr toJava;
    protected Label lhs;
    protected Label rhs;

    public LabelLeAssertion_c(JifTypeSystem ts, Label lhs, Label rhs,
            Position pos, LabelLeAssertionToJavaExpr toJava) {
        super(ts, pos);
        this.lhs = lhs;
        this.rhs = rhs;
        this.toJava = toJava;
    }

    @Override
    public Label lhs() {
        return lhs;
    }

    @Override
    public Label rhs() {
        return rhs;
    }

    @Override
    public LabelLeAssertion lhs(Label lhs) {
        LabelLeAssertion_c n = (LabelLeAssertion_c) copy();
        n.lhs = lhs;
        return n;
    }

    @Override
    public LabelLeAssertion rhs(Label rhs) {
        LabelLeAssertion_c n = (LabelLeAssertion_c) copy();
        n.rhs = rhs;
        return n;
    }

    @Override
    public boolean isCanonical() {
        return true;
    }

    @Override
    public String toString() {
        return lhs + " assert<= " + rhs;
    }

    @Override
    public Expr toJava(JifToJavaRewriter rw) throws SemanticException {
        return toJava.toJava(this, rw);
    }
}
