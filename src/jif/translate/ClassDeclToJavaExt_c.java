package jif.translate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jif.ast.JifClassDecl;
import jif.types.JifContext;
import jif.types.JifPolyType;
import jif.types.JifSubst;
import jif.types.JifSubstType;
import jif.types.JifTypeSystem;
import jif.types.Param;
import jif.types.ParamInstance;
import jif.types.label.Label;
import jif.types.principal.Principal;
import polyglot.ast.Block;
import polyglot.ast.ClassBody;
import polyglot.ast.ClassDecl;
import polyglot.ast.ClassMember;
import polyglot.ast.Expr;
import polyglot.ast.Formal;
import polyglot.ast.Id;
import polyglot.ast.Node;
import polyglot.ast.Stmt;
import polyglot.ast.TypeNode;
import polyglot.types.ClassType;
import polyglot.types.ConstructorInstance;
import polyglot.types.Context;
import polyglot.types.Flags;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.NodeVisitor;

public class ClassDeclToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    /*
     * Some static final constants and methods for producing names of
     * generated methods and fields.
     */
    public static final String INSTANCEOF_METHOD_NAME = "jif$Instanceof";
    protected static final String INITIALIZATIONS_METHOD_NAME = "jif$init";

    public static final String castMethodName(ClassType ct) {
        return "jif$cast$" + ct.fullName().replace('.', '_');
    }

    public static final String interfaceClassImplName(String jifInterfaceName) {
        return jifInterfaceName + "_JIF_IMPL";
    }

    public static final String constructorTranslatedName(ClassType ct) {
        return (ct.fullName() + ".").replace('.', '$');
    }

    protected static final String DEFAULT_CONSTRUCTOR_INVOKER_METHOD_NAME =
            "jif$invokeDefConstructor";

    /*
     * Code for translating ClassDecls
     */
    private boolean hasDefaultConstructor = false;
    private List<? extends Type> defaultConstructorExceptions = null;

    @Override
    public NodeVisitor toJavaEnter(JifToJavaRewriter rw)
            throws SemanticException {
        // Bypass params and authority.
        JifClassDecl n = (JifClassDecl) node();

        rw.enteringClass(n.type());

        ClassType ct = n.type();
        for (ConstructorInstance ci : ct.constructors()) {
            if (ci.formalTypes().isEmpty()) {
                hasDefaultConstructor = true;
                defaultConstructorExceptions = ci.throwTypes();
                break;
            }
        }

        return rw.bypass(n.params()).bypass(n.authority())
                .bypass(n.constraints());
    }

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        Node result = toJavaImpl(rw);
        rw.leavingClass();
        return result;
    }

    protected Node toJavaImpl(JifToJavaRewriter rw) throws SemanticException {
        JifClassDecl n = (JifClassDecl) node();
        JifPolyType jpt = (JifPolyType) n.type();

        ClassBody cb = n.body();
        if (!jpt.flags().isInterface()) {
            if (!rw.jif_ts().isSignature(jpt)) {
                // add constructor
                cb = cb.addMember(produceConstructor(jpt, rw));
                if (hasDefaultConstructor) {
                    cb = cb.addMember(produceDefaultConstructorInvoker(jpt, rw,
                            defaultConstructorExceptions));
                }
                // add initializer method (which is called from every
                // translated constructer.
                cb = addInitializer(cb, rw);

                // add any static initializers
                cb = addStaticInitializers(cb, rw);
            }
            if (rw.jif_ts().needsDynamicTypeMethods(jpt)) {
                // add instanceof and cast static methods to the class
                cb = cb.addMember(produceInstanceOfMethod(jpt, rw, false));
                cb = cb.addMember(produceCastMethod(jpt, rw));
            }

            if (rw.jif_ts().isParamsRuntimeRep(jpt)) {
                if (!jpt.params().isEmpty()) {
                    // add fields for params
                    if (!rw.jif_ts().isSignature(jpt)) {
                        for (ParamInstance pi : jpt.params()) {
                            String paramFieldName =
                                    ParamToJavaExpr_c.paramFieldName(pi);
                            TypeNode tn = typeNodeForParam(pi, rw);
                            cb = cb.addMember(
                                    rw.qq().parseMember("private final %T %s;",
                                            tn, paramFieldName));
                        }
                    }
                }

                // add getter methods for any params declared in interfaces
                cb = addInterfaceParamGetters(cb, jpt, jpt, rw);
            }
        } else {
            // it's an interface
            if (rw.jif_ts().needsImplClass(jpt)) {
                ClassBody implBody =
                        rw.java_nf().ClassBody(Position.compilerGenerated(),
                                new ArrayList<ClassMember>(2));
                implBody = implBody
                        .addMember(produceInstanceOfMethod(jpt, rw, true));
                implBody = implBody.addMember(produceCastMethod(jpt, rw));

                Id name = rw.java_nf().Id(Position.compilerGenerated(),
                        interfaceClassImplName(n.name()));
                ClassDecl implDecl = rw.java_nf().ClassDecl(
                        Position.compilerGenerated(),
                        n.flags().clearInterface().Abstract(), name, null,
                        Collections.<TypeNode> emptyList(), implBody);
                rw.addAdditionalClassDecl(implDecl);

                // add getters for params to the interface
                for (ParamInstance pi : jpt.params()) {
                    String paramFieldNameGetter =
                            ParamToJavaExpr_c.paramFieldNameGetter(pi);
                    TypeNode tn = typeNodeForParam(pi, rw);
                    cb = cb.addMember(rw.qq().parseMember("%T %s();", tn,
                            paramFieldNameGetter));
                }

            }
        }

        return rw.java_nf().ClassDecl(n.position(), n.flags(), n.id(),
                n.superClass(), n.interfaces(), cb, n.javadoc());
    }

    /**
     * Create a method for initializations, and add it to cb.
     */
    protected ClassBody addInitializer(ClassBody cb, JifToJavaRewriter rw) {
        List<Stmt> inits = new ArrayList<Stmt>(rw.getInitializations());
        rw.getInitializations().clear();
        return cb.addMember(rw.qq().parseMember("private void %s() { %LS }",
                INITIALIZATIONS_METHOD_NAME, inits));
    }

    /**
     * Create methods for static initializations, and add it to cb.
     *
     */
    protected ClassBody addStaticInitializers(ClassBody cb,
            JifToJavaRewriter rw) {
        if (rw.getStaticInitializations().isEmpty()) {
            return cb;
        }
        List<Stmt> inits = new ArrayList<Stmt>(rw.getStaticInitializations());
        rw.getStaticInitializations().clear();

        Block b;
        if (inits.size() == 1) {
            b = (Block) inits.get(0);
        } else {
            b = rw.java_nf().Block(Position.compilerGenerated(), inits);
        }
        return cb.addMember(rw.java_nf()
                .Initializer(Position.compilerGenerated(), Flags.STATIC, b));
    }

    /**
     * Go through the interfaces, and add any required fields and getters for the fields.
     * @param cb
     * @param jpt
     * @param rw
     * @throws SemanticException
     */
    protected ClassBody addInterfaceParamGetters(ClassBody cb,
            JifPolyType baseClass, JifPolyType jpt, JifToJavaRewriter rw)
            throws SemanticException {
        // go through the interfaces of cb
        if (!rw.jif_ts().isParamsRuntimeRep(jpt)) {
            // don't bother adding interface methods for classes that don't represent the runtime params (i.e., Jif sig classes)
            return cb;
        }
        for (Type interf : jpt.interfaces()) {
            if (rw.jif_ts().isParamsRuntimeRep(interf)
                    && !rw.jif_ts().isSubtype(baseClass.superType(), interf)) {

                // the interface is not implemented in a super class,
                // so add fields and params for runtime representation of params
                JifPolyType interfPT = null;
                if (interf instanceof JifSubstType) {
                    JifSubstType interfST = (JifSubstType) interf;
                    JifSubst subst = (JifSubst) interfST.subst();
                    interfPT = (JifPolyType) interfST.base();
                    for (ParamInstance pi : interfPT.params()) {
                        String paramFieldName =
                                ParamToJavaExpr_c.paramFieldName(pi);
                        String paramFieldNameGetter =
                                ParamToJavaExpr_c.paramFieldNameGetter(pi);
                        TypeNode tn = typeNodeForParam(pi, rw);
                        Expr lblExpr = rw.paramToJava(subst.get(pi));
                        if (!rw.jif_ts().isSignature(jpt)) {
                            // it's a real Jif class, so add a real implementation
                            cb = cb.addMember(rw.qq().parseMember(
                                    "private %T %s;", tn, paramFieldName));
                            cb = cb.addMember(rw.qq().parseMember(
                                    "public final %T %s() { "
                                            + " if (this.%s==null) this.%s = %E; "
                                            + "return this.%s; }",
                                    tn, paramFieldNameGetter, paramFieldName,
                                    paramFieldName, lblExpr, paramFieldName));
                        } else {
                            // it's just a signature file, add the method sig but nothing else.
                            cb = cb.addMember(rw.qq().parseMember(
                                    "public final native %T %s();", tn,
                                    paramFieldNameGetter));
                        }
                    }
                } else if (interf instanceof JifPolyType) {
                    interfPT = (JifPolyType) interf;
                }

                if (interfPT != null) {
                    // recurse on the supertype of interfaces.
                    cb = addInterfaceParamGetters(cb, baseClass, interfPT, rw);
                }

            }
        }
        return cb;

    }

    protected ClassMember produceInstanceOfMethod(JifPolyType jpt,
            JifToJavaRewriter rw, boolean useGetters) throws SemanticException {
        Context A = rw.context();
        rw = (JifToJavaRewriter) rw.context(A.pushStatic());
        JifTypeSystem jifts = rw.jif_ts();
        List<Formal> formals = produceFormals(jpt, rw);

        String name = jpt.name();

        if (jifts.isSignature(jpt)) {
            // just produce a header
            return rw.qq().parseMember("static public native boolean %s(%LF);",
                    INSTANCEOF_METHOD_NAME, formals);
        }

        StringBuffer sb = new StringBuffer();
        sb.append("static public boolean %s(%LF) {");
        if (jpt.params().isEmpty()) {
            sb.append("return (o instanceof %s);");
        } else {
            sb.append("if (o instanceof %s) { ");
            sb.append("%s c = (%s)o; ");

            // now test each of the params
            boolean moreThanOneParam = (jpt.params().size() > 1);
            sb.append(moreThanOneParam ? "boolean ok = true;" : "");
            for (ParamInstance pi : jpt.params()) {
                String paramFieldName = ParamToJavaExpr_c.paramFieldName(pi);
                String paramArgName = ParamToJavaExpr_c.paramArgName(pi);
                String comparison = "equivalentTo";
                if (pi.isCovariantLabel()) {
                    comparison = "relabelsTo";
                }

                sb.append(moreThanOneParam ? "ok = ok && " : "return ");

                String paramExpr = paramFieldName;
                if (useGetters) {
                    paramExpr =
                            ParamToJavaExpr_c.paramFieldNameGetter(pi) + "()";
                }
                if (pi.isPrincipal()) {
                    // e.g., PrincipalUtil.equivTo(c.expr, paramArgName)
                    sb.append(jifts.PrincipalUtilClassName() + "." + comparison
                            + "(c." + paramExpr + "," + paramArgName + ");");
                } else {
                    // e.g., LabelUtil.equivTo(paramArgName)
                    sb.append(rw.runtimeLabelUtil() + "." + comparison + "(c."
                            + paramExpr + "," + paramArgName + ");");
                }
            }
            if (moreThanOneParam) sb.append("return ok;");
            sb.append("}");
            sb.append("return false;");
        }

        sb.append("}");
        return rw.qq().parseMember(sb.toString(), INSTANCEOF_METHOD_NAME,
                formals, name, name, name);
    }

    private static TypeNode typeNodeForParam(ParamInstance pi,
            JifToJavaRewriter rw) throws SemanticException {
        Type paramType = pi.isPrincipal() ? rw.jif_ts().Principal()
                : rw.jif_ts().Label();
        return rw.typeToJava(paramType, Position.compilerGenerated());
    }

    protected ClassMember produceCastMethod(JifPolyType jpt,
            JifToJavaRewriter rw) throws SemanticException {
        Context A = rw.context();
        rw = (JifToJavaRewriter) rw.context(A.pushStatic());

        TypeNode tn = rw.typeToJava(jpt, Position.compilerGenerated());
        ;
        List<Formal> formals = produceFormals(jpt, rw);
        if (rw.jif_ts().isSignature(jpt)) {
            // just produce a header
            return rw.qq().parseMember("static public native %T %s(%LF);", tn,
                    castMethodName(jpt), formals);

        }

        StringBuffer sb = new StringBuffer();
        sb.append("static public %T %s(%LF) {");
        sb.append("if (o == null) return null;");
        sb.append("if (%s(%LE)) return (%T)o;");
        sb.append("throw new ClassCastException();");
        sb.append("}");

        List<Expr> args = produceParamArgs(jpt, rw);
        return rw.qq().parseMember(sb.toString(), tn, castMethodName(jpt),
                formals, INSTANCEOF_METHOD_NAME, args, tn);
    }

    protected List<Formal> produceFormals(JifPolyType jpt, JifToJavaRewriter rw)
            throws SemanticException {
        List<Formal> formals = new ArrayList<Formal>(jpt.params().size() + 1);
        Position pos = Position.compilerGenerated();
        for (ParamInstance pi : jpt.params()) {
            Id paramArgName =
                    rw.java_nf().Id(pos, ParamToJavaExpr_c.paramArgName(pi));
            TypeNode tn = typeNodeForParam(pi, rw);
            Formal f = rw.java_nf().Formal(pos, Flags.FINAL, tn, paramArgName);
            formals.add(f);
        }
        formals.add(produceObjectFormal(jpt, rw));
        return formals;
    }

    /**
     * Returns the formal with id "o" for the object passed to the cast and instanceof methods
     */
    protected Formal produceObjectFormal(JifPolyType jpt,
            JifToJavaRewriter rw) {
        Position pos = Position.compilerGenerated();
        TypeNode tn = rw.qq().parseType("java.lang.Object");
        Id id = rw.java_nf().Id(pos, "o");
        return rw.java_nf().Formal(pos, Flags.FINAL, tn, id);
    }

    static protected List<Formal> produceParamFormals(JifPolyType jpt,
            JifToJavaRewriter rw) throws SemanticException {
        List<Formal> formals = new ArrayList<Formal>(jpt.params().size() + 1);
        Position pos = Position.compilerGenerated();
        for (ParamInstance pi : jpt.params()) {
            Id paramArgName =
                    rw.java_nf().Id(pos, ParamToJavaExpr_c.paramArgName(pi));
            TypeNode tn = typeNodeForParam(pi, rw);
            Formal f = rw.java_nf().Formal(pos, Flags.FINAL, tn, paramArgName);
            formals.add(f);
        }
        return formals;
    }

    protected List<Expr> produceParamArgs(JifPolyType jpt,
            JifToJavaRewriter rw) {
        List<Expr> args = new ArrayList<Expr>(jpt.params().size() + 1);
        for (ParamInstance pi : jpt.params()) {
            String paramArgName = ParamToJavaExpr_c.paramArgName(pi);
            args.add(rw.qq().parseExpr(paramArgName));
        }

        // add the object argument too.
        args.add(rw.qq().parseExpr("o"));
        return args;
    }

    protected ClassMember produceConstructor(JifPolyType jpt,
            JifToJavaRewriter rw) throws SemanticException {
        // add arguments for params.
        List<Formal> formals = produceParamFormals(jpt, rw);

        List<Stmt> inits = new ArrayList<Stmt>();

        // add super call.
        List<Expr> superArgs = new ArrayList<Expr>();
        Type superType = jpt.superType();
        if (superType instanceof JifSubstType && rw.jif_ts()
                .isParamsRuntimeRep(((JifSubstType) superType).base())) {
            JifSubstType superjst = (JifSubstType) jpt.superType();
            JifPolyType superjpt = (JifPolyType) superjst.base();

            JifContext A = (JifContext) rw.context();
            JifToJavaRewriter rwCons =
                    (JifToJavaRewriter) rw.context(A.pushConstructorCall());
            Expr thisQualifier = rw.qq().parseExpr("this");
            for (ParamInstance pi : superjpt.params()) {
                Param param = ((JifSubst) superjst.subst()).get(pi);
                if (pi.isLabel()) {
                    superArgs
                            .add(((Label) param).toJava(rwCons, thisQualifier));
                } else {
                    superArgs.add(
                            ((Principal) param).toJava(rwCons, thisQualifier));
                }
            }
        }
        inits.add(rw.qq().parseStmt("super(%LE);", (Object) superArgs));

        // create initializers for the fields from the arguments
        for (ParamInstance pi : jpt.params()) {
            String paramFieldName = ParamToJavaExpr_c.paramFieldName(pi);
            String paramArgName = ParamToJavaExpr_c.paramArgName(pi);
            inits.add(rw.qq().parseStmt(
                    "this." + paramFieldName + " = " + paramArgName + ";"));
        }

        inits.addAll(additionalConstructorCode(rw));

        Id name = rw.java_nf().Id(Position.compilerGenerated(), jpt.name());
        return rw.java_nf().ConstructorDecl(Position.compilerGenerated(),
                Flags.PUBLIC, name, formals, Collections.<TypeNode> emptyList(),
                rw.java_nf().Block(Position.compilerGenerated(), inits));
    }

    protected List<Stmt> additionalConstructorCode(JifToJavaRewriter rw) {
        return Collections.emptyList();
    }

    /**
     * Produce a method (with a standard name) that will invoke the default
     * constructor of the class. This method assumes that such a default
     * constructor exists.
     */
    protected ClassMember produceDefaultConstructorInvoker(ClassType ct,
            JifToJavaRewriter rw, List<? extends Type> throwTypes) {
        // add arguments for params.
        if (throwTypes == null || throwTypes.isEmpty()) {
            return rw.qq().parseMember("public void "
                    + DEFAULT_CONSTRUCTOR_INVOKER_METHOD_NAME + "() {" + "this."
                    + constructorTranslatedName(ct) + "();" + "}");
        }
        List<TypeNode> typeNodes = new ArrayList<TypeNode>(throwTypes.size());
        for (Type t : throwTypes) {
            Id name = rw.java_nf().Id(Position.compilerGenerated(),
                    t.toClass().name());
            TypeNode tn = rw.java_nf().AmbTypeNode(Position.compilerGenerated(),
                    name);
            typeNodes.add(tn);
        }
        return rw.qq().parseMember(
                "public void " + DEFAULT_CONSTRUCTOR_INVOKER_METHOD_NAME
                        + "() throws %LT {" + "this."
                        + constructorTranslatedName(ct) + "();" + "}",
                (Object) typeNodes);

    }
}
