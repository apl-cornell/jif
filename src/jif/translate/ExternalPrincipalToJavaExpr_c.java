package jif.translate;

import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.ext.jl.ast.*;
import jif.ast.*;
import polyglot.ext.jl.types.*;
import jif.types.*;
import jif.types.principal.ExternalPrincipal;
import jif.types.principal.Principal;
import polyglot.visit.*;
import java.util.*;

public class ExternalPrincipalToJavaExpr_c extends PrincipalToJavaExpr_c {
    public Expr toJava(Principal principal, JifToJavaRewriter rw) throws SemanticException {
        ExternalPrincipal P = (ExternalPrincipal) principal;
        return rw.qq().parseExpr("jif.principal.%s.P", P.name());
    }
}
