package jif.translate;

import java.util.ArrayList;
import java.util.List;

import jif.types.JifParsedPolyType;
import jif.types.JifSubst;
import jif.types.JifSubstClassType_c;
import jif.types.LabeledType;
import jif.types.ParamInstance;
import jif.types.TypeParam;
import polyglot.ast.LocalDecl;
import polyglot.ast.Node;
import polyglot.ast.TypeNode;
import polyglot.ext.jl5.ast.JL5NodeFactory;
import polyglot.types.LocalInstance;
import polyglot.types.Named;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.SerialVersionUID;
import polyglot.visit.NodeVisitor;

public class LocalDeclToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected LocalInstance li = null;

    @Override
    public NodeVisitor toJavaEnter(JifToJavaRewriter rw)
            throws SemanticException {
        LocalDecl n = (LocalDecl) this.node();
        this.li = n.localInstance();
        return super.toJavaEnter(rw);
    }

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        LocalDecl n = (LocalDecl) node();
        JL5NodeFactory nf = (JL5NodeFactory) rw.java_nf();
        if (n.localInstance().type() instanceof LabeledType) {
            Type t = ((LabeledType) n.localInstance().type()).typePart();
            if (t instanceof JifSubstClassType_c) {
                JifSubstClassType_c subB = (JifSubstClassType_c) t;
                List<TypeNode> args = new ArrayList<TypeNode>();
                for (ParamInstance pi : ((JifParsedPolyType) subB.base())
                        .params()) {
                    if (pi.isType()) {
                        TypeParam tp =
                                (TypeParam) ((JifSubst) subB.subst()).get(pi);
                        Named namedType = (Named) tp.type();
                        args.add(nf.AmbTypeNode(tp.position(),
                                nf.Id(tp.position(), namedType.name())));
                    }
                }
                if (args.size() > 0) {
                    n =
                            n.type(nf.AmbTypeInstantiation(subB.position(), nf
                                    .AmbTypeNode(subB.position(), nf
                                            .Id(n.type().position(), n.type()
                                                    .name())), args));
                }
            }
        }
        LocalInstance li = n.localInstance();
        n =
                rw.java_nf().LocalDecl(n.position(), n.flags(), n.type(),
                        n.id(), n.init());
        return n.localInstance(li);
    }
}
