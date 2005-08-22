package jif.translate;

import polyglot.ast.*;
import polyglot.ext.jl.ast.*;
import jif.ast.*;
import polyglot.types.*;
import polyglot.ext.jl.types.*;
import jif.types.*;
import polyglot.visit.*;

public class FormalToJavaExt_c extends ToJavaExt_c {
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        Formal n = (Formal) node();
        Formal newN = rw.nodeFactory().Formal(n.position(), n.flags(), n.type(), n.name());
        LocalInstance li = n.localInstance();
        
        newN = newN.localInstance(rw.typeSystem().localInstance(li.position(), 
                                                                li.flags(), 
                                                                rw.typeSystem().unknownType(li.position()), 
                                                                li.name()));
        return newN;
    }
}
