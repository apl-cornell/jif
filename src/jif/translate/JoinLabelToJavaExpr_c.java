package jif.translate;

import java.util.Iterator;
import java.util.LinkedList;

import jif.types.label.JoinLabel;
import jif.types.label.Label;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;

public class JoinLabelToJavaExpr_c extends LabelToJavaExpr_c {
    @Override
    public Expr toJava(Label label, JifToJavaRewriter rw) throws SemanticException {
        JoinLabel L = (JoinLabel) label;

        if (L.joinComponents().size() == 1) {
            return rw.labelToJava(L.joinComponents().iterator().next());
        }

        LinkedList<Label> l = new LinkedList<Label>(L.joinComponents());
        Iterator<Label> iter = l.iterator();
        Label head = iter.next();
        Expr e = rw.labelToJava(head);
        while (iter.hasNext()) {
            head = iter.next();
            Expr f = rw.labelToJava(head);
            e = rw.qq().parseExpr("%E.join(%E)", e, f);
        }
        return e;
    }
}
