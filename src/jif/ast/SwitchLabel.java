package jif.ast;

import java.util.List;

import jif.types.hierarchy.PrincipalHierarchy;

import polyglot.ast.CompoundStmt;
import polyglot.ast.Expr;

/** An immutable representation of the Jif <code>swicth label</code> statement.
 */
public interface SwitchLabel extends CompoundStmt {
    Expr expr();
    SwitchLabel expr(Expr expr);

    List cases();
    SwitchLabel cases(List cases);
    
    PrincipalHierarchy ph();
    SwitchLabel ph(PrincipalHierarchy ph);
}
