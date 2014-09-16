package jif.translate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import jif.ast.JifMethodDecl;
import jif.types.ActsForConstraint;
import jif.types.Assertion;
import jif.types.JifMethodInstance;
import jif.types.JifParsedPolyType;
import jif.types.JifPolyType;
import jif.types.JifSubstClassType_c;
import jif.types.JifTypeSystem;
import jif.types.LabelLeAssertion;
import jif.types.LabeledType;
import jif.types.Param;
import jif.types.ParamInstance;
import jif.types.TypeParam;
import polyglot.ast.Binary;
import polyglot.ast.Block;
import polyglot.ast.Expr;
import polyglot.ast.Formal;
import polyglot.ast.MethodDecl;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.Stmt;
import polyglot.ast.TypeNode;
import polyglot.ext.jl5.ast.JL5NodeFactory;
import polyglot.types.Flags;
import polyglot.types.MethodInstance;
import polyglot.types.Named;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.NodeVisitor;

public class MethodDeclToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected JifMethodInstance mi;
    protected List<Formal> formals;

    /**
     * @throws SemanticException
     */
    @Override
    public NodeVisitor toJavaEnter(JifToJavaRewriter rw)
            throws SemanticException {
        // Bypass labels and constraints
        JifMethodDecl n = (JifMethodDecl) node();

        mi = (JifMethodInstance) n.methodInstance();
        formals = new ArrayList<Formal>(n.formals());

        // Bypass startLabel, returnLabel and constraints.
        return rw.bypass(n.startLabel()).bypass(n.returnLabel())
                .bypass(n.constraints());
    }

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        MethodDecl n = (MethodDecl) node();
        JL5NodeFactory nf = (JL5NodeFactory) rw.java_nf();

        if (n.methodInstance().returnType() instanceof LabeledType) {
            Type t = ((LabeledType) n.methodInstance().returnType()).typePart();
            if (t instanceof JifSubstClassType_c) {
                JifSubstClassType_c subB = (JifSubstClassType_c) t;
                List<TypeNode> args = new ArrayList<TypeNode>();
                Iterator<Entry<ParamInstance, Param>> entries =
                        subB.subst().entries();
                while (entries.hasNext()) {
                    Entry<ParamInstance, Param> e = entries.next();
                    if (e.getKey().isType()) {
                        TypeParam tp = (TypeParam) e.getValue();
                        Named namedType = (Named) tp.type();
                        args.add(nf.AmbTypeNode(e.getValue().position(),
                                nf.Id(tp.position(), namedType.name())));
                    }
                }
                if (args.size() > 0) {
                    n =
                            n.returnType(nf.AmbTypeInstantiation(subB
                                    .position(),
                                    nf.AmbTypeNode(subB.position(), nf.Id(n
                                            .returnType().position(), n
                                            .returnType().name())), args));
                }
            }
        }

        List<Formal> newFormals = new ArrayList<Formal>();
        for (int i = 0; i < n.methodInstance().formalTypes().size(); i++) {
            Type ft = n.methodInstance().formalTypes().get(i);
            TypeNode tn = null;
            if (ft instanceof LabeledType) {
                Type t = ((LabeledType) ft).typePart();
                if (t instanceof JifSubstClassType_c) {
                    JifSubstClassType_c subB = (JifSubstClassType_c) t;
                    List<TypeNode> args = new ArrayList<TypeNode>();
                    Iterator<Entry<ParamInstance, Param>> entries =
                            subB.subst().entries();
                    while (entries.hasNext()) {
                        Entry<ParamInstance, Param> e = entries.next();
                        if (e.getKey().isType()) {
                            TypeParam tp = (TypeParam) e.getValue();
                            Named namedType = (Named) tp.type();
                            args.add(nf.AmbTypeNode(e.getValue().position(),
                                    nf.Id(tp.position(), namedType.name())));
                        }
                    }
                    if (args.size() > 0) {
                        tn =
                                nf.AmbTypeInstantiation(
                                        subB.position(),
                                        nf.AmbTypeNode(subB.position(), nf.Id(
                                                t.position(), subB.name())),
                                                args);
                    }
                }
            }
            Formal f = n.formals().get(i);
            newFormals.add(nf.Formal(f.position(), f.flags(),
                    tn == null ? f.type() : tn, nf.Id(f.position(), f.name())));
        }
        n = (MethodDecl) n.formals(newFormals);

        boolean isMainMethod = "main".equals(n.name()) && n.flags().isStatic();
        if (isMainMethod && n.formals().size() == 2) {
            // the method is static main(principal p, String[] args). We
            // need to translate this specially.
            // (The typechecking for JifMethodDecl ensures that the formals
            // are of the correct type.)
            return staticMainToJava(rw, n);
        }

        MethodInstance mi = n.methodInstance();
        List<Formal> formals = new ArrayList<Formal>(n.formals().size() + 2);

        // for static methods, add args for the params of the class
        if (mi.flags().isStatic() && mi.container() instanceof JifPolyType) {
            JifPolyType jpt = (JifPolyType) mi.container();
            formals.addAll(ClassDeclToJavaExt_c.produceParamFormals(jpt, rw));
        }

        formals.addAll(n.formals());
        n =
                rw.java_nf().MethodDecl(n.position(), n.flags(),
                        n.returnType(), n.id(), formals, n.throwTypes(),
                        n.body());
        n = n.methodInstance(null);

        if (isMainMethod) {
            // Translate the constraints and use them to guard the body.
            n = (MethodDecl) n.body(guardWithConstraints(rw, n.body()));
        }

        return n;
    }

    /** Rewrite static main(principal p, String[] args) {...} to
     * static main(String[] args) {Principal p = Runtime.getUser(); {...} };
     */
    public Node staticMainToJava(JifToJavaRewriter rw, MethodDecl n)
            throws SemanticException {
        Formal formal0 = n.formals().get(0); // the principal
        Formal formal1 = n.formals().get(1); // the string array
        List<Formal> formalList = Collections.singletonList(formal1);

        Block origBody = n.body();

        JifTypeSystem jifTs = rw.jif_ts();
        TypeNode type = rw.qq().parseType(jifTs.PrincipalClassName());
        Expr init =
                rw.qq().parseExpr(
                        jifTs.RuntimePackageName() + ".Runtime.user(null)");

        Stmt declPrincipal =
                rw.java_nf().LocalDecl(origBody.position(), Flags.FINAL, type,
                        formal0.id(), init);

        // Translate the constraints and use them to guard the body.
        Block newBody = guardWithConstraints(rw, origBody);

        newBody =
                rw.java_nf().Block(origBody.position(), declPrincipal, newBody);

        n =
                rw.java_nf().MethodDecl(n.position(), n.flags(),
                        n.returnType(), n.id(), formalList, n.throwTypes(),
                        newBody);
        n = n.methodInstance(null);
        return n;
    }

    protected Block guardWithConstraints(JifToJavaRewriter rw, Block b)
            throws SemanticException {
        NodeFactory nf = rw.java_nf();
        List<Assertion> constraints =
                new ArrayList<Assertion>(mi.constraints());
        JifParsedPolyType pct = (JifParsedPolyType) rw.currentClass();
        constraints.addAll(pct.constraints());
        Position pos = b.position();
        Expr guard = null;
        for (Assertion constraint : constraints) {
            Expr conjunct;
            if (constraint instanceof ActsForConstraint) {
                conjunct = ((ActsForConstraint<?, ?>) constraint).toJava(rw);
            } else if (constraint instanceof LabelLeAssertion) {
                conjunct = ((LabelLeAssertion) constraint).toJava(rw);
            } else {
                continue;
            }

            // Turn the constraint into a boolean expression.
            if (guard == null) {
                guard = conjunct;
            } else {
                guard = nf.Binary(pos, guard, Binary.COND_AND, conjunct);
            }
        }

        if (guard == null) return b;
        Expr errorMessage =
                nf.StringLit(pos, "The method " + mi.debugString()
                        + " has constraints that are unsatisfied.");
        Stmt error =
                nf.Throw(pos, nf.New(pos,
                        nf.CanonicalTypeNode(pos, rw.java_ts().Error()),
                        Collections.singletonList(errorMessage)));
        return nf.Block(pos, nf.If(pos, guard, b, error));
    }
}
