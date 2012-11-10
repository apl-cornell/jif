package jif.translate;

import java.util.Iterator;
import java.util.LinkedList;

import jif.types.label.Label;
import jif.types.label.MeetLabel;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;
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

        LinkedList<Label> l = new LinkedList<Label>(L.meetComponents());
        Iterator<Label> iter = l.iterator();
        Label head = iter.next();
        Expr e = rw.labelToJava(head);
        while (iter.hasNext()) {
            head = iter.next();
            Expr f = rw.labelToJava(head);
            e = rw.qq().parseExpr("%E.meet(%E)", e, f);
        }
        return e;
    }
}
