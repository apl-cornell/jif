package jif.translate;

import java.util.Iterator;

import jif.types.label.*;
import jif.types.principal.Principal;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;

public class PairLabelToJavaExpr_c extends LabelToJavaExpr_c {
    public Expr toJava(Label label, JifToJavaRewriter rw) throws SemanticException {
        PairLabel L = (PairLabel) label;
        Expr confExp = L.labelJ().toJava(rw);
        Expr integExp = L.labelM().toJava(rw);
        return rw.qq().parseExpr("jif.lang.LabelUtil.pairLabel(%E, %E)", 
                                 confExp, integExp);
    }
}
