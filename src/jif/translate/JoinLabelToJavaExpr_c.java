package jif.translate;

import java.util.LinkedList;

import jif.types.label.*;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;

public class JoinLabelToJavaExpr_c extends LabelToJavaExpr_c {
    public Expr toJava(Label label, JifToJavaRewriter rw) throws SemanticException {
        JoinLabel L = (JoinLabel) label;

        if (L.components().size() == 1) {
            LinkedList l = new LinkedList(L.components());
            Label head = (Label) l.removeFirst(); 
            return rw.labelToJava(head);
        }

        if (L.components().isEmpty()) {
            return rw.qq().parseExpr("jif.lang.LabelUtil.bottom()");
        }
        else {
            LinkedList l = new LinkedList(L.components());
            Label head = (Label) l.removeFirst(); 
            Label tail = rw.jif_ts().joinLabel(L.position(), l);

            Expr x = rw.labelToJava(head);
            Expr y = rw.labelToJava(tail);

            return rw.qq().parseExpr("%E.join(%E)", x, y);
        }
    }
}
