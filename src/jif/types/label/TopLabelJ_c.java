package jif.types.label;


import java.util.Set;

import jif.translate.JifToJavaRewriter;
import jif.types.JifTypeSystem;
import jif.types.hierarchy.LabelEnv;
import polyglot.ast.Expr;
import polyglot.types.*;
import polyglot.types.Resolver;
import polyglot.types.TypeObject;
import polyglot.util.*;

/** An implementation of the <code>TopLabelJ</code> interface. 
 */
public class TopLabelJ_c extends LabelJ_c implements TopLabelJ {
    protected TopLabelJ_c() {
    }
    
    public TopLabelJ_c(JifTypeSystem ts, Position pos) {
        super(ts, pos);
    }
    
    public boolean isTop() { return true; }    
    public boolean isComparable() { return true; }    
    public boolean isEnumerable() { return false; }    
    public boolean isCanonical() { return true; }    
    public boolean isDisambiguatedImpl() { return true; }     
    public boolean isRuntimeRepresentable() { return false; }
    public boolean isCovariant() { return false; }
    
    public String componentString(Set printedLabels) {
        return "<top>";
    }    
    public String toString() {
        return "<top>";
    }    
    public boolean equalsImpl(TypeObject o) {
        return o instanceof TopLabelJ;
    }    
    public int hashCode() { return 390230; }
    public boolean leq_(LabelJ L, LabelEnv env) {
        return L.isTop();
    }

    public Expr toJava(JifToJavaRewriter rw) throws SemanticException {
        throw new InternalCompilerError("Cannot to java");
    }
}
