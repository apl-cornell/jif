package jif.visit;

import polyglot.ast.*;
import polyglot.ast.Expr;
import polyglot.ast.Field;
import polyglot.ast.Node;
import polyglot.types.FieldInstance;
import polyglot.types.Type;
import polyglot.visit.NodeVisitor;

/** 
 * Visitor which traverses an expression AST, and determines if the expression
 * is a constant expression. This visitor should only be used for checking the 
 * initialization expressions of static fields; it is not checking for 
 * compile-time constants.
 * 
 * In general, we prevent static initializers from containing any references,
 * as such a refernce may cause a class to be loaded, and thus leak 
 * information about when a class is first mentioned. However, we allow
 * literals (e.g. String literals) and array initializers (although if an 
 * element of the array initalizer is not constant, it will be ruled out.)
 * 
 */
public class ConstChecker extends NodeVisitor
{
    /**
     * Is the expression constant?
     */
    private boolean isConst;
    
    public ConstChecker() {
	isConst = true;
    }

    public boolean isConst() {
	return isConst;
    }

    public Node override(Node n) {
        // If we've already determined the expression is non-constant,
        // then don't bother continuing
	if (!isConst) 
	    return n;

	return null;
    }

    public Node leave(Node old, Node n, NodeVisitor v) {
	if (n instanceof Field) {
	    FieldInstance fi = ((Field)n).fieldInstance();
	    if (!fi.flags().isFinal()) {
                // a non-final field is not constant
		isConst = false;
            }
	}
	else if (n instanceof Expr) {
	    Type t = ((Expr) n).type();
	    if (t.isReference() && !(n instanceof Lit) && !(n instanceof ArrayInit)) {
                // generally we rule out references as it may cause a  
                // class to be loaded, and thus leak information about when
                // a class is first mentioned. However, we allow
                // literals and array initializers (although if an element
                // of the array initalizer is not constant, it will be ruled
                // out.)
		isConst = false;
	    }
	}
	
	return n;
    }
}
