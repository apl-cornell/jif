package jif.translate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import jif.types.JifSubstClassType_c;
import jif.types.LabeledType;
import jif.types.Param;
import jif.types.ParamInstance;
import jif.types.TypeParam;
import polyglot.ast.ArrayInit;
import polyglot.ast.Expr;
import polyglot.ast.FieldDecl;
import polyglot.ast.Node;
import polyglot.ast.TypeNode;
import polyglot.ext.jl5.ast.JL5NodeFactory;
import polyglot.types.FieldInstance;
import polyglot.types.Flags;
import polyglot.types.Named;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.NodeVisitor;

public class FieldDeclToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected FieldInstance fi = null;

    @Override
    public NodeVisitor toJavaEnter(JifToJavaRewriter rw)
            throws SemanticException {
        FieldDecl n = (FieldDecl) this.node();
        this.fi = n.fieldInstance();
        return super.toJavaEnter(rw);
    }

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        FieldDecl n = (FieldDecl) node();
        JL5NodeFactory nf = (JL5NodeFactory) rw.java_nf();

        // if it is an instance field with an initializing expression, we need
        // the initialiazation to the initializer method.
        if (!n.flags().isStatic() && n.init() != null) {
            Expr init = n.init();
            if (init instanceof ArrayInit) {
                Type base = fi.type().toArray().base();
                init =
                        nf.NewArray(
                                Position.compilerGenerated(),
                                rw.typeToJava(base,
                                        Position.compilerGenerated()), 1,
                                (ArrayInit) init);
            }
            rw.addInitializer(fi, init);
            n = n.init(null);
        }

        if (n.fieldInstance().type() instanceof LabeledType) {
            Type b = ((LabeledType) n.fieldInstance().type()).typePart();
            if (b instanceof JifSubstClassType_c) {
                JifSubstClassType_c subB = (JifSubstClassType_c) b;
                List<TypeNode> args = new ArrayList<TypeNode>();
                Iterator<Entry<ParamInstance, Param>> entries =
                        subB.subst().entries();
                while (entries.hasNext()) {
                    Entry<ParamInstance, Param> e = entries.next();
                    if (e.getKey().isType()) {
                        TypeParam tp = (TypeParam) e.getValue();
                        Named namedType = (Named) tp.type();
                        args.add(nf.AmbTypeNode(e.getValue().position(),
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

        n = nf.FieldDecl(n.position(), n.flags(), n.type(), n.id(), n.init());
        if (n.init() == null && n.flags().isFinal()) {
            // Strip "final" to allow translated constructor to assign to it.
            n = n.flags(n.flags().clear(Flags.FINAL));
        }
        n = n.fieldInstance(null);

        return n;
    }
}
