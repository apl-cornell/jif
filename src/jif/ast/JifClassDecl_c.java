package jif.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jif.types.ActsForConstraint;
import jif.types.ActsForParam;
import jif.types.Assertion;
import jif.types.JifClassType;
import jif.types.JifContext;
import jif.types.JifParsedPolyType;
import jif.types.JifTypeSystem;
import jif.types.LabelLeAssertion;
import jif.types.Param;
import jif.types.ParamInstance;
import jif.types.label.AccessPathThis;
import jif.types.label.Label;
import jif.types.principal.Principal;
import polyglot.ast.ClassBody;
import polyglot.ast.ClassDecl_c;
import polyglot.ast.Id;
import polyglot.ast.Node;
import polyglot.ast.TypeNode;
import polyglot.ext.param.types.MuPClass;
import polyglot.types.ClassType;
import polyglot.types.Context;
import polyglot.types.Flags;
import polyglot.types.ParsedClassType;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.CodeWriter;
import polyglot.util.CollectionUtil;
import polyglot.util.InternalCompilerError;
import polyglot.util.ListUtil;
import polyglot.util.Position;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.Translator;
import polyglot.visit.TypeBuilder;

/** An implementation of the <code>JifClassDecl</code> interface.
 */
public class JifClassDecl_c extends ClassDecl_c implements JifClassDecl
{
    protected List<ParamDecl> params;
    protected List<PrincipalNode> authority;
    protected List<ConstraintNode<Assertion>> constraints;

    public JifClassDecl_c(Position pos, Flags flags, Id name,
            List<ParamDecl> params, TypeNode superClass,
            List<TypeNode> interfaces, List<PrincipalNode> authority,
            List<ConstraintNode<Assertion>> constraints, ClassBody body) {
        super(pos, flags, name, superClass, interfaces, body);
        this.params = Collections.unmodifiableList(new ArrayList<ParamDecl>(params));
        this.authority = Collections.unmodifiableList(new ArrayList<PrincipalNode>(authority));
        this.constraints =
                Collections
                .unmodifiableList(new ArrayList<ConstraintNode<Assertion>>(
                        constraints));
    }

    @Override
    public List<ConstraintNode<Assertion>> constraints() {
        return this.constraints;
    }
    @Override
    public JifClassDecl constraints(List<ConstraintNode<Assertion>> constraints) {
        JifClassDecl_c n = (JifClassDecl_c) copy();
        n.constraints =
                Collections
                .unmodifiableList(new ArrayList<ConstraintNode<Assertion>>(
                        constraints));
        return n;
    }

    @Override
    public List<ParamDecl> params() {
        return this.params;
    }

    @Override
    public JifClassDecl params(List<ParamDecl> params) {
        JifClassDecl_c n = (JifClassDecl_c) copy();
        n.params = Collections.unmodifiableList(new ArrayList<ParamDecl>(params));
        return n;
    }

    @Override
    public List<PrincipalNode> authority() {
        return this.authority;
    }

    @Override
    public JifClassDecl authority(List<PrincipalNode> authority) {
        JifClassDecl_c n = (JifClassDecl_c) copy();
        n.authority = Collections.unmodifiableList(new ArrayList<PrincipalNode>(authority));
        return n;
    }

