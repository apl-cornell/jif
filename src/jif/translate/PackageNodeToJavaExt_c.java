package jif.translate;

import polyglot.ast.*;
import polyglot.ext.jl.ast.*;
import jif.ast.*;
import polyglot.types.*;
import polyglot.types.Package;
import polyglot.ext.jl.types.*;
import jif.types.*;
import polyglot.visit.*;

public class PackageNodeToJavaExt_c extends ToJavaExt_c {
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        PackageNode n = (PackageNode) node();
        Package p = n.package_();
        p = (Package) rw.java_ts().packageForName(p.fullName());
        return rw.java_nf().PackageNode(n.position(), p);
    }
}
