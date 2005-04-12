package jif.types.label;

import jif.types.*;
import polyglot.main.Report;
import polyglot.types.*;
import polyglot.types.ClassType;
import polyglot.types.Type;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

/**
 * TODO Documentation
 * Represent a final access path rooted at "this".
 */
public class AccessPathThis extends AccessPathRoot {
    private ClassType ct;
    /**
     * 
     * @param ct may be null.
     */
    public AccessPathThis(ClassType ct, Position pos) {
        super(pos);
        this.ct = ct;
    }
    
    public boolean isCanonical() { return true; }
    public AccessPath subst(AccessPathRoot r, AccessPath e) {
        if (r instanceof AccessPathThis) {            
            if (this.equals(r)) {
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
        String name = "<not-typechecked>";
        if (Report.should_report(Report.debug, 2)) { 
            if (ct != null) name = ct.fullName();
            return "this(of " + name + ")";
        }
        if (Report.should_report(Report.debug, 1)) { 
            if (ct != null) name = ct.name();
            return "this(of " + name + ")";
        }
        return "this";
    }
    
    public String exprString() {
        return "this";
    }
    public boolean equals(Object o) {
        if (o instanceof AccessPathThis) {
            if (ct == null) return true;
            return ct.equals(((AccessPathThis)o).ct);
        }
        return false;        
    }

    public int hashCode() {
        if (ct != null) return ct.hashCode();
        return -572309;
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
    public void verify(JifContext A) throws SemanticException {
        if (ct == null) {
            ct = A.currentClass();
        }
        else {
            if (!ct.equals(A.currentClass())) {
                throw new InternalCompilerError("Unexpected class type for access path this");
            }
        }
    }
}
