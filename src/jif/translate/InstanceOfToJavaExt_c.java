package jif.translate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import polyglot.ast.*;
import polyglot.ext.jl.ast.*;
import jif.ast.*;
import jif.extension.JifCastDel;
import jif.extension.JifInstanceOfDel;
import polyglot.types.*;
import polyglot.ext.jl.types.*;
import jif.types.*;
import polyglot.visit.*;

public class InstanceOfToJavaExt_c extends ToJavaExt_c {
    private Type compareType;

    public NodeVisitor toJavaEnter(JifToJavaRewriter rw) throws SemanticException {
        Instanceof io = (Instanceof)this.node();
        this.compareType = io.compareType().type();
        return super.toJavaEnter(rw);
    }

    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        Instanceof io = (Instanceof)this.node();
        if (!((JifInstanceOfDel)io.del()).isToSubstJifClass()) {
            return super.toJava(rw);
        }

        List args = new ArrayList();

        // add all the actual param expressions to args
        JifSubstType t = (JifSubstType)compareType;
        JifSubst subst = (JifSubst)t.subst();
        JifPolyType base = (JifPolyType)t.base();
        for (Iterator iter = base.params().iterator(); iter.hasNext(); ) {
            ParamInstance pi = (ParamInstance)iter.next();
            args.add(rw.paramToJava(subst.get(pi)));
        }

        // add the actual expression being cast.
        args.add(io.expr());

        String jifImplClass = ((JifSubstType)compareType).fullName();
        if (((JifSubstType)compareType).flags().isInterface()) {
            jifImplClass = ClassDeclToJavaExt_c.interfaceClassImplName(jifImplClass);
        }
        return rw.qq().parseExpr(jifImplClass + "." + ClassDeclToJavaExt_c.INSTANCEOF_METHOD_NAME + "(%LE)", (Object)args);
    }
}