    protected JifClassDecl_c reconstruct(Id name, List<ParamDecl> params,
            TypeNode superClass, List<TypeNode> interfaces,
            List<PrincipalNode> authority,
            List<ConstraintNode<Assertion>> constraints, ClassBody body) {
        if (! CollectionUtil.equals(params, this.params) || ! CollectionUtil.equals(authority, this.authority)
                || ! CollectionUtil.equals(params, this.constraints)) {
            JifClassDecl_c n = (JifClassDecl_c) copy();
            n.params = ListUtil.copy(params, true);
            n.authority = ListUtil.copy(authority, true);
            n.constraints = ListUtil.copy(constraints, true);
            return (JifClassDecl_c) n.reconstruct(name, superClass, interfaces, body);
        }

        return (JifClassDecl_c) super.reconstruct(name, superClass, interfaces, body);
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        Id name = (Id)visitChild(this.name, v);
        List<ParamDecl> params = visitList(this.params, v);
        TypeNode superClass = (TypeNode) visitChild(this.superClass, v);
        List<TypeNode> interfaces = visitList(this.interfaces, v);
        List<PrincipalNode> authority = visitList(this.authority, v);
        List<ConstraintNode<Assertion>> constraints = visitList(this.constraints, v);
        ClassBody body = (ClassBody) visitChild(this.body, v);
        return reconstruct(name, params, superClass, interfaces, authority, constraints, body);
    }

    @Override
    public Context enterScope(Context c) {
        JifContext A = (JifContext) c;
        A = addParamsToContext(A);
        A.setProvider(((JifClassType) type).provider());
        return addConstraintsToContext(A);
    }
    @Override
    public Context enterChildScope(Node child, Context c) {
        if (child == this.body) {
            JifContext A = (JifContext) c;
            JifParsedPolyType ct = (JifParsedPolyType) this.type;
            ClassType inst = ct;
            A = (JifContext)A.pushClass(ct, inst);
            return addAuthorityToContext(A);
        }
        return super.enterChildScope(child, c);
    }

    public JifContext addConstraintsToContext(JifContext A) {
        final JifParsedPolyType ct = (JifParsedPolyType) this.type;

        // Add programer-specified constraints.
        for (Assertion constraint : ct.constraints()) {
            if (constraint instanceof ActsForConstraint) {
                @SuppressWarnings("unchecked")
                ActsForConstraint<ActsForParam, ActsForParam> pi =
                (ActsForConstraint<ActsForParam, ActsForParam>) constraint;
                ActsForParam actor = pi.actor();
                ActsForParam granter = pi.granter();
                if (actor instanceof Principal && granter instanceof Principal) {
                    A.addActsFor((Principal) actor, (Principal) granter);
                } else if (actor instanceof Label) {
                    A.addActsFor((Label) actor, (Principal) granter);
                } else {
                    throw new InternalCompilerError(
                            "Unexpected ActsForParam type: " + actor.getClass()
                            + " actsfor " + granter.getClass());
                }
            } else if (constraint instanceof LabelLeAssertion) {
                LabelLeAssertion lle = (LabelLeAssertion) constraint;
                A.addAssertionLE(lle.lhs(), lle.rhs());
            } else {
                throw new InternalCompilerError("Unexpected assertion type: "
                        + constraint, constraint.position());
            }
        }

        return A;
    }

    @Override
    public  JifContext addParamsToContext(JifContext A) {
        JifParsedPolyType ct = (JifParsedPolyType) this.type;
        A = (JifContext)A.pushBlock();
        for (ParamInstance pi : ct.params()) {
            A.addVariable(pi);
        }
        return A;
    }

    @Override
    public JifContext addAuthorityToContext(JifContext A) {
        JifTypeSystem ts = (JifTypeSystem)A.typeSystem();
        JifParsedPolyType ct = (JifParsedPolyType) this.type;
        Set<Principal> s = new LinkedHashSet<Principal>(ct.authority());
        if (ts.isSubtype(ct, ts.PrincipalClass())) {
            // This class implements jif.lang.Princpal, and as such
            // implicitly has the authority of the principal represented by
            // "this"
            s.add(ts.dynamicPrincipal(ct.position(),
                    new AccessPathThis(ct, ct.position())));
        }
        A.setAuthority(s);
        return A;
    }

    @Override
    public Node buildTypes(TypeBuilder tb) throws SemanticException {
        JifClassDecl_c n = (JifClassDecl_c) super.buildTypes(tb);
        n.buildParams((JifTypeSystem) tb.typeSystem());
        return n;
    }

