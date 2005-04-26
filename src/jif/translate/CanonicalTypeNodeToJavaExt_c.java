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
        return translate(rw, n.type(), n.position());
    }

    static TypeNode translate(JifToJavaRewriter rw, Type t, Position pos)
	throws SemanticException
    {
//        CanonicalTypeNode n = (CanonicalTypeNode) node();

        NodeFactory nf = rw.java_nf();
        TypeSystem ts = rw.java_ts();

        if (t.isNull()) return canonical(nf, ts.Null(), pos);
        if (t.isVoid()) return canonical(nf, ts.Void(), pos);
        if (t.isBoolean()) return canonical(nf, ts.Boolean(), pos);
        if (t.isByte()) return canonical(nf, ts.Byte(), pos);
        if (t.isChar()) return canonical(nf, ts.Char(), pos);
        if (t.isShort()) return canonical(nf, ts.Short(), pos);
        if (t.isInt()) return canonical(nf, ts.Int(), pos);
        if (t.isLong()) return canonical(nf, ts.Long(), pos);
        if (t.isFloat()) return canonical(nf, ts.Float(), pos);
        if (t.isDouble()) return canonical(nf, ts.Double(), pos);

        if (rw.jif_ts().isLabel(t)) {
            return nf.AmbTypeNode(pos,
                                  nf.PackageNode(pos,
                                                 ts.packageForName("jif.lang")),
                                  "Label");
        }

        if (rw.jif_ts().isPrincipal(t)) {
            return nf.AmbTypeNode(pos,
                                  nf.PackageNode(pos,
                                                 ts.packageForName("jif.lang")),
                                  "Principal");
        }

        if (t.isArray()) {
            return rw.java_nf().ArrayTypeNode(pos,
                                              translate(rw, t.toArray().base(), pos));
        }

        if (t.isClass()) {
            Package p = t.toClass().package_();
            String name = t.toClass().name();
            if (p == null) {
                return nf.AmbTypeNode(pos, name);
            }
            else {
                return nf.AmbTypeNode(pos,
                                      nf.PackageNode(pos,
                                                     ts.packageForName(p.fullName())),
                                      name);
            }
        }

        throw new InternalCompilerError("Cannot translate type " + t + ".");
    }

    private static TypeNode canonical(NodeFactory nf, Type t, Position pos) {
        return nf.CanonicalTypeNode(pos, t);
    }
}
