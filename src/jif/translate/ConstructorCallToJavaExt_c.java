package jif.translate;

import polyglot.ast.*;
import polyglot.ext.jl.ast.*;
import jif.ast.*;
import polyglot.types.*;
import polyglot.ext.jl.types.*;
import jif.types.*;
import polyglot.visit.*;
import polyglot.util.*;

import java.util.*;

public class ConstructorCallToJavaExt_c extends ToJavaExt_c {
    /** Rewrite this(a) to this.C$(a); Rewrite super(a) to super.C$(a) */
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        ConstructorCall n = (ConstructorCall) node();
        ConstructorInstance ci = n.constructorInstance();
        ClassType ct = ci.container().toClass();

        ConstructorCall.Kind kind = n.kind();

        // only translate calls to jif constructors
        if (! rw.jif_ts().isJifClass(ct)) {
            return rw.java_nf().ConstructorCall(n.position(),
                    n.kind(), n.qualifier(),
                    n.arguments());
        }

        String name = (ct.fullName() + ".").replace('.', '$');

        if (kind == ConstructorCall.THIS) {
            return rw.qq().parseStmt("this.%s(%LE);", name, n.arguments());
        }
        else {
            return rw.qq().parseStmt("super.%s(%LE);", name, n.arguments());
        }        
    }
}
