package jif.translate;

import java.util.Iterator;
import java.util.LinkedList;

import jif.types.label.JoinLabel;
import jif.types.label.Label;
import polyglot.ast.Expr;
import polyglot.types.ConstructorInstance;
import polyglot.types.SemanticException;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

public class JoinLabelToJavaExpr_c extends LabelToJavaExpr_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Expr toJava(Label label, JifToJavaRewriter rw, Expr qualifier)
            throws SemanticException {
        JoinLabel L = (JoinLabel) label;

        if (L.joinComponents().size() == 1) {
            return rw.labelToJava(L.joinComponents().iterator().next(),
                    qualifier);
        }
        boolean simplify = true;
        if (rw.context().currentCode() instanceof ConstructorInstance
                && rw.currentClass().isSubtype(rw.jif_ts().PrincipalClass()))
            simplify = false;

        LinkedList<Label> l = new LinkedList<Label>(L.joinComponents());
        Iterator<Label> iter = l.iterator();
        Label head = iter.next();
        Expr e = rw.labelToJava(head, qualifier);
        while (iter.hasNext()) {
            head = iter.next();
            Expr f = rw.labelToJava(head, qualifier);
            e = rw.qq().parseExpr("%E.join(%E, %E)", e, f, rw.java_nf()
                    .BooleanLit(Position.compilerGenerated(), simplify));
        }
        return e;
    }
}
