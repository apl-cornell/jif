package jif.types.label;

import polyglot.main.Report;
import polyglot.types.Resolver;
import polyglot.types.Type;

/**
 * TODO Documentation
 * Represent a final access path rooted at "this".
 */
public class AccessPathUninterpreted extends AccessPathRoot {
    public AccessPathUninterpreted() {
    }
    
    public boolean isCanonical() { return true; }
    public String translate(Resolver c) { return null; }
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

    public Type type() {
        return null;
    }
}
