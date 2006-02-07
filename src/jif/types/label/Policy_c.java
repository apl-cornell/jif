package jif.types.label;

import java.util.HashSet;
import java.util.Set;

import jif.types.JifTypeSystem;
import polyglot.ext.jl.types.TypeObject_c;
import polyglot.util.Position;

/** An implementation of the <code>PolicyLabel</code> interface. 
 */
public abstract class Policy_c extends TypeObject_c implements Policy {
    public Policy_c(JifTypeSystem ts, Position pos) {
        super(ts, pos); 
    }    
    
    public final String toString() {
        return toString(new HashSet());
    }
    
    abstract public String toString(Set printedLabels);
    
}
