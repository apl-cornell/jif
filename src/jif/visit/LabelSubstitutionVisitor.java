package jif.visit;

import java.util.ArrayList;
import java.util.List;

import jif.ast.JifConstructorDecl;
import jif.ast.JifMethodDecl;
import jif.ast.LabelNode;
import jif.types.ActsForConstraint;
import jif.types.ActsForParam;
import jif.types.Assertion;
import jif.types.AuthConstraint;
import jif.types.AutoEndorseConstraint;
import jif.types.CallerConstraint;
import jif.types.JifConstructorInstance;
import jif.types.JifFieldInstance;
import jif.types.JifLocalInstance;
import jif.types.JifMethodInstance;
import jif.types.JifProcedureInstance;
import jif.types.LabelLeAssertion;
import jif.types.LabelSubstitution;
import jif.types.TypeSubstitutor;
import jif.types.label.Label;
import jif.types.principal.Principal;
import polyglot.ast.Block;
import polyglot.ast.Expr;
import polyglot.ast.FieldDecl;
import polyglot.ast.Formal;
import polyglot.ast.Local;
import polyglot.ast.LocalDecl;
import polyglot.ast.Node;
import polyglot.ast.ProcedureDecl;
import polyglot.ast.TypeNode;
import polyglot.types.LocalInstance;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.InternalCompilerError;
import polyglot.visit.NodeVisitor;

/**
 * Visits an AST, and applies a <code>LabelSubstitution</code> to all labels
 * that occur in the AST. The <code>LabelSubstitution</code> is not allowed
 * to throw any <code>SemanticException</code>s.
 */
public class LabelSubstitutionVisitor extends NodeVisitor {
    /**
     * Should the Rewriter skip over the
     */
    private boolean skipBody;

    /**
     * The substitution to use.
     */
    private LabelSubstitution substitution;

    /**
     * Utility class to rewrite labels in types.
     */
    private TypeSubstitutor typeSubstitutor;

    /**
     * 
     * @param substitution the LabelSubstitution to use.
     * @param skipBody skip over the body of method/constructor decls?
     */
    public LabelSubstitutionVisitor(LabelSubstitution substitution,
            boolean skipBody) {
        this(substitution, new TypeSubstitutor(substitution), skipBody);
    }

    public LabelSubstitutionVisitor(LabelSubstitution substitution,
            TypeSubstitutor typeSubst, boolean skipBody) {
        this.skipBody = skipBody;
        this.substitution = substitution;
        this.typeSubstitutor = typeSubst;
    }

    // Don't recurse into the body.
    @Override
    public Node override(Node n) {
        if (skipBody && n instanceof Block) {
            return n;
        }

        return null;
    }

    @Override
    public Node leave(Node old, Node n, NodeVisitor v) {
        try {
            if (n instanceof TypeNode) {
                TypeNode c = (TypeNode) n;
                c = rewriteTypeNode(c);
                return c;
            } else if (n instanceof Expr) {
                Expr e = (Expr) n;
                e = rewriteExpr(e);

                if (e instanceof Local) {
                    Local lc = (Local) e;
                    LocalInstance li = lc.localInstance();
                    Type t = rewriteType(li.type());

                    // Imperatively update the local instance.
                    li.setType(t);
                    return lc;
                }
                return e;
            } else if (n instanceof LabelNode) {
                LabelNode ln = (LabelNode) n;
                Label l = rewriteLabel(ln.label());
                ln = ln.label(l);
                return ln;
            } else if (n instanceof Formal) {
                Formal fn = (Formal) n;

                JifLocalInstance li = (JifLocalInstance) fn.localInstance();
                Type t = rewriteType(li.type());

                // Imperatively update the local instance.
                li.setType(t);
                li.setLabel(rewriteLabel(li.label()));
                return fn;
            } else if (n instanceof LocalDecl) {
                LocalDecl ld = (LocalDecl) n;

                JifLocalInstance li = (JifLocalInstance) ld.localInstance();
                Type t = rewriteType(li.type());

                // Imperatively update the local instance.
                li.setType(t);
                li.setLabel(rewriteLabel(li.label()));
                return ld;
            } else if (n instanceof FieldDecl) {
                FieldDecl fd = (FieldDecl) n;

                JifFieldInstance fi = (JifFieldInstance) fd.fieldInstance();

                // Imperatively update the field instance.
                rewriteFieldInstance(fi);

                return fd;
            } else if (n instanceof ProcedureDecl) {
                ProcedureDecl md = (ProcedureDecl) n;

                JifProcedureInstance mi =
                        (JifProcedureInstance) md.procedureInstance();

                // Process return label.
                mi.setReturnLabel(rewriteLabel(mi.returnLabel()),
                        mi.isDefaultReturnLabel());

                // Process PC bound.
                mi.setPCBound(rewriteLabel(mi.pcBound()),
                        mi.isDefaultPCBound());

                // Process throw types.
                ArrayList<Type> throwTypes =
                        new ArrayList<Type>(mi.throwTypes());
                for (int i = 0; i < throwTypes.size(); i++) {
                    throwTypes.set(i, rewriteType(throwTypes.get(i)));
                }

                // Process formal types.
                List<Type> formalTypes = new ArrayList<Type>(mi.formalTypes());
                for (int i = 0; i < formalTypes.size(); i++) {
                    formalTypes.set(i, rewriteType(formalTypes.get(i)));
                }

                // Process constraints.
                List<Assertion> constraints =
                        new ArrayList<>(mi.constraints().size());
                for (Assertion constraint : mi.constraints()) {
                    constraints.add(rewriteConstraint(constraint));
                }

                if (mi instanceof JifMethodInstance) {
                    JifMethodInstance jmi = (JifMethodInstance) mi;
                    jmi.setReturnType(rewriteType(jmi.returnType()));

                    jmi.setThrowTypes(throwTypes);
                    jmi.setFormalTypes(formalTypes);
                    jmi.setConstraints(constraints);
                    md = ((JifMethodDecl) md).methodInstance(jmi);
                } else if (mi instanceof JifConstructorInstance) {
                    JifConstructorInstance jci = (JifConstructorInstance) mi;

                    jci.setThrowTypes(throwTypes);
                    jci.setFormalTypes(formalTypes);
                    jci.setConstraints(constraints);
                    md = ((JifConstructorDecl) md).constructorInstance(jci);
                }

                return md;
            }

            return n;
        } catch (SemanticException e) {
            throw new InternalCompilerError(
                    "Unexpected SemanticException " + "thrown", e);
        }
    }

