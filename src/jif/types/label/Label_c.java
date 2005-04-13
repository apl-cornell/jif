package jif.types.label;

import java.util.*;

import jif.translate.*;
import jif.types.*;
import jif.types.JifTypeSystem;
import polyglot.ast.Expr;
import polyglot.ext.jl.types.TypeObject_c;
import polyglot.types.SemanticException;
import polyglot.types.TypeObject;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

/** 
 * An abstract implementation of the <code>Label</code> interface. 
 */
public abstract class Label_c extends TypeObject_c implements Label
{
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

    public final boolean hasVariables() {
        return !variables().isEmpty();
    }

    public Set variables() {
        return Collections.EMPTY_SET;
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
    public Collection components() {
        return Collections.singleton(this);
    }
    //
    /**
     * By default, a label is not Bottom
     */
    public boolean isBottom() { return false; }    
    public boolean isTop() { return false; }
    public boolean isInvariant() { return ! isCovariant(); }
    
    /**
     * A label is a singleton if it only has a single component.
     */
    public boolean isSingleton() {
        return components().size() <= 1;
    }
    
    /**
     * Return the single component.
     * 
     * @throws InternalCompilerError if this label is not a singleton.
     */
    public Label singletonComponent() {
        if (! isSingleton()) {
            throw new InternalCompilerError(
            "Cannot get singleton component of a non-singleton label.");
        }
        
        if (isBottom()) 
            return this;
        else
            return (Label)components().toArray()[0];
    }
    
    /**
     * Join this label to L. The result is generally a JoinLabel
     */
    public final Label join(Label L) {
        return ((JifTypeSystem) ts).join(this, L);
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

    public Label simplify() {
        return this; 
    }

    public Label subst(LabelSubstitution substitution) throws SemanticException {
        return substitution.substLabel(this); 
    }
}
