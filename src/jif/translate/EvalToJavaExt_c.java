package jif.translate;

import polyglot.ast.Eval;
import polyglot.ast.Node;
import polyglot.types.SemanticException;

public class EvalToJavaExt_c extends ToJavaExt_c {
    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        Eval n = (Eval)node();
        return rw.java_nf().Eval(n.position(), n.expr());
    }
}
