package jif.types;

import java.util.Iterator;
import java.util.LinkedHashMap;
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
        if (t instanceof LabeledType && recurseIntoLabeledType((LabeledType)t)) {
            LabeledType lt = (LabeledType)t;
            Label L = lt.labelPart();
            Type bt = lt.typePart();
            return lt.labelPart(rewriteLabel(L)).typePart(rewriteType(bt));
        }
        else if (t instanceof ArrayType && recurseIntoArrayType((ArrayType)t)) {
            ArrayType at = (ArrayType)t;
            return at.base(rewriteType(at.base()));
        }
        else if (t instanceof JifSubstType && recurseIntoSubstType((JifSubstType)t)) {
            JifSubstType jst = (JifSubstType)t;
            Map newMap = new LinkedHashMap();
            boolean diff = false;

            for (Iterator i = jst.entries(); i.hasNext();) {
                Map.Entry e = (Map.Entry)i.next();
                Object arg = e.getValue();
                Param p;
                if (arg instanceof Label) {
                    p = (Label)rewriteLabel((Label)arg);
                }
                else if (arg instanceof Principal) {
                    p = (Principal)rewritePrincipal((Principal)arg);
                }
                else {
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
                JifTypeSystem ts = (JifTypeSystem)t.typeSystem();
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

    protected Label rewriteLabel(Label L) throws SemanticException {
        if (L == null) return L;
        return L.subst(substitution);
    }
    protected Principal rewritePrincipal(Principal p) throws SemanticException {
        if (p == null) return p;
        return p.subst(substitution);
    }
}
