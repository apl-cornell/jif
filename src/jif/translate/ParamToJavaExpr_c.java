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
import polyglot.util.SerialVersionUID;

public class ParamToJavaExpr_c implements LabelToJavaExpr, PrincipalToJavaExpr {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Expr toJava(Label label, JifToJavaRewriter rw, Expr thisQualifier)
            throws SemanticException {
        return toJava(label, rw, thisQualifier, true);
    }

    @Override
    public Expr toJava(Label label, JifToJavaRewriter rw, Expr thisQualifier,
            boolean simplify) throws SemanticException {
        if (label instanceof ParamLabel) {
            return toJava(((ParamLabel) label).paramInstance(), rw,
                    thisQualifier);
        }
        return toJava(((CovariantParamLabel) label).paramInstance(), rw,
                thisQualifier);
    }

    @Override
    public Expr toJava(Principal principal, JifToJavaRewriter rw,
            Expr thisQualifier) throws SemanticException {
        return toJava(((ParamPrincipal) principal).paramInstance(), rw,
                thisQualifier);
    }

    public Expr toJava(ParamInstance pi, JifToJavaRewriter rw,
            Expr thisQualifier) {
        if (rw.jif_ts().isSignature(pi.container())) {
            // the parameter to be translated is in the code
            // of a non-Jif class (which does have runtime representation
            // of params).
            // This code is not used at runtime, and we do not
            // require the Java code with Jif signatures to have
            // a standard name for parameters, so just return a placeholder.
            return rw.qq().parseExpr("null");
        }
        JifContext A = (JifContext) rw.context();
        if (A.inStaticContext() && !rw.inConstructor()) {
            // We are in a static context, so we do not have
            // the field instances available. Instead, the
            // params will have been given to us as method
            // arguments.
            // (note that if we are translating a constructor
            // and are in a static context, then we must
            // be in a constructor call (i.e., this(...) or
            // super(...)), and we do in fact have the
            // fields available to us. Hence the "!rw.inConstructor()")
            return rw.qq().parseExpr(paramArgName(pi));
        } else {
            return rw.qq().parseExpr("%E.%s", thisQualifier,
                    paramFieldName(pi));
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
