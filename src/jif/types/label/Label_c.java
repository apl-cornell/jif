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
public abstract class Label_c extends TypeObject_c implements Label {
    protected String description;

    protected LabelToJavaExpr toJava;

    protected Label_c() {
        super();
    }

    public Label_c(JifTypeSystem ts, Position pos, LabelToJavaExpr toJava) {
        super(ts, pos);
        this.toJava = toJava;
    }

    public Label_c(JifTypeSystem ts, Position pos) {
        this(ts, pos, new CannotLabelToJavaExpr_c());
    }

    public String description() {
        return description;
    }

    public void setDescription(String d) {
        this.description = d;
    }

    public boolean hasVariableComponents() {
        return !variableComponents().isEmpty();
    }

    public final boolean hasVariables() {
        return !variables().isEmpty();
    }

    public boolean hasWritersToReaders() {
        return false;
    }

    public final Set variables() {
        LabelVarGatherer lvg = new LabelVarGatherer();
        try {
            this.subst(lvg);
        }
        catch (SemanticException e) {
            throw new InternalCompilerError(e);
        }
        return lvg.variables;
    }

    public Expr toJava(JifToJavaRewriter rw) throws SemanticException {
        return toJava.toJava(this, rw);
    }

    //    /**
    //     * By default, a label is enumerable
    //     */
    //    public boolean isEnumerable() {
    //	return true;
    //    }
    //
    /**
     * By default, the components of a label is simply the label itself.
     */
//    public Collection components() {
//        return Collections.singleton(this);
//    }

    //
    /**
     * By default, a label is not Bottom
     */
    public boolean isBottom() {
        return false;
    }

    public boolean isTop() {
        return false;
    }

    public boolean isInvariant() {
        return !isCovariant();
    }

//    /**
//     * A label is a singleton if it only has a single component.
//     */
//    public boolean isSingleton() {
//        return components().size() <= 1;
//    }
//
//    /**
//     * Return the single component.
//     * 
//     * @throws InternalCompilerError if this label is not a singleton.
//     */
//    public Label singletonComponent() {
//        if (!isSingleton()) {
//            throw new InternalCompilerError(
//                    "Cannot get singleton component of a non-singleton label.");
//        }
//
//        if (isBottom())
//            return this;
//        else
//            return (Label)components().toArray()[0];
//    }
    
    /**
     * Check if the label is disambiguated, without recursing into child labels.
     */
    protected abstract boolean isDisambiguatedImpl();
    
    public final boolean isDisambiguated() {
        final boolean[] result = new boolean[1];
        result[0] = true;
        try {
            this.subst(new LabelSubstitution() {
                public Label substLabel(Label L) throws SemanticException {
                    if (result[0] && L instanceof Label_c) {
                        result[0] = ((Label_c)L).isDisambiguatedImpl();
                    }
                    return L;
                }
                public Principal substPrincipal(Principal p) throws SemanticException {
                    return p;
                }    
      });
        }
        catch (SemanticException e) {
            throw new InternalCompilerError("Unexpected semantic exception", e);
        }
        return result[0];
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

    public final Label simplify() {
        return ((Label_c)this.normalize()).simplifyImpl();
    }
    protected Label simplifyImpl() {
        return this;
    }
    
    public Label normalize() {
        return this;
    }

    public ConfPolicy confProjection() {
        return ((JifTypeSystem)ts).confProjection(this);
    }

    public IntegPolicy integProjection() {
        return ((JifTypeSystem)ts).integProjection(this);
    }

    public List throwTypes(TypeSystem ts) {
        return Collections.EMPTY_LIST;
    }

    public Label subst(LabelSubstitution substitution) throws SemanticException {
        return substitution.substLabel(this);
    }

    public PathMap labelCheck(JifContext A, LabelChecker lc) {
        JifTypeSystem ts = (JifTypeSystem)A.typeSystem();
        return ts.pathMap().N(A.pc()).NV(A.pc());
    }

    public Set variableComponents() {
        return Collections.EMPTY_SET;
    }

    /**
     * This class is used to implement
     * {@link Label#variables() Label.variables()}. It constructs a set of
     * <code>VarLabel</code>s.
     */
    private static class LabelVarGatherer extends LabelSubstitution {
        private final Set variables = new LinkedHashSet();

        public Label substLabel(Label L) throws SemanticException {
            if (L instanceof VarLabel) {
                variables.add(L);
    }
            return L;
        }
    
    }
    
}