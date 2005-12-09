package jif.translate;

import jif.types.JifClassType;
import jif.types.JifContext;
import jif.types.ParamInstance;
import jif.types.label.*;
import jif.types.principal.ParamPrincipal;
import jif.types.principal.Principal;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;
import polyglot.util.InternalCompilerError;

public class ParamToJavaExpr_c implements LabelToJavaExpr, PrincipalToJavaExpr {
    public Expr toJava(Label label, JifToJavaRewriter rw) throws SemanticException {
        if (label instanceof ParamLabel) {
            return toJava(((ParamLabel)label).paramInstance(), rw);
        }
        return toJava(((CovariantParamLabel)label).paramInstance(), rw);
    }
    public Expr toJava(LabelJ label, JifToJavaRewriter rw) throws SemanticException {
        throw new InternalCompilerError("Should never be called");
    }
    public Expr toJava(LabelM label, JifToJavaRewriter rw) throws SemanticException {
        throw new InternalCompilerError("Should never be called");
    }

    public Expr toJava(Principal principal, JifToJavaRewriter rw) throws SemanticException {
        return toJava(((ParamPrincipal)principal).paramInstance(), rw);
    }

    public Expr toJava(ParamInstance pi, JifToJavaRewriter rw) throws SemanticException {
        if (!rw.jif_ts().isJifClass(pi.container())) {
            // the parameter to be translated is in the code
            // of a non-Jif class (which does have runtime representation
            // of params).
            // This code is not used at runtime, and we do not
            // require the Java code with Jif signatures to have
            // a standard name for parameters, so just return a placeholder.
            return rw.qq().parseExpr("null");
        }
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
