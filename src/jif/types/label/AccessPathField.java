package jif.types.label;

import jif.ast.JifInstantiator;
import jif.types.*;
import jif.types.JifContext;
import jif.types.PathMap;
import polyglot.ast.Expr;
import polyglot.ast.Field;
import polyglot.types.*;
import polyglot.types.FieldInstance;
import polyglot.types.Resolver;

/**
 * TODO Documentation
 * Represent a final access path 
 */
public class AccessPathField extends AccessPath {
    private FieldInstance fi;
    private AccessPath path;

    public AccessPathField(AccessPath path, FieldInstance fi) {
        this.fi = fi;
        this.path = path;
    }
    
    public boolean isCanonical() { return path.isCanonical(); }
    public AccessPath subst(AccessPathRoot r, AccessPath e) {
        AccessPath newPath = path.subst(r, e);
        if (newPath == path) return this;
        
        return new AccessPathField(newPath, fi);
    }
    public String toString() {
        return path + "." + fi.name();
    }
    public boolean equals(Object o) {
        if (o instanceof AccessPathField) {
            AccessPathField that = (AccessPathField)o; 
            return this.fi.name().equals(that.fi.name()) && this.path.equals(that.path);
        }
        return false;        
    }
    public int hashCode() {
        return path.hashCode() + fi.name().hashCode();
    }

    public Type type() {
        return fi.type();
    }

    public PathMap labelcheck(JifContext A) {
    	PathMap Xt = path.labelcheck(A);

    	JifTypeSystem ts = (JifTypeSystem)A.typeSystem();    	

        // null pointer exception may be thrown.
    	// TODO: take into account not-null checking somehow
        PathMap X = Xt.exc(Xt.NV(), ts.NullPointerException());
    	
        Label L = ts.labelOfField(fi, A.pc());
        Label objLabel = Xt.NV();
        L = JifInstantiator.instantiate(L, A, path, path.type().toReference(), Xt.NV());

        X = X.NV(L.join(X.NV()));
        return X;
    }
}
