package jif.ast;

import jif.types.*;
import jif.extension.*;
import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.visit.*;
import java.util.*;

/** An ambiguous new array expression.
 *  The ambiguity arises because in <code>new T.a[n][m]</code>, <code>n</code>
 *  may be either an expression or a label/principal parameter. 
 */
public interface AmbNewArray extends Expr, Ambiguous
{
    /** Gets the base type. */
    TypeNode baseType();
    
    /** Returns a copy of this node with the base type updated. */
    AmbNewArray baseType(TypeNode baseType);

    /** Gets the ambiguous name.  */
    String name();
    
    /** Returns a copy of this node with the ambiguous name updated.  */
    AmbNewArray name(String name);

    /** Gets the additional dimensions.  */
    List dims();
    
    /** Returns a copy of this node with the additional dimensions updated.  */
    AmbNewArray dims(List dims);
}
