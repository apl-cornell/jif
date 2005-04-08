package jif.types.label;

import polyglot.main.Report;
import polyglot.types.*;
import polyglot.types.ClassType;
import polyglot.types.Resolver;
import polyglot.util.InternalCompilerError;

/**
 * TODO Documentation
 * Represent a final access path rooted at a class, e.g. Foo.f.
 */
public class AccessPathClass extends AccessPathRoot {
    private ClassType ct;
    public AccessPathClass(ClassType ct) {
        this.ct = ct;
    }
    
    public boolean isCanonical() { return true; }
    public String translate(Resolver c) { return null; }
    public AccessPath subst(AccessPathRoot r, AccessPath e) {
        return this;
    }
    
    public String toString() {
        if (Report.should_report(Report.debug, 2)) { 
            return ct.fullName();
        }
        return ct.name();
    }
    
    public boolean equals(Object o) {
        if (o instanceof AccessPathClass) {
            return ct.equals(((AccessPathClass)o).ct);
        }
        return false;        
    }

    public Type type() {
        return ct;
    }
}
