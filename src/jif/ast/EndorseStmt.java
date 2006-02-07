package jif.ast;

import polyglot.ast.CompoundStmt;
import polyglot.ast.Stmt;

/** An immutable representation of the Jif <code>endorse</code> statement. 
 *  <p>Grammer: <code>endorse(label_to, label_from) stmt</code> </p>
 */
public interface EndorseStmt extends DowngradeStmt
{
}
