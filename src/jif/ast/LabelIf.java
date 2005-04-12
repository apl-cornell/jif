package jif.ast;

import polyglot.ast.CompoundStmt;
import polyglot.ast.Stmt;

/**An immutable representation of a Jif <code>actsFor</code> statement.
 * Grammer: <code>actsFor(actor, granter) statement [else statement]
 * </code>
 */
public interface LabelIf extends CompoundStmt
{
    /** Gets the actor. */
    LabelExpr lhs();

    /** Makes a copy node and sets the lhs of the copy. */
    LabelIf lhs(LabelExpr lhs);

    /** Gets the rhs. */
    LabelExpr rhs();

    /** Makes a copy of this node and sets the rhs of the copy. */
    LabelIf rhs(LabelExpr rhs);

    /** Gets the consequent statement. */
    Stmt consequent();

    /** Makes a copy of this node and sets the consequent statement of the copy.
     * */
    LabelIf consequent(Stmt consequent);

    /** Gets the alternative statement. */
    Stmt alternative();

    /** Makes a copy of this node and sets the alternative statement of the
     * copy. */
    LabelIf alternative(Stmt alternative);
}
