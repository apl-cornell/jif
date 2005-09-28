package jif.translate;

import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.ext.jl.ast.*;
import jif.ast.*;
import polyglot.ext.jl.types.*;
import jif.types.*;
import jif.types.label.JoinLabel;
import jif.types.label.Label;
import polyglot.visit.*;
import java.util.*;

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
