package jif.types.label;

import jif.types.*;
import polyglot.types.*;
import polyglot.types.LocalInstance;
import polyglot.types.Type;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

/**
 * TODO Documentation
 * Represent a final access path rooted at a local variable.
 */
public class AccessPathLocal extends AccessPathRoot {
    private LocalInstance li;
    private String name;
    public AccessPathLocal(LocalInstance li, String name, Position pos) {
        super(pos);
        this.li = li;
        this.name = name;
        if (li != null && !name.equals(li.name())) {
            throw new InternalCompilerError("Inconsistent local names");
        }
    }
    
    public boolean isCanonical() { return li.isCanonical(); }
    public AccessPath subst(AccessPathRoot r, AccessPath e) {
        if (r instanceof AccessPathLocal) {            
            if (li.equals(((AccessPathLocal)r).li)) {
                return e;
            }
        }
        return this;
    }
    
    public String toString() {
        return name;
    }
    public String exprString() {
        return name;
    }
    
    public boolean equals(Object o) {
        if (o instanceof AccessPathLocal) {
            return li.equals(((AccessPathLocal)o).li);
        }
        return false;        
    }

    public int hashCode() {
        return name.hashCode();
    }
    public Type type() {
        if (li == null) return null;
        return li.type();
    }

    public PathMap labelcheck(JifContext A) {
    	JifTypeSystem ts = (JifTypeSystem)A.typeSystem();
    	Label L = ts.labelOfLocal(li, A.pc());

    	PathMap X = ts.pathMap();
    	X = X.N(A.pc());
    	X = X.NV(L.join(A.pc()));
        
        return X;
    }
    public void verify(JifContext A) throws SemanticException {
        if (li == null) {
            li = A.findLocal(name);
        }
        else {
            if (!li.equals(A.findLocal(name))) {
                throw new InternalCompilerError("Unexpected local instance for name " + name);
            }
        }
        if (!li.flags().isFinal()) {
            throw new SemanticException("Non-final local variable used in access path", position());
        }
    }
}
