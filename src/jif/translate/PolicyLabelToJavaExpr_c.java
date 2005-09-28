package jif.translate;

import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.ext.jl.ast.*;
import jif.ast.*;
import polyglot.ext.jl.types.*;
import jif.types.*;
import jif.types.label.Label;
import jif.types.label.PolicyLabel;
import jif.types.principal.Principal;

import java.util.*;

public class PolicyLabelToJavaExpr_c extends LabelToJavaExpr_c {
    public Expr toJava(Label label, JifToJavaRewriter rw) throws SemanticException {
        PolicyLabel L = (PolicyLabel) label;

        Expr owner = rw.principalToJava(L.owner());

        Expr set = rw.qq().parseExpr("new jif.lang.PrincipalSet()");

        for (Iterator i = L.readers().iterator(); i.hasNext(); ) {
    	    Principal p = (Principal) i.next();
            Expr pe = rw.principalToJava(p);
            set = rw.qq().parseExpr("(%E).add(%E)", set, pe);
        }

        return rw.qq().parseExpr("jif.lang.LabelUtil.privacyPolicyLabel(%E, (%E))", owner, set);
    }
}
