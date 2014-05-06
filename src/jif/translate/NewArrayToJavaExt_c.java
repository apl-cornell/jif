package jif.translate;

import jif.types.JifTypeSystem;
import jif.types.LabeledType;
import jif.types.UninstTypeParam;
import polyglot.ast.NewArray;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

public class NewArrayToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        NewArray n = (NewArray) node();
        JifTypeSystem jts = (JifTypeSystem) n.type().typeSystem();
        Type base =
                jts.isLabeled(n.type().toArray().base()) ? ((LabeledType) n
                        .type().toArray().base()).typePart() : n.type()
                        .toArray().base();
        if (base instanceof UninstTypeParam) {
            return rw.java_nf().Cast(
                    Position.compilerGenerated(),
                    rw.java_nf().ArrayTypeNode(
                            Position.compilerGenerated(),
                            rw.java_nf().AmbTypeNode(
                                    Position.compilerGenerated(),
                                    rw.java_nf().Id(
                                            Position.compilerGenerated(),
                                            ((UninstTypeParam) base)
                                                    .paramInstance().name()))),
                    rw.java_nf().NewArray(
                            n.position(),
                            rw.java_nf().CanonicalTypeNode(n.position(),
                                    rw.java_ts().Object()), n.dims(),
                            n.additionalDims(), n.init()));
        } else {
            return rw.java_nf().NewArray(n.position(), n.baseType(), n.dims(),
                    n.additionalDims(), n.init());
        }
    }
}
