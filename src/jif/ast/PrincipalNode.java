package jif.ast;

import polyglot.ast.*;
import jif.types.*;
import jif.types.principal.Principal;

/** A placeholder in AST for a Jif principal. 
 */
public interface PrincipalNode extends ParamNode, Expr {
    Principal principal();
    PrincipalNode principal(Principal principal);
}
