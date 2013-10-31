package jif.ast;

import jif.types.principal.Principal;
import polyglot.ast.Expr;

/** A placeholder in AST for a Jif principal. 
 */
public interface PrincipalNode extends ActsForParamNode<Principal>, Expr {
    Principal principal();

    PrincipalNode principal(Principal principal);
}
