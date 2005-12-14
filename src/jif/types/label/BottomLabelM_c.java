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

/** An implementation of the <code>BottomLabelJ</code> interface. 
 */
public class BottomLabelM_c extends LabelM_c implements BottomLabelM {
    protected BottomLabelM_c() {
    }
    
    public BottomLabelM_c(JifTypeSystem ts, Position pos) {
        super(ts, pos);
    }
    
    public boolean isBottom() { return true; }    
    public boolean isCanonical() { return true; }    
    public boolean isDisambiguatedImpl() { return true; }     
    public boolean isRuntimeRepresentable() { return false; }
    
    public String componentString(Set printedLabels) {
        return "<bot integ>";
    }    
    public boolean equalsImpl(TypeObject o) {
        return o instanceof BottomLabelM;
    }    
    public int hashCode() { return 8934; }
    public boolean leq_(LabelM L, LabelEnv env) {
        return true;
    }
    public Expr toJava(JifToJavaRewriter rw) throws SemanticException {
        return toJava.toJava(this, rw);               
    }        
}
