package jif.ast;

import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.visit.*;

import jif.types.*;

import java.util.*;

/**An immutable representation of a Jif <code>actsFor</code> statement.
 * Grammer: <code>actsFor(actor, granter) statement [else statement]
 * </code>
 */
public interface ActsFor extends CompoundStmt
{
    /** Gets the actor. */
    PrincipalNode actor();

    /** Makes a copy node and sets the actor of the copy. */
    ActsFor actor(PrincipalNode actor);

    /** Gets the granter. */
    PrincipalNode granter();

    /** Makes a copy of this node and sets the granter of the copy. */
    ActsFor granter(PrincipalNode granter);

    /** Gets the consequent statement. */
    Stmt consequent();

    /** Makes a copy of this node and sets the consequent statement of the copy.
     * */
    ActsFor consequent(Stmt consequent);

    /** Gets the alternative statement. */
    Stmt alternative();

    /** Makes a copy of this node and sets the alternative statement of the
     * copy. */
    ActsFor alternative(Stmt alternative);
}
