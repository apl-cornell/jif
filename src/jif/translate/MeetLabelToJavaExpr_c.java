package jif.translate;

import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.ext.jl.ast.*;
import jif.ast.*;
import polyglot.ext.jl.types.*;
import jif.types.*;
import jif.types.label.Label;
import jif.types.label.MeetLabel;
import polyglot.visit.*;
import java.util.*;

public class MeetLabelToJavaExpr_c extends LabelToJavaExpr_c {
    public Expr toJava(Label label, JifToJavaRewriter rw) throws SemanticException {
        MeetLabel L = (MeetLabel) label;

        if (L.components().size() == 1) {
            LinkedList l = new LinkedList(L.components());
            Label head = (Label) l.removeFirst(); 
            return rw.labelToJava(head);
        }

        if (L.components().isEmpty()) {
            return rw.qq().parseExpr("jif.lang.Label.top()");
        }
        else {
            LinkedList l = new LinkedList(L.components());
            Label head = (Label) l.removeFirst(); 
            Label tail = rw.jif_ts().meetLabel(L.position(), l);

            Expr x = rw.labelToJava(head);
            Expr y = rw.labelToJava(tail);

            return rw.qq().parseExpr("%E.meet(%E)", x, y);
        }
    }
}