    private void buildParams(JifTypeSystem ts) throws SemanticException {
        JifParsedPolyType ct = (JifParsedPolyType)this.type;
        if (ct == null) {
            // The only way the class type could be null is if
            // super.buildTypes failed. Give up now.
            return;
        }
        MuPClass<ParamInstance, Param> pc = ts.mutablePClass(ct.position());

        ct.setInstantiatedFrom(pc);
        pc.clazz(ct);

        Set<String> names = new LinkedHashSet<String>(params.size());

        List<ParamInstance> newParams = new ArrayList<ParamInstance>(params.size());
        for (ParamDecl p : params) {
            newParams.add(p.paramInstance());
            //check for renaming error
            if (names.contains(p.name()))
                throw new SemanticException("Redefined Parameter Error.", p
                        .position());
            else
                names.add(p.name());
        }
        ct.setParams(newParams);
    }


    @Override
    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        JifClassDecl_c n = (JifClassDecl_c)super.disambiguate(ar);

        JifParsedPolyType ct = (JifParsedPolyType) n.type;

        List<Principal> principals = new ArrayList<Principal>(n.authority().size());
        for (PrincipalNode p : n.authority()) {
            principals.add(p.principal());
        }
        ct.setAuthority(principals);

        List<Assertion> constraints =
                new ArrayList<Assertion>(n.constraints().size());
        for (ConstraintNode<Assertion> cn : n.constraints()) {
            if (!cn.isDisambiguated()) {
                // constraint nodes haven't been disambiguated yet.
                ar.job().extensionInfo().scheduler().currentGoal().setUnreachableThisRun();
                return this;
            }
            constraints.addAll(cn.constraints());
        }
        ct.setConstraints(constraints);

        return n;
    }

    @Override
    public JifClassDecl type(Type type) {
        JifClassDecl_c n = (JifClassDecl_c) copy();
        n.type = (ParsedClassType) type;
        return n;
    }

    @Override
    public void prettyPrintHeader(CodeWriter w, PrettyPrinter tr) {
        if (flags.isInterface()) {
            w.write(flags.clearInterface().clearAbstract().translate());
            w.write("interface ");
        }
        else {
            w.write(flags.translate());
            w.write("class ");
        }

        w.write(name.id());

        if (! params.isEmpty()) {
            w.write("[");
            for (Iterator<ParamDecl> i = params.iterator(); i.hasNext(); ) {
                ParamDecl p = i.next();
                print(p, w, tr);
                if (i.hasNext()) {
                    w.write(",");
                    w.allowBreak(0, " ");
                }
            }
            w.write("]");
        }

        if (! authority.isEmpty()) {
            w.write(" authority(");
            for (Iterator<PrincipalNode> i = authority.iterator(); i.hasNext(); ) {
                PrincipalNode p = i.next();
                print(p, w, tr);
                if (i.hasNext()) {
                    w.write(",");
                    w.allowBreak(0, " ");
                }
            }
            w.write(")");
        }

        if (superClass() != null) {
            w.write(" extends ");
            print(superClass(), w, tr);
        }

        if (! interfaces.isEmpty()) {
            if (flags.isInterface()) {
                w.write(" extends ");
            }
            else {
                w.write(" implements ");
            }

            for (Iterator<TypeNode> i = interfaces().iterator(); i.hasNext(); ) {
                TypeNode tn = i.next();
                print(tn, w, tr);

                if (i.hasNext()) {
                    w.write (", ");
                }
            }
        }

        w.write(" {");
    }

//  public Node typeCheck(TypeChecker tc) throws SemanticException {
//  // The invariantness of the class must agree with its super class
//  // and its interfaces.
//  Type superClass = null;
//  if (this.superClass() != null) {
//  superClass = this.superClass().type();
//  }

//  return super.typeCheck(tc);

//  }

    @Override
    public void translate(CodeWriter w, Translator tr) {
        throw new InternalCompilerError("cannot translate " + this);
    }
}
