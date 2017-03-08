package jif.types.label;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jif.translate.CannotLabelToJavaExpr_c;
import jif.translate.JifToJavaRewriter;
import jif.translate.LabelToJavaExpr;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.LabelSubstitution;
import jif.types.Param_c;
import jif.types.PathMap;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/**
 * An abstract implementation of the <code>Label</code> interface.
 */
public abstract class Label_c extends Param_c implements Label {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected String description;

    protected LabelToJavaExpr toJava;

    protected Set<Variable> variables = null; // memoized

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

    @Override
    public Label_c copy() {
        Label_c l = (Label_c) super.copy();
        l.variables = null;
        l.simplified = null;
        return l;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public void setDescription(String d) {
        this.description = d;
    }

    @Override
    public boolean hasVariableComponents() {
        return !variableComponents().isEmpty();
    }

    @Override
    public final boolean hasVariables() {
        return !variables().isEmpty();
    }

    @Override
    public boolean hasWritersToReaders() {
        return false;
    }

    @Override
    public Set<Variable> variables() {
        if (variables == null) {
            VariableGatherer lvg = new VariableGatherer();
            try {
                this.subst(lvg);
            } catch (SemanticException e) {
                throw new InternalCompilerError(e);
            }
            variables = lvg.variables;
        }
        return variables;
    }

    @Override
    public Expr toJava(JifToJavaRewriter rw, Expr thisQualifier)
            throws SemanticException {
        return toJava(rw, thisQualifier, true);
    }

    @Override
    public Expr toJava(JifToJavaRewriter rw, Expr thisQualifier,
            boolean simplify) throws SemanticException {
        return toJava.toJava(this, rw, thisQualifier, simplify);
    }

    //
    /**
     * By default, a label is not Bottom
     */
    @Override
    public boolean isBottom() {
        return false;
    }

    @Override
    public boolean isTop() {
        return false;
    }

    @Override
    public boolean isInvariant() {
        return !isCovariant();
    }

    /**
     * Check if the label is disambiguated, without recursing into child labels.
     */
    protected abstract boolean isDisambiguatedImpl();

    @Override
    public final boolean isDisambiguated() {
        final boolean[] result = new boolean[1];
        result[0] = true;
        try {
            this.subst(new LabelSubstitution() {
                @Override
                public Label substLabel(Label L) {
                    if (result[0] && L instanceof Label_c) {
                        result[0] = ((Label_c) L).isDisambiguatedImpl();
                    }
                    return L;
                }

                @Override
                public Principal substPrincipal(Principal p) {
                    return p;
                }
            });
        } catch (SemanticException e) {
            throw new InternalCompilerError("Unexpected semantic exception", e);
        }
        return result[0];
    }

    @Override
    public String toString() {
        return toString(new HashSet<Label>());
    }

    @Override
    public String toString(Set<Label> printedLabels) {
        return "{" + componentString(printedLabels) + "}";
    }

    @Override
    public String componentString() {
        return componentString(new HashSet<Label>());
    }

    @Override
    abstract public String componentString(Set<Label> printedLabels);

    @Override
    public abstract boolean equalsImpl(TypeObject t);

    private Label simplified = null;

    @Override
    public final Label simplify() {
        // memoize the result
        if (simplified == null) {
            simplified = ((Label_c) this.normalize()).simplifyImpl();
        }
        return simplified;
    }

    protected Label simplifyImpl() {
        return this;
    }

    @Override
    public Label normalize() {
        return this;
    }

    @Override
    public ConfPolicy confProjection() {
        return ((JifTypeSystem) ts).confProjection(this);
    }

    @Override
    public IntegPolicy integProjection() {
        return ((JifTypeSystem) ts).integProjection(this);
    }

    @Override
    public List<Type> throwTypes(TypeSystem ts) {
        return Collections.emptyList();
    }

    @Override
    public Label subst(LabelSubstitution substitution)
            throws SemanticException {
        return substitution.substLabel(this);
    }

    @Override
    public PathMap labelCheck(JifContext A, LabelChecker lc) {
        JifTypeSystem ts = (JifTypeSystem) A.typeSystem();
        return ts.pathMap().N(A.pc()).NV(A.pc());
    }

    @Override
    public Set<Variable> variableComponents() {
        return Collections.emptySet();
    }

}
