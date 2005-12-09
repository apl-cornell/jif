package jif.types.label;

import java.util.*;

import jif.translate.*;
import jif.types.*;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.ast.Expr;
import polyglot.ext.jl.types.TypeObject_c;
import polyglot.types.*;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

/**
 * An abstract implementation of the <code>Label</code> interface.
 */
public abstract class LabelJ_c extends TypeObject_c implements LabelJ {
    protected LabelJToJavaExpr_c toJava;

    protected LabelJ_c() {
        super();
    }

    public LabelJ_c(JifTypeSystem ts, Position pos) {
        super(ts, pos);
        this.toJava = new LabelJToJavaExpr_c();
    }

    public abstract Expr toJava(JifToJavaRewriter rw) throws SemanticException;

    public boolean isBottom() { return false; }
    public boolean isTop() { return false; }

    /**
     * Check if the label is disambiguated, without recursing into child labels.
     */
    protected abstract boolean isDisambiguatedImpl();
    
    public final boolean isDisambiguated() {
        final boolean[] result = new boolean[1];
        result[0] = true;
        try {
            this.subst(new LabelSubstitution() {
                public LabelJ substLabelJ(LabelJ L) throws SemanticException {
                    if (result[0] && L instanceof LabelJ_c) {
                        result[0] = ((LabelJ_c)L).isDisambiguatedImpl();
                    }
                    return L;
                }
      });
        }
        catch (SemanticException e) {
            throw new InternalCompilerError("Unexpected semantic exception", e);
        }
        return result[0];
    }

    public LabelJ simplify() {
        return this;
    }
    public String toString() {
        return "{" + componentString(new HashSet()) + "}";
    }

    public String toString(Set printedLabels) {
        return "{" + componentString(printedLabels) + "}";
    }

    public String componentString() {
        return componentString(new HashSet());
    }

    abstract public String componentString(Set printedLabels);

    public abstract boolean equalsImpl(TypeObject t);

    public List throwTypes(TypeSystem ts) {
        return Collections.EMPTY_LIST;
    }

    public LabelJ subst(LabelSubstitution substitution) throws SemanticException {
        return substitution.substLabelJ(this);
    }

    public PathMap labelCheck(JifContext A, LabelChecker lc) {
        JifTypeSystem ts = (JifTypeSystem)A.typeSystem();
        return ts.pathMap().N(A.pc()).NV(A.pc());
    }

}
