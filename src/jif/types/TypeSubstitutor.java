package jif.types;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jif.types.label.Label;
import jif.types.principal.Principal;
import polyglot.types.ArrayType;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.InternalCompilerError;

/**
 * Visits an AST, and applies a <code>LabelSubsitution</code> to all labels
 * that occur in the AST. The <code>LabelSubsitution</code> is not allowed
 * to throw any <code>SemanticException</code>s.
 */
public class TypeSubstitutor {
    /**
     * The substitution to use.
     */
    private LabelSubstitution substitution;

    /**
     * 
     * @param substitution the LabelSubstitution to use.
     */
    public TypeSubstitutor(LabelSubstitution substitution) {
        this.substitution = substitution;
    }

    public Type rewriteType(Type t) throws SemanticException {
        if (t instanceof LabeledType
                && recurseIntoLabeledType((LabeledType) t)) {
            LabeledType lt = (LabeledType) t;
            Label L = lt.labelPart();
            Type bt = lt.typePart();
            return lt.labelPart(rewriteLabel(L)).typePart(rewriteType(bt));
        } else
            if (t instanceof ArrayType && recurseIntoArrayType((ArrayType) t)) {
            ArrayType at = (ArrayType) t;
            return at.base(rewriteType(at.base()));
        } else if (t instanceof JifSubstType
                && recurseIntoSubstType((JifSubstType) t)) {
            JifSubstType jst = (JifSubstType) t;
            Map<ParamInstance, Param> newMap =
                    new LinkedHashMap<ParamInstance, Param>();
            boolean diff = false;

            for (Iterator<Map.Entry<ParamInstance, Param>> i = jst.entries(); i
                    .hasNext();) {
                Map.Entry<ParamInstance, Param> e = i.next();
                Object arg = e.getValue();
                Param p;
                if (arg instanceof Label) {
                    p = rewriteLabel((Label) arg);
                } else if (arg instanceof Principal) {
                    p = rewritePrincipal((Principal) arg);
                } else {
                    throw new InternalCompilerError(
                            "Unexpected type for entry: "
                                    + arg.getClass().getName());
                }
                newMap.put(e.getKey(), p);

                if (p != arg) {
                    diff = true;
                }
            }
            if (diff) {
                JifTypeSystem ts = (JifTypeSystem) t.typeSystem();
                t = ts.subst(jst.base(), newMap);
                return t;
            }

        }
        return t;
    }

    protected boolean recurseIntoSubstType(JifSubstType type) {
        return true;
    }

    protected boolean recurseIntoArrayType(ArrayType type) {
        return true;
    }

    protected boolean recurseIntoLabeledType(LabeledType type) {
        return true;
    }

    public <P extends ActsForParam> P rewriteActsForParam(P param)
            throws SemanticException {
        if (param == null) return null;

        @SuppressWarnings("unchecked")
        P result = (P) param.subst(substitution);
        return result;
    }

    public Label rewriteLabel(Label L) throws SemanticException {
        return rewriteActsForParam(L);
    }

    protected Principal rewritePrincipal(Principal p) throws SemanticException {
        return rewriteActsForParam(p);
    }

    public <Actor extends ActsForParam, Granter extends ActsForParam> Assertion rewriteAssertion(
            Assertion a) throws SemanticException {
        if (a instanceof ActsForConstraint) {
            @SuppressWarnings("unchecked")
            ActsForConstraint<Actor, Granter> c =
                    (ActsForConstraint<Actor, Granter>) a.copy();
            c = c.actor(rewriteActsForParam(c.actor()));
            c = c.granter(rewriteActsForParam(c.granter()));
            return c;
        } else if (a instanceof AuthConstraint) {
            AuthConstraint c = (AuthConstraint) a.copy();
            c = c.principals(rewritePrincipalList(c.principals()));
            return c;
        } else if (a instanceof AutoEndorseConstraint) {
            AutoEndorseConstraint c = (AutoEndorseConstraint) a.copy();
            c = c.endorseTo(rewriteLabel(c.endorseTo()));
            return c;
        } else if (a instanceof CallerConstraint) {
            CallerConstraint c = (CallerConstraint) a.copy();
            c = c.principals(rewritePrincipalList(c.principals()));
            return c;
        } else if (a instanceof LabelLeAssertion) {
            LabelLeAssertion c = (LabelLeAssertion) a.copy();
            c = c.lhs(rewriteLabel(c.lhs()));
            c = c.rhs(rewriteLabel(c.rhs()));
            return c;
        }
        throw new InternalCompilerError("Unexpected assertion " + a);
    }

    private List<Principal> rewritePrincipalList(List<Principal> list)
            throws SemanticException {
        List<Principal> newList = new ArrayList<Principal>(list.size());
        for (Principal p : list) {
            newList.add(rewritePrincipal(p));
        }
        return newList;
    }
}
