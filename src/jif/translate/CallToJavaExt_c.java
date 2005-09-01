package jif.translate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import polyglot.ast.*;
import polyglot.ext.jl.ast.*;
import jif.ast.*;
import polyglot.types.*;
import polyglot.ext.jl.types.*;
import jif.types.*;
import polyglot.visit.*;

public class CallToJavaExt_c extends ExprToJavaExt_c {
    public Expr exprToJava(JifToJavaRewriter rw) throws SemanticException {
        Call n = (Call) node();
        List args = new ArrayList();
        
        MethodInstance mi = n.methodInstance();
        // for static methods of Jif classes, add args for the params of the class
        if (mi.flags().isStatic() && 
                mi.container() instanceof JifSubstType && 
                rw.jif_ts().isJifClass(((JifSubstType)mi.container()).base())) {
            JifSubstType t = (JifSubstType)mi.container();
            JifSubst subst = (JifSubst)t.subst();
            JifPolyType base = (JifPolyType)t.base();
            for (Iterator iter = base.params().iterator(); iter.hasNext(); ) {
                ParamInstance pi = (ParamInstance)iter.next();
                args.add(rw.paramToJava(subst.get(pi)));            
            }
        }
        args.addAll(n.arguments());
        
        n = (Call)n.arguments(args);        
        n = n.methodInstance(null);
        n = (Call)n.del(null);
        return n;
    }
}
