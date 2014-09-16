package jif.extension;

import java.util.ArrayList;
import java.util.List;

import jif.ast.JifSingletonAccess;
import jif.ast.JifNew_c;
import jif.translate.ToJavaExt;
import jif.types.JifClassType;
import jif.types.JifContext;
import jif.types.JifFieldInstance;
import jif.types.JifProcedureInstance;
import jif.types.JifTypeSystem;
import jif.types.LabelConstraint;
import jif.types.NamedLabel;
import jif.types.Param;
import jif.types.PathMap;
import jif.types.label.AccessPathField;
import jif.types.label.Label;
import jif.types.principal.DynamicPrincipal;
import jif.types.principal.Principal;
import jif.visit.JifTypeChecker;
import jif.visit.LabelChecker;
import jif.visit.SingletonChecker;
import polyglot.ast.Expr;
import polyglot.ast.New;
import polyglot.ast.Node;
import polyglot.ast.Special;
import polyglot.types.ClassType;
import polyglot.types.FieldInstance;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.InternalCompilerError;
import polyglot.util.SerialVersionUID;
import polyglot.visit.NodeVisitor;
import polyglot.visit.TypeChecker;

/** The Jif extension of the <code>New</code> node.
 * 
 *  @see polyglot.ast.New
 */
public class JifNewExt extends JifExprExt {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifNewExt(ToJavaExt toJava) {
        super(toJava);
    }

    protected ConstructorChecker constructorChecker = new ConstructorChecker();

    @Override
    public New node() {
        return (New) super.node();
    }

    @Override
    public NodeVisitor typeCheckEnter(TypeChecker tc) throws SemanticException {
        JifTypeChecker jtc =
                (JifTypeChecker) superLang().typeCheckEnter(node(), tc);
        return jtc.inferClassParameters(true);
    }

    @Override
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        JifNew_c n = (JifNew_c) superLang().typeCheck(node(), tc);

        Type t = n.objectType().type();
        LabelTypeCheckUtil ltcu =
                ((JifTypeSystem) tc.typeSystem()).labelTypeCheckUtil();
        ltcu.typeCheckType(tc, t);

        n = (JifNew_c) n.type(t);

        return n;
    }

    @Override
    public List<Type> throwTypes(TypeSystem ts) {
        New node = node();
        List<Type> ex = new ArrayList<Type>(superLang().throwTypes(node(), ts));
        LabelTypeCheckUtil ltcu = ((JifTypeSystem) ts).labelTypeCheckUtil();

        if (node.objectType().type() instanceof JifClassType) {
            ex.addAll(ltcu.throwTypes((JifClassType) node.objectType().type()));
        }
        return ex;
    }

    @Override
    public Node labelCheck(LabelChecker lc) throws SemanticException {
        New noe = node();

        JifTypeSystem ts = lc.typeSystem();
        JifContext A = lc.jifContext();
        A = (JifContext) noe.del().enterScope(A);

        List<Type> throwTypes = new ArrayList<Type>(noe.del().throwTypes(ts));

        ClassType ct = (ClassType) ts.unlabel(noe.type());

        // If there are any final static labels/principals with initializers in rt,
        // add them to the env.
        if (ct != null && ct.fields() != null) {
            for (FieldInstance fi : ct.fields()) {
                JifFieldInstance jfi = (JifFieldInstance) fi;
                if (jfi.flags().isFinal() && jfi.flags().isStatic()
                        && jfi.hasInitializer()) {
                    AccessPathField path =
                            (AccessPathField) ts.varInstanceToAccessPath(jfi,
                                    jfi.position());
                    Param init = jfi.initializer();
                    if (ts.isLabel(jfi.type())) {
                        Label dl = ts.dynamicLabel(jfi.position(), path);
                        Label rhs_label = (Label) init;
                        if (rhs_label == null) {
                            throw new InternalCompilerError(
                                    "FinalParams has not run yet");
                        }
                        A.addDefinitionalAssertionEquiv(dl, rhs_label, true);
                    } else if (ts.isImplicitCastValid(jfi.type(),
                            ts.Principal())) {
                        DynamicPrincipal dp =
                                ts.dynamicPrincipal(jfi.position(), path);
                        Principal rhs_principal = (Principal) init;
                        if (rhs_principal == null) {
                            throw new InternalCompilerError(
                                    "FinalParams has not run yet");
                        }
                        A.addDefinitionalEquiv(dp, rhs_principal);
                    }
                }
            }
        }

        constructorChecker.checkConstructorAuthority(ct, A, lc, noe.position());

        Label newLabel = null;
        boolean npExc = false;
        if (noe.qualifier() == null) {
            newLabel =
                    ts.freshLabelVariable(
                            noe.position(),
                            "new" + ct.name(),
                            "label of the reference to the newly created "
                                    + ct.name() + " object, at "
                                    + noe.position());
        } else {
            // labelcheck qualifier like the target of a method call.
            Expr e = (Expr) lc.labelCheck(noe.qualifier());

            if (e.type() == null)
                throw new InternalCompilerError("Type of " + e + " is null",
                        e.position());

            PathMap Xs = getPathMap(e);
            if (Xs == null)
                throw new InternalCompilerError("No entry for " + e);
            A.setPc(Xs.N(), lc);

            if (!(e instanceof Special)) {
                // TODO: a NPE may be thrown depending on the qualifier.
                //       for now, assume the qualifier may be null.
                npExc = (!((JifNewDel) node().del()).qualIsNeverNull());
                newLabel = Xs.NV();
                A.setPc(Xs.NV(), lc);
            } else {
                newLabel =
                        ((JifClassType) lc.context().currentClass())
                                .thisLabel();
            }
        }
        if (ts.isLabeled(noe.type())) {
            // error messages for equality constraints aren't displayed, so no
            // need to define error messages.
            lc.constrain(new NamedLabel("new_label",
                    "label of the reference to the newly created " + ct.name(),
                    newLabel), LabelConstraint.EQUAL, new NamedLabel(
                    "declared_label", "declared label of the newly created "
                            + ct.name(), ts.labelOfType(noe.type())), A
                    .labelEnv(), noe.position());
        }

        CallHelper helper =
                lc.createCallHelper(newLabel, noe, ct,
                        (JifProcedureInstance) noe.constructorInstance(),
                        noe.arguments(), node().position());
        LabelChecker callLC = lc.context(A);
        helper.checkCall(callLC, throwTypes, npExc);

        PathMap retX = helper.X();
        PathMap X = retX.NV(lc.upperBound(retX.NV(), newLabel));

        checkThrowTypes(throwTypes);
        return updatePathMap(noe.arguments(helper.labelCheckedArgs()), X);
    }

    @Override
    public void checkSingletons(SingletonChecker sc) throws SemanticException {
        New jcd = (New) node();
        JifClassType jct = (JifClassType) jcd.type();
        if (jcd instanceof JifSingletonAccess) {
            if (!jct.isSingleton()) {
                throw new SemanticException(
                        "Classes cannot be instantiated with the keyword single.");
            }
            return;
        }
        if (jct.isSingleton()) {
            throw new SemanticException("Singletons cannot be instantiated.");
        }
    }

}
