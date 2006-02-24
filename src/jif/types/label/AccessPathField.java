package jif.types.label;

import java.util.ArrayList;
import java.util.List;

import jif.ast.JifInstantiator;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.PathMap;
import jif.visit.LabelChecker;
import polyglot.types.*;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

/**
 * Represent a final access path whose last element is a field access to a final
 * field, for example "p.f", where p is a final access path. 
 * @see jif.types.label.AccessPath
 */
public class AccessPathField extends AccessPath {
    protected FieldInstance fi;
    protected String fieldName;
    protected final AccessPath path;
    private boolean neverNull = false;

    public AccessPathField(AccessPath path, FieldInstance fi, String fieldName, Position pos) {
        super(pos);
        this.fi = fi;
        this.path = path;
        this.fieldName = fieldName;
        if (fi != null && !fieldName.equals(fi.name())) {
            throw new InternalCompilerError("Inconsistent field names");
        }
    }
    
    public boolean isNeverNull() {
        return neverNull;
    }
    public void setIsNeverNull() {
        this.neverNull = true;
    }

    public boolean isCanonical() { return path.isCanonical(); }
    public boolean isUninterpreted() { return path.isUninterpreted(); }
    public AccessPath subst(AccessPathRoot r, AccessPath e) {
        AccessPath newPath = path.subst(r, e);
        if (newPath == path) return this;
        
        return new AccessPathField(newPath, fi, fieldName, position());
    }
    
    public final AccessPathRoot root() {
        return path.root();
    }

    public String toString() {
        return path + "." + fieldName;
    }
    public String exprString() {
        return path.exprString() + "." + fieldName;
    }	
    public AccessPath path() {
        return this.path;
    }
    public FieldInstance fieldInstance() {
        return this.fi;
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

    public PathMap labelcheck(JifContext A, LabelChecker lc) {
    	PathMap Xt = path.labelcheck(A, lc);

    	JifTypeSystem ts = (JifTypeSystem)A.typeSystem();    	

    	PathMap X = Xt;
    	if (!isTargetNeverNull()) {    	    
    	    // null pointer exception may be thrown.
    	    X = Xt.exc(Xt.NV(), ts.NullPointerException());
    	}
    	
        Label L = ts.labelOfField(fi, A.pc());
        L = JifInstantiator.instantiate(L, A, path, path.type().toReference(), Xt.NV());

        X = X.NV(lc.upperBound(L, X.NV()));
        return X;
    }
    
    protected boolean isTargetNeverNull() {
        return path.isNeverNull();
    }
    public void verify(JifContext A) throws SemanticException {
        path.verify(A);
        if (!path.type().isReference()) {
            throw new SemanticException("Expression " + path + " used in final access path is not a reference type", position());
        }
        FieldInstance found = A.typeSystem().findField(path.type().toReference(), fieldName); 
        if (fi == null || !fi.isCanonical()) {            
            fi = found;
        }
        else {
            if (!fi.equals(found)) {
                throw new InternalCompilerError("Unexpected field instance for name " + fieldName + ": original was " + fi + "; found was " + found);
            }
        }
        if (fi == null) {
            throw new SemanticException("Field " + fieldName + " cannot be found in class " + path.type(), position());
        }
        if (!fi.flags().isFinal()) {
            throw new SemanticException("Field " + fi.name() + " in access path is not final", position());
        }
    }
    public List throwTypes(TypeSystem ts) {
        List l = path.throwTypes(ts);
        if (isTargetNeverNull()) {    	    
            // this field access will never throw a NPE 
            return l;
        }
        
        List throwTypes = new ArrayList(l.size() + 1);        
        throwTypes.addAll(l);
        throwTypes.add(ts.NullPointerException());
        
        return throwTypes;
    }
    
}
