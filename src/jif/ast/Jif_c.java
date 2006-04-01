package jif.ast;

import java.util.Iterator;
import java.util.List;

import jif.translate.ToJavaExt;
import jif.types.JifContext;
import jif.types.PathMap;
import jif.visit.LabelChecker;
import polyglot.ast.Node;
import polyglot.ext.jl.ast.Ext_c;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.InternalCompilerError;

/** An implementation of the <code>Jif</code> interface. 
 */
public class Jif_c extends Ext_c implements Jif
{
    protected PathMap X;
    protected Jif del;
    protected ToJavaExt toJava;

    public Jif_c(ToJavaExt toJava) {
        this(null, toJava);
    }

    public Jif_c(Jif del, ToJavaExt toJava) {
        this.del = del;
        this.toJava = toJava;
    }

    public ToJavaExt toJava() {
        return toJava;
    }

    public void init(Node node) {
        super.init(node);
        toJava.init(node);
        if (del != null) {
            del.init(node);
        }
    }

    public Jif toJava(ToJavaExt toJava) {
        // Set toJava to null to prevent it from being copied unnecessarily.
        ToJavaExt old = this.toJava;
        this.toJava = null;

        Jif_c copy = (Jif_c) copy();
        copy.toJava = toJava;

        // Restore the old pointer.
        this.toJava = old;

        return copy;
    }

    public Object copy() {
        Jif_c copy = (Jif_c) super.copy();
        if (toJava != null) {
            copy.toJava = (ToJavaExt) toJava.copy();
        }
        if (del != null) {
            copy.del = (Jif) del.copy();
        }
        return copy;
    }

    public Jif del(Jif del) {
        if (del == this.del) {
            return this;
        }

        Jif old = this.del;
        this.del = null;

        Jif_c copy = (Jif_c) copy();
        copy.del = del != this ? del : null;

        // Restore the old pointer.
        this.del = old;

        return copy;
    }

    public Jif del() {
        return del != null ? del : this;
    }

    public PathMap X() {
	return X;
    }

    public Jif X(PathMap X) {
	Jif_c n = (Jif_c) copy();
	n.X = X;
	return n;
    }

    public Node labelCheck(LabelChecker lc) throws SemanticException {
        JifContext A = lc.jifContext(); 
        A = (JifContext) node().del().enterScope(A);
        return node();
    }

    // Some utility functions used to avoid casts.
    public static PathMap X(Node n) {
        return JifUtil.X(n);
    }

    public static Node X(Node n, PathMap X) {
        return JifUtil.X(n, X);
    }   

    /**
     * Check that the type excType is indeed in the list of types thrown, 
     * thowTypes, and remoive excType from that list.
     * @param throwTypes
     * @param excType
     */
    public static void checkAndRemoveThrowType(List throwTypes, Type excType) {
        if (!throwTypes.remove(excType)) {
            throw new InternalCompilerError("The type " + excType + " is not "
                                            + "declared to be thrown!");
        }
    }

    /**
     * Check that the list of types thrown, 
     * thowTypes, does not contain any checked exceptions, i.e., all throw 
     * types have been correctly label checked.
     * @param throwTypes
     */
    public static void checkThrowTypes(List throwTypes) {
        for (Iterator iter = throwTypes.iterator(); iter.hasNext();) {
            Type thrw = (Type)iter.next();
            if (thrw.typeSystem().uncheckedExceptions().contains(thrw)) {
                iter.remove();
            }            
        }
        if (!throwTypes.isEmpty()) {
            throw new InternalCompilerError("The types " + throwTypes + " are " +
                                            "declared to be thrown, but " +
                                            "are not label checked!");
        }
    }

    
}
