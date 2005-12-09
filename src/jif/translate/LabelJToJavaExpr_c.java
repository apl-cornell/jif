package jif.translate;

import java.util.Iterator;
import java.util.LinkedList;

import jif.types.label.*;
import jif.types.principal.Principal;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;
import polyglot.util.InternalCompilerError;

public class LabelJToJavaExpr_c extends LabelToJavaExpr_c {
    public Expr toJava(ReaderPolicy label, JifToJavaRewriter rw) throws SemanticException {
        Expr e = toConfPolJava(label, rw);
        return rw.qq().parseExpr("jif.lang.LabelUtil.confCollection(%E)", e);
    }
    public Expr toConfPolJava(ReaderPolicy L, JifToJavaRewriter rw) throws SemanticException {        
        Expr owner = rw.principalToJava(L.owner());
        
        Expr set = rw.qq().parseExpr("new jif.lang.PrincipalSet()");
        
        for (Iterator i = L.readers().iterator(); i.hasNext(); ) {
            Principal p = (Principal) i.next();
            Expr pe = rw.principalToJava(p);
            set = rw.qq().parseExpr("(%E).add(%E)", set, pe);
        }
        return rw.qq().parseExpr("jif.lang.LabelUtil.readerPolicyLabel(%E, (%E))", owner, set);
    }

    // returns an expr of type ConfCollection 
    public Expr toJava(JoinLabelJ L, JifToJavaRewriter rw) throws SemanticException {
        Expr e = rw.qq().parseExpr("jif.lang.LabelUtil.bottomConf()");
        for (Iterator iter = L.components().iterator(); iter.hasNext(); ) {
            ReaderPolicy lj = (ReaderPolicy)iter.next();
            Expr x = toConfPolJava(lj, rw);

            e = rw.qq().parseExpr("%E.join(%E)", e, x);
        }
        return e;
    }
}
