package jif.types.label;

import polyglot.main.Report;
import polyglot.types.Type;
import polyglot.util.Position;

/**
 * TODO Documentation
 * Represent a final access path rooted at "this".
 */
public class AccessPathUninterpreted extends AccessPathRoot {
    public AccessPathUninterpreted(Position pos) {
        super(pos);
    }
    
    public boolean isCanonical() { return true; }
    public AccessPath subst(AccessPathRoot r, AccessPath e) {
        return e;
    }
    
    public String toString() {
        if (Report.should_report(Report.debug, 1)) { 
            return "<uninterpreted path>";
        }
        return "<>";
    }
    
    public boolean equals(Object o) {
        return this == o;
    }

    public int hashCode() {
        return System.identityHashCode(this);
    }
    public Type type() {
        return null;
    }
}