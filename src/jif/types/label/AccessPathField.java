package jif.types.label;

import jif.ast.JifInstantiator;
import jif.types.*;
import polyglot.types.*;
import polyglot.types.FieldInstance;
import polyglot.types.Type;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

/**
 * TODO Documentation
 * Represent a final access path 
 */
public class AccessPathField extends AccessPath {
    private FieldInstance fi;
    private String fieldName;
    private AccessPath path;

    public AccessPathField(AccessPath path, FieldInstance fi, String fieldName, Position pos) {
        super(pos);
        this.fi = fi;
        this.path = path;
        this.fieldName = fieldName;
        if (fi != null && !fieldName.equals(fi.name())) {
            throw new InternalCompilerError("Inconsistent field names");
        }
    }
    
    public boolean isCanonical() { return path.isCanonical(); }
    public AccessPath subst(AccessPathRoot r, AccessPath e) {
        AccessPath newPath = path.subst(r, e);
        if (newPath == path) return this;
        
        return new AccessPathField(newPath, fi, fieldName, position());
    }
    public String toString() {
        return path + "." + fieldName;
    }
    public boolean equals(Object o) {
        if (o instanceof AccessPathField) {
            AccessPathField that = (AccessPathField)o; 
            return this.fieldName.equals(that.fieldName) && this.path.equals(that.path);
        }
        return false;        
    }
    public int hashCode() {        
        return path.hashCode() + fieldName.hashCode();
    }
    

    public Type type() {
        if (fi == null) return null;
        return fi.type();
    }

    public PathMap labelcheck(JifContext A) {
    	PathMap Xt = path.labelcheck(A);

    	JifTypeSystem ts = (JifTypeSystem)A.typeSystem();    	

        // null pointer exception may be thrown.
    	// TODO: take into account not-null checking somehow
        PathMap X = Xt.exc(Xt.NV(), ts.NullPointerException());
    	
        Label L = ts.labelOfField(fi, A.pc());
        L = JifInstantiator.instantiate(L, A, path, path.type().toReference(), Xt.NV());

        X = X.NV(L.join(X.NV()));
        return X;
    }
    public void verify(JifContext A) throws SemanticException {
        path.verify(A);
        if (!path.type().isReference()) {
            throw new SemanticException("Expression " + path + " used in final access path is not a reference type", position());
        }
        FieldInstance found = path.type().toReference().fieldNamed(fieldName); 
        if (fi == null || !fi.isCanonical()) {            
            fi = found;
        }
        else {
            if (!fi.equals(found)) {
                throw new InternalCompilerError("Unexpected field instance for name " + fieldName + ": original was " + fi + "; found was " + found);
            }
        }
        if (!fi.flags().isFinal()) {
            throw new SemanticException("Field " + fi.name() + " in access path is not final", position());
        }
    }
}
