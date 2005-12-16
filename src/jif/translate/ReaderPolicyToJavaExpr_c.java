package jif.translate;

import jif.types.label.Label;
import jif.types.label.ReaderPolicy;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;

public class ReaderPolicyToJavaExpr_c extends LabelToJavaExpr_c {
    public Expr toJava(Label label, JifToJavaRewriter rw) throws SemanticException {
        ReaderPolicy policy = (ReaderPolicy)label;
        Expr owner = rw.principalToJava(policy.owner());
        Expr writer = rw.principalToJava(policy.reader());
        
        return rw.qq().parseExpr("jif.lang.LabelUtil.readerPolicyLabel(%E, %E)", owner, writer);
    }

}
