package jif.types.label;

import jif.types.*;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.PathMap;
import polyglot.main.Report;
import polyglot.types.*;
import polyglot.types.ClassType;
import polyglot.types.Resolver;
import polyglot.util.InternalCompilerError;

/**
 * TODO Documentation
 * Represent a final access path rooted at "this".
 */
public class AccessPathThis extends AccessPathRoot {
    private ClassType ct;
    public AccessPathThis(ClassType ct) {
        this.ct = ct;
    }
    
    public boolean isCanonical() { return true; }
    public AccessPath subst(AccessPathRoot r, AccessPath e) {
        if (r instanceof AccessPathThis) {            
            if (ct.equals(((AccessPathThis)r).ct)) {
                return e;
            }
            else {
                throw new InternalCompilerError("Trying to replace \"this\" root of " + 
                           ((AccessPathThis)r).ct.fullName() + 
                           " in an access path expression rooted in " + 
                           this.ct.fullName());
            }
        }
        return this;
    }
    
    public String toString() {
        if (Report.should_report(Report.debug, 2)) { 
            return "this(of " + ct.fullName() + ")";
        }
        if (Report.should_report(Report.debug, 1)) { 
            return "this(of " + ct.name() + ")";
        }
        return "this";
    }
    
    public boolean equals(Object o) {
        if (o instanceof AccessPathThis) {
            return ct.equals(((AccessPathThis)o).ct);
        }
        return false;        
    }

    public int hashCode() {
        return ct.hashCode();
    }
    public Type type() {
        return ct;
    }

    public PathMap labelcheck(JifContext A) {
    	JifTypeSystem ts = (JifTypeSystem)A.typeSystem();
    	JifClassType ct = (JifClassType)A.currentClass();
    	

    	PathMap X = ts.pathMap();
    	X = X.N(A.pc());
    	
    	// X(this).NV = this_label, which is upper-bounded by the begin label. 
    	X = X.NV(ct.thisLabel().join(A.pc()));	    	
        return X;
    }
}
