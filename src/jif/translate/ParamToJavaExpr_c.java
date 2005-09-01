package jif.translate;

import jif.types.JifClassType;
import jif.types.JifContext;
import jif.types.ParamInstance;
import jif.types.label.CovariantParamLabel;
import jif.types.label.Label;
import jif.types.label.ParamLabel;
import jif.types.principal.ParamPrincipal;
import jif.types.principal.Principal;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;

public class ParamToJavaExpr_c implements LabelToJavaExpr, PrincipalToJavaExpr {
    public Expr toJava(Label label, JifToJavaRewriter rw) throws SemanticException {
        if (label instanceof ParamLabel) {
            return toJava(((ParamLabel)label).paramInstance(), rw);
        }
        return toJava(((CovariantParamLabel)label).paramInstance(), rw);
    }

    public Expr toJava(Principal principal, JifToJavaRewriter rw) throws SemanticException {
        return toJava(((ParamPrincipal)principal).paramInstance(), rw);
    }

    public Expr toJava(ParamInstance pi, JifToJavaRewriter rw) throws SemanticException {
        JifContext A = (JifContext)rw.context();
        if (A.inStaticContext()) {
            return rw.qq().parseExpr(paramArgName(pi));            
        }
        else {
            return rw.qq().parseExpr("this." + paramFieldName(pi));            
        }        
    }
    
    public static String paramFieldName(ParamInstance pi) {
        JifClassType jct = pi.container();
        String fullName = jct.fullName().replace('.', '_');
        return "jif$" + fullName + "_" + pi.name();
    }
    public static String paramFieldNameGetter(ParamInstance pi) {
        JifClassType jct = pi.container();
        String fullName = jct.fullName().replace('.', '_');
        return "jif$get" + fullName + "_" + pi.name();
    }
    public static String paramArgName(ParamInstance pi) {
        return "jif$" + pi.name();
    }

}
