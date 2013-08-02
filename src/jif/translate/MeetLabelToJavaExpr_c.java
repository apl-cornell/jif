package jif.translate;

import java.util.Iterator;
import java.util.LinkedList;

import jif.types.label.Label;
import jif.types.label.MeetLabel;
import polyglot.ast.Expr;
import polyglot.types.ConstructorInstance;
import polyglot.types.SemanticException;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

public class MeetLabelToJavaExpr_c extends LabelToJavaExpr_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Expr toJava(Label label, JifToJavaRewriter rw)
            throws SemanticException {
        MeetLabel L = (MeetLabel) label;

        if (L.meetComponents().size() == 1) {
            return rw.labelToJava(L.meetComponents().iterator().next());
        }

        boolean simplify = true;
        if (rw.context().currentCode() instanceof ConstructorInstance
                && rw.currentClass().isSubtype(rw.jif_ts().PrincipalClass()))
            simplify = false;

        LinkedList<Label> l = new LinkedList<Label>(L.meetComponents());
        Iterator<Label> iter = l.iterator();
        Label head = iter.next();
        Expr e = rw.labelToJava(head);
        while (iter.hasNext()) {
            head = iter.next();
            Expr f = rw.labelToJava(head);
            e =
                    rw.qq().parseExpr(
                            "%E.meet(%E, %E)",
                            e,
                            f,
                            rw.java_nf().BooleanLit(
                                    Position.compilerGenerated(), simplify));
        }
        return e;
    }
}
