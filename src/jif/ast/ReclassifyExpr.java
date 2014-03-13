/* begin-new */

package jif.ast;

import polyglot.ast.Expr;
import polyglot.ast.Id;

/** An immutable representation of the Jif <code>reclassify</code> expression. 
 *  <p>Grammar: <code>reclassify(expression, idintifier)</code> </p>
 */

/* Probably this extension should change */
public interface ReclassifyExpr extends Expr {

    Expr expr();

    ReclassifyExpr expr(Expr expr);

    String downgradeKind();

    Id actionId();

}

/* end-new */
