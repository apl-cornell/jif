package jif.ast;

import java.util.List;

import polyglot.ast.*;

/** An ambiguous new array expression.
 *  The ambiguity arises because in <code>new T.a[e][m]</code>, <code>e</code>
 *  may be either a dimension expression or a label/principal parameter. 
 */
public interface AmbNewArray extends Expr, Ambiguous
{
    /** Gets the base type. */
    TypeNode baseType();
    
    /** Returns a copy of this node with the base type updated. */
    AmbNewArray baseType(TypeNode baseType);

    /** Gets the expr.  Will either be an Expr or a String*/
    Object expr();
    
    /** Gets the additional dimensions.  */
    List dims();
    
    /** Returns a copy of this node with the additional dimensions updated.  */
    AmbNewArray dims(List dims);
}
