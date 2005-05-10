package jif.translate;

import java.util.*;

import jif.ast.JifClassDecl;
import jif.types.*;
import jif.types.label.Label;
import jif.types.principal.Principal;
import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.Position;
import polyglot.visit.NodeVisitor;

public class ClassDeclToJavaExt_c extends ToJavaExt_c {
    static final String INSTANCEOF_METHOD_NAME = "jif$Instanceof";
    static final String castMethodName(ClassType ct) {
        return "jif$cast$"+ct.fullName().replace('.','_');
    }
    static final String interfaceClassImplName(String jifInterfaceName) {
        return jifInterfaceName + "_JIF_IMPL";
    }
    public NodeVisitor toJavaEnter(JifToJavaRewriter rw) throws SemanticException {
        // Bypass params and authority.
        JifClassDecl n = (JifClassDecl) node();

        rw.enteringClass(n.type());

        return rw.bypass(n.params()).bypass(n.authority());
    }

    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        JifClassDecl n = (JifClassDecl) node();
        JifPolyType jpt = (JifPolyType)n.type();

        ClassBody cb = n.body();
        if (rw.jif_ts().isJifClass(jpt)) {
            if (!jpt.flags().isInterface()) {
                // add constructor
                cb = cb.addMember(produceConstructor(jpt, rw));

                if (!jpt.params().isEmpty()) {
                    // add instanceof and cast static methods to the class
                    cb = cb.addMember(produceInstanceOfMethod(jpt, rw, false));
                    cb = cb.addMember(produceCastMethod(jpt, rw));

                    // add fields for params
                    for (Iterator iter = jpt.params().iterator(); iter.hasNext(); ) {
                        ParamInstance pi = (ParamInstance)iter.next();
                        String paramFieldName = ParamToJavaExpr_c.paramFieldName(pi);
                        TypeNode tn = typeNodeForParam(pi, rw);
                        cb = cb.addMember(rw.qq().parseMember("private final %T %s;", tn, paramFieldName));
                    }
                }

                // add getter methods for any params declared in interfaces
                cb = addInterfaceParamGetters(cb, jpt, jpt, rw);
            }
            else {
                ClassBody implBody = rw.java_nf().ClassBody(Position.COMPILER_GENERATED, new ArrayList(2));
                implBody = implBody.addMember(produceInstanceOfMethod(jpt, rw, true));
                implBody = implBody.addMember(produceCastMethod(jpt, rw));

                ClassDecl implDecl = rw.java_nf().ClassDecl(Position.COMPILER_GENERATED,
                                                            n.flags().clearInterface().Abstract(),
                                                            interfaceClassImplName(n.name()),
                                                            null,
                                                            Collections.EMPTY_LIST,
                                                            implBody);
                rw.addAdditionalClassDecl(implDecl);

                // add getters for params to the interface
                for (Iterator iter = jpt.params().iterator(); iter.hasNext(); ) {
                    ParamInstance pi = (ParamInstance)iter.next();
                    String paramFieldNameGetter = ParamToJavaExpr_c.paramFieldNameGetter(pi);
                    TypeNode tn = typeNodeForParam(pi, rw);
                    cb = cb.addMember(rw.qq().parseMember("%T %s();", tn,paramFieldNameGetter));
                }


            }
        }


