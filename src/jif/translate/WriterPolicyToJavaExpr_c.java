package jif.translate;

import jif.types.label.Label;
import jif.types.label.WriterPolicy;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;

public class WriterPolicyToJavaExpr_c extends LabelToJavaExpr_c {
    public Expr toJava(Label label, JifToJavaRewriter rw) throws SemanticException {
        WriterPolicy policy = (WriterPolicy)label;
        Expr owner = rw.principalToJava(policy.owner());
        Expr writer = rw.principalToJava(policy.writer());
        
        return rw.qq().parseExpr("jif.lang.LabelUtil.writerPolicyLabel(%E, %E)", owner, writer);
    }

}
