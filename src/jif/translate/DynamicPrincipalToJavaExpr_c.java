package jif.translate;

import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.ext.jl.ast.*;
import jif.ast.*;
import polyglot.ext.jl.types.*;
import jif.types.*;
import jif.types.principal.DynamicPrincipal;
import jif.types.principal.Principal;
import polyglot.visit.*;
import java.util.*;

public class DynamicPrincipalToJavaExpr_c extends PrincipalToJavaExpr_c {
    public Expr toJava(Principal principal, JifToJavaRewriter rw) throws SemanticException {
        DynamicPrincipal p = (DynamicPrincipal) principal;
        return rw.qq().parseExpr(p.name());
    }
}
