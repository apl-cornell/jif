package jif.ast;

import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.visit.*;

import jif.types.*;

import java.util.*;

/** An authority constraint node. It represents an authority
 *  constraint of a method or a class. 
 *  <p>Grammer: <tt>authority(principal_list)</tt>
 */
public interface AuthConstraintNode extends ConstraintNode
{
    /** Gets the list of principal who grants their authorities. */
    List principals();
    
    /** Returns a copy of this node with the principal list updated. */
    AuthConstraintNode principals(List principals);
}
