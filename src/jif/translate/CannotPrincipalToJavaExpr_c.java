package jif.translate;

import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.ext.jl.ast.*;
import jif.ast.*;
import polyglot.ext.jl.types.*;
import jif.types.*;
import jif.types.principal.Principal;
import polyglot.visit.*;

public class CannotPrincipalToJavaExpr_c extends PrincipalToJavaExpr_c {
    public Expr toJava(Principal principal, JifToJavaRewriter rw) throws SemanticException {
        Principal P = (Principal) principal;
        throw new InternalCompilerError(P.position(),
                                        "Cannot translate " + P + " to Java.");
    }
}
