package jif.translate;

import polyglot.ast.*;
import polyglot.ext.jl.ast.*;
import jif.ast.*;
import polyglot.types.*;
import polyglot.types.Package;
import polyglot.ext.jl.types.*;
import jif.types.*;
import polyglot.visit.*;
import polyglot.util.*;

public class CanonicalTypeNodeToJavaExt_c extends ToJavaExt_c {
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        CanonicalTypeNode n = (CanonicalTypeNode) node();
        return translate(rw, n.type());
    }

    TypeNode translate(JifToJavaRewriter rw, Type t)
	throws SemanticException
    {
        CanonicalTypeNode n = (CanonicalTypeNode) node();

        NodeFactory nf = rw.java_nf();
        TypeSystem ts = rw.java_ts();

        if (t.isNull()) return canonical(nf, ts.Null());
        if (t.isVoid()) return canonical(nf, ts.Void());
        if (t.isBoolean()) return canonical(nf, ts.Boolean());
        if (t.isByte()) return canonical(nf, ts.Byte());
        if (t.isChar()) return canonical(nf, ts.Char());
        if (t.isShort()) return canonical(nf, ts.Short());
        if (t.isInt()) return canonical(nf, ts.Int());
        if (t.isLong()) return canonical(nf, ts.Long());
        if (t.isFloat()) return canonical(nf, ts.Float());
        if (t.isDouble()) return canonical(nf, ts.Double());

        if (rw.jif_ts().isLabel(t)) {
            return nf.AmbTypeNode(n.position(),
                                  nf.PackageNode(n.position(),
                                                 ts.packageForName("jif.lang")),
                                  "Label");
        }

        if (rw.jif_ts().isPrincipal(t)) {
            return nf.AmbTypeNode(n.position(),
                                  nf.PackageNode(n.position(),
                                                 ts.packageForName("jif.lang")),
                                  "Principal");
        }

        if (t.isArray()) {
            return rw.java_nf().ArrayTypeNode(n.position(),
                                              translate(rw, t.toArray().base()));
        }

        if (t.isClass()) {
            Package p = t.toClass().package_();
            String name = t.toClass().name();
            if (p == null) {
                return nf.AmbTypeNode(n.position(), name);
            }
            else {
                return nf.AmbTypeNode(n.position(),
                                      nf.PackageNode(n.position(),
                                                     ts.packageForName(p.fullName())),
                                      name);
            }
        }

        throw new InternalCompilerError("Cannot translate type " + t + ".");
    }

    TypeNode canonical(NodeFactory nf, Type t) {
        CanonicalTypeNode n = (CanonicalTypeNode) node();
        return nf.CanonicalTypeNode(n.position(), t);
    }
}
