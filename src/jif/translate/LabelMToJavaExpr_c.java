package jif.translate;

import java.util.Iterator;
import java.util.LinkedList;

import jif.types.label.*;
import jif.types.principal.Principal;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;
import polyglot.util.InternalCompilerError;

public class LabelMToJavaExpr_c extends LabelToJavaExpr_c {
    public Expr toJava(WriterPolicy label, JifToJavaRewriter rw) throws SemanticException {
        Expr e = toIntegPolJava(label, rw);
        return rw.qq().parseExpr("jif.lang.LabelUtil.integCollection(%E)", e);
    }
    public Expr toIntegPolJava(WriterPolicy L, JifToJavaRewriter rw) throws SemanticException {
        Expr owner = rw.principalToJava(L.owner());
        
        Expr set = rw.qq().parseExpr("new jif.lang.PrincipalSet()");
        
        for (Iterator i = L.writers().iterator(); i.hasNext(); ) {
            Principal p = (Principal) i.next();
            Expr pe = rw.principalToJava(p);
            set = rw.qq().parseExpr("(%E).add(%E)", set, pe);
        }
        return rw.qq().parseExpr("jif.lang.LabelUtil.writerPolicyLabel(%E, (%E))", owner, set);
    }

    // returns an expr of type ConfCollection 
    public Expr toJava(MeetLabelM L, JifToJavaRewriter rw) throws SemanticException {
        Expr e = rw.qq().parseExpr("jif.lang.LabelUtil.topInteg()");
        for (Iterator iter = L.components().iterator(); iter.hasNext(); ) {
            WriterPolicy lj = (WriterPolicy)iter.next();
            Expr x = toIntegPolJava(lj, rw);

            e = rw.qq().parseExpr("%E.meet(%E)", e, x);
        }
        return e;
    }
}
