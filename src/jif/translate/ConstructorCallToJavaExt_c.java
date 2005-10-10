package jif.translate;

import java.util.*;

import com.sun.rsasign.c;

import jif.types.*;
import jif.types.JifPolyType;
import jif.types.JifSubstType;
import jif.types.label.Label;
import jif.types.principal.Principal;
import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.Position;

public class ConstructorCallToJavaExt_c extends ToJavaExt_c {
    /** Rewrite this(a) to this.C$(a); Rewrite super(a) to super.C$(a) */
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        ConstructorCall n = (ConstructorCall) node();
        ConstructorInstance ci = n.constructorInstance();
        ClassType ct = ci.container().toClass();

        ConstructorCall.Kind kind = n.kind();

        // only translate calls to jif constructors        
        if (! rw.jif_ts().isJifClass(ct)) {
            List arguments = new ArrayList(n.arguments().size() + 2);
            JifPolyType jpt = null;
            if (n.kind() == ConstructorCall.THIS && 
                    ci.container() instanceof JifPolyType && 
                    rw.jif_ts().isParamsRuntimeRep(ct)) {
                jpt = (JifPolyType)ci.container();
            }
            else if (n.kind() == ConstructorCall.SUPER && 
                    ci.container() instanceof JifSubstType && rw.jif_ts().isParamsRuntimeRep(((JifSubstType)ci.container()).base())) {
                jpt = (JifPolyType)((JifSubstType)ci.container()).base();
            }
            if (jpt != null) {
                Expr placeholder = rw.java_nf().NullLit(Position.COMPILER_GENERATED);
                for (Iterator iter = jpt.params().iterator(); iter.hasNext(); ) {
                    iter.next();
                    arguments.add(placeholder);
                }
            }
            arguments.addAll(n.arguments());
            return rw.java_nf().ConstructorCall(n.position(),
                    n.kind(), n.qualifier(),
                    arguments);
        }

        String name = ClassDeclToJavaExt_c.constructorTranslatedName(ct);

        if (kind == ConstructorCall.THIS) {
            return rw.qq().parseStmt("this.%s(%LE);", name, n.arguments());
        }
        else {
            return rw.qq().parseStmt("super.%s(%LE);", name, n.arguments());
        }        
    }
}