        rw.leavingClass();
        return rw.java_nf().ClassDecl(n.position(), n.flags(), n.name(),
                                      n.superClass(), n.interfaces(), cb);
    }

    /**
     * Go through the interfaces, and add any required fields and getters for the fields.
     * @param cb
     * @param jpt
     * @param rw
     * @throws SemanticException
     */
    private static ClassBody addInterfaceParamGetters(ClassBody cb, JifPolyType baseClass, JifPolyType jpt, JifToJavaRewriter rw) throws SemanticException {
        // go through the interfaces of cb
        for (Iterator iter = jpt.interfaces().iterator(); iter.hasNext(); ) {
            Type interf = (Type)iter.next();
            if (rw.jif_ts().isJifClass(interf) &&
                    !rw.jif_ts().isSubtype(baseClass.superType(), interf) &&
                    interf instanceof JifSubstType) {
                // the interface is not implemented in a super class,
                // so add fields and params.
                JifSubstType interfST = (JifSubstType)interf;
                JifSubst subst = (JifSubst)interfST.subst();
                JifPolyType interfPT = (JifPolyType)interfST.base();
                for (Iterator iter2 = interfPT.params().iterator(); iter2.hasNext(); ) {
                    ParamInstance pi = (ParamInstance)iter2.next();
                    String paramFieldName = ParamToJavaExpr_c.paramFieldName(pi);
                    String paramFieldNameGetter = ParamToJavaExpr_c.paramFieldNameGetter(pi);
                    TypeNode tn = typeNodeForParam(pi, rw);
                    Expr lblExpr = rw.paramToJava(subst.get(pi));
                    cb = cb.addMember(rw.qq().parseMember("private %T %s;", tn, paramFieldName));
                    cb = cb.addMember(rw.qq().parseMember(
                                         "public final %T %s() { "
                                                 + " if (this.%s==null) this.%s = %E; "
                                                 + "return this.%s; }",
                                         tn, paramFieldNameGetter, paramFieldName,
                                         paramFieldName, lblExpr, paramFieldName));

                }

                cb = addInterfaceParamGetters(cb, baseClass, interfPT, rw);

            }
        }
        return cb;

    }
    private static ClassMember produceInstanceOfMethod(JifPolyType jpt, JifToJavaRewriter rw, boolean useGetters) throws SemanticException {
        Context A = rw.context();
        rw = (JifToJavaRewriter)rw.context(A.pushStatic());

        String name = jpt.name();
        StringBuffer sb = new StringBuffer();
        sb.append("static public boolean %s(%LF) {");
        sb.append("if (o instanceof %s) { ");
        sb.append("%s c = (%s)o; ");

        // now test each of the params
        boolean moreThanOneParam = (jpt.params().size() > 1);
        sb.append(moreThanOneParam?"boolean ok = true;":"");
        for (Iterator iter = jpt.params().iterator(); iter.hasNext(); ) {
            ParamInstance pi = (ParamInstance)iter.next();
            String paramFieldName = ParamToJavaExpr_c.paramFieldName(pi);
            String paramArgName = ParamToJavaExpr_c.paramArgName(pi);
            String comparison = "equivalentTo";
            if (pi.isCovariantLabel()) {
                comparison = "relabelsTo";
            }

            sb.append(moreThanOneParam?"ok = ok && ":"return ");

            String paramExpr = paramFieldName;
            if (useGetters) {
                paramExpr = ParamToJavaExpr_c.paramFieldNameGetter(pi) + "()";
            }
            if (pi.isPrincipal()) {  
                // e.g., PrincipalUtil.equivTo(c.expr, paramArgName)
                sb.append("jif.lang.PrincipalUtil."+comparison+
                                     "(c."+paramExpr+","+paramArgName+");");
            }
            else {
                // e.g., c.equivTo(paramArgName)
                sb.append("c."+paramExpr+"."+comparison+"("+paramArgName+");");
            }
        }

        sb.append(moreThanOneParam?"return ok; }":"}");
        sb.append("return false;");

        sb.append("}");

        List formals = produceParamFormals(jpt, rw, true);
        return rw.qq().parseMember(sb.toString(), INSTANCEOF_METHOD_NAME, formals, name, name, name);
    }

    private static TypeNode typeNodeForParam(ParamInstance pi, JifToJavaRewriter rw) throws SemanticException {
        Type paramType = pi.isPrincipal() ? rw.jif_ts().Principal() : rw.jif_ts().Label();
        return CanonicalTypeNodeToJavaExt_c.translate(rw, paramType, Position.COMPILER_GENERATED);
    }
    private static ClassMember produceCastMethod(JifPolyType jpt, JifToJavaRewriter rw) throws SemanticException {
        Context A = rw.context();
        rw = (JifToJavaRewriter)rw.context(A.pushStatic());

        StringBuffer sb = new StringBuffer();
        sb.append("static public %T %s(%LF) {");
        sb.append("if (%s(%LE)) return (%T)o;");
        sb.append("throw new ClassCastException();");
        sb.append("}");

        List formals = produceParamFormals(jpt, rw, true);
        List args = produceParamArgs(jpt, rw);
        TypeNode tn = CanonicalTypeNodeToJavaExt_c.translate(rw, jpt, Position.COMPILER_GENERATED);;
        return rw.qq().parseMember(sb.toString(), tn, castMethodName(jpt), formals, INSTANCEOF_METHOD_NAME, args, tn);
    }

    static List produceParamFormals(JifPolyType jpt, JifToJavaRewriter rw, boolean addObjectFormal) throws SemanticException {
        List formals = new ArrayList(jpt.params().size() + 1);
        Position pos = Position.COMPILER_GENERATED;
        for (Iterator iter = jpt.params().iterator(); iter.hasNext(); ) {
            ParamInstance pi = (ParamInstance)iter.next();
            String paramArgName = ParamToJavaExpr_c.paramArgName(pi);
            TypeNode tn = typeNodeForParam(pi, rw);
            Formal f = rw.java_nf().Formal(pos, Flags.FINAL, tn, paramArgName);
            formals.add(f);
        }

        if (addObjectFormal) {
            // add the object argument too.
            TypeNode tn = rw.qq().parseType("Object");
            formals.add(rw.java_nf().Formal(pos, Flags.FINAL, tn, "o"));
        }
        return formals;
    }

    private static List produceParamArgs(JifPolyType jpt, JifToJavaRewriter rw) {
        List args = new ArrayList(jpt.params().size() + 1);
        for (Iterator iter = jpt.params().iterator(); iter.hasNext(); ) {
            ParamInstance pi = (ParamInstance)iter.next();
            String paramArgName = ParamToJavaExpr_c.paramArgName(pi);
            args.add(rw.qq().parseExpr(paramArgName));
        }

        // add the object argument too.
        args.add(rw.qq().parseExpr("o"));
        return args;
    }

    private static ClassMember produceConstructor(JifPolyType jpt, JifToJavaRewriter rw) throws SemanticException {
        // add arguments for params.
        List formals = produceParamFormals(jpt, rw, false);

        List inits = new ArrayList();

        // add super call.
        List superArgs = new ArrayList();
        Type superType = jpt.superType();
        if (superType instanceof JifSubstType && rw.jif_ts().isJifClass(((JifSubstType)superType).base())) {
            JifSubstType superjst = (JifSubstType)jpt.superType();
            JifPolyType superjpt = (JifPolyType)superjst.base();

            JifContext A = (JifContext)rw.context();
            JifToJavaRewriter rwCons = (JifToJavaRewriter)rw.context(A.pushConstructorCall());
            for (Iterator iter = superjpt.params().iterator(); iter.hasNext(); ) {
                ParamInstance pi = (ParamInstance)iter.next();
                Param param = ((JifSubst)superjst.subst()).get(pi);
                if (pi.isLabel()) {
                    superArgs.add(((Label)param).toJava(rwCons));
                }
                else {
                    superArgs.add(((Principal)param).toJava(rwCons));
                }
            }
        }
        inits.add(rw.qq().parseStmt("super(%LE);", (Object)superArgs));

        // create initializers for the fields from the arguments
        for (Iterator iter = jpt.params().iterator(); iter.hasNext(); ) {
            ParamInstance pi = (ParamInstance)iter.next();
            String paramFieldName = ParamToJavaExpr_c.paramFieldName(pi);
            String paramArgName = ParamToJavaExpr_c.paramArgName(pi);
            inits.add(rw.qq().parseStmt("this." + paramFieldName + " = " + paramArgName + ";"));
        }


        return rw.java_nf().ConstructorDecl(Position.COMPILER_GENERATED,
                                            Flags.PUBLIC,
                                            jpt.name(),
                                            formals,
                                            Collections.EMPTY_LIST,
                                            rw.java_nf().Block(Position.COMPILER_GENERATED,
                                                               inits));
    }
}