    /**
     * Replace the args in the label of type nodes.
     */
    public TypeNode rewriteTypeNode(TypeNode tn) throws SemanticException {
        Type t = tn.type();
        return tn.type(rewriteType(t));
    }

    public Expr rewriteExpr(Expr e) throws SemanticException {
        Type t = e.type();
        return e.type(rewriteType(t));
    }

    private Type rewriteType(Type t) throws SemanticException {
        return typeSubstitutor.rewriteType(t);
    }

    protected <P extends ActsForParam> P rewriteActsForParam(P param)
            throws SemanticException {
        if (param == null) return param;
        @SuppressWarnings("unchecked")
        P result = (P) param.subst(substitution).simplify();
        return result;
    }

    protected Label rewriteLabel(Label L) throws SemanticException {
        return rewriteActsForParam(L);
    }

    protected Principal rewritePrincipal(Principal p) throws SemanticException {
        return rewriteActsForParam(p);
    }

    protected List<Principal> rewritePrincipals(List<Principal> principals)
            throws SemanticException {
        List<Principal> result = new ArrayList<>(principals.size());
        for (Principal p : principals) {
            result.add(rewritePrincipal(p));
        }
        return result;
    }

    /**
     * Imperatively rewrites the given field instance.
     */
    protected void rewriteFieldInstance(JifFieldInstance fi)
            throws SemanticException {
        fi.setType(rewriteType(fi.type()));
        fi.setLabel(rewriteLabel(fi.label()));
    }

    protected <Actor extends ActsForParam, Granter extends ActsForParam> ActsForConstraint<Actor, Granter> rewriteActsForConstraint(
            ActsForConstraint<Actor, Granter> afc) throws SemanticException {
        Actor actor = rewriteActsForParam(afc.actor());
        Granter granter = rewriteActsForParam(afc.granter());
        return afc.actor(actor).granter(granter);
    }

    protected Assertion rewriteConstraint(Assertion c)
            throws SemanticException {
        if (c instanceof ActsForConstraint<?, ?>) {
            return rewriteActsForConstraint((ActsForConstraint<?, ?>) c);
        }

        if (c instanceof AuthConstraint) {
            AuthConstraint ac = (AuthConstraint) c;
            return ac.principals(rewritePrincipals(ac.principals()));
        }

        if (c instanceof AutoEndorseConstraint) {
            AutoEndorseConstraint aec = (AutoEndorseConstraint) c;
            return aec.endorseTo(rewriteLabel(aec.endorseTo()));
        }

        if (c instanceof CallerConstraint) {
            CallerConstraint cc = (CallerConstraint) c;
            return cc.principals(rewritePrincipals(cc.principals()));
        }

        if (c instanceof LabelLeAssertion) {
            LabelLeAssertion lla = (LabelLeAssertion) c;
            Label lhs = rewriteLabel(lla.lhs());
            Label rhs = rewriteLabel(lla.rhs());
            return lla.lhs(lhs).rhs(rhs);
        }

        throw new InternalCompilerError(
                "Unexpected subclass of Assertion: " + c.getClass());
    }
}
