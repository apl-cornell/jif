package jif.translate;

import polyglot.ast.*;
import polyglot.ext.jl.ast.*;
import jif.ast.*;
import polyglot.types.*;
import polyglot.ext.jl.types.*;
import jif.types.*;
import jif.types.principal.Principal;
import jif.visit.*;

public abstract class PrincipalToJavaExpr_c implements PrincipalToJavaExpr {
    public abstract Expr toJava(Principal principal, JifToJavaRewriter rw) throws SemanticException;
}
