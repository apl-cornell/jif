package jif.translate;

import java.util.ArrayList;
import java.util.List;

import jif.types.JifPolyType;
import jif.types.JifSubstType;
import jif.types.ParamInstance;
import polyglot.ast.ConstructorCall;
import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.types.ClassType;
import polyglot.types.ConstructorInstance;
import polyglot.types.SemanticException;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

public class ConstructorCallToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    /** Rewrite this(a) to this.C$(a); Rewrite super(a) to super.C$(a) */
    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        ConstructorCall n = (ConstructorCall) node();
        ConstructorInstance ci = n.constructorInstance();
        ClassType ct = ci.container().toClass();

        ConstructorCall.Kind kind = n.kind();
        rw.haveThisCall(kind == ConstructorCall.THIS);

        // only translate calls to jif constructors
        if (rw.jif_ts().isSignature(ct)) {
            List<Expr> arguments =
                    new ArrayList<Expr>(n.arguments().size() + 2);
            JifPolyType jpt = null;
            if (n.kind() == ConstructorCall.THIS
                    && ci.container() instanceof JifPolyType
                    && rw.jif_ts().isParamsRuntimeRep(ct)) {
                jpt = (JifPolyType) ci.container();
            } else if (n.kind() == ConstructorCall.SUPER
                    && ci.container() instanceof JifSubstType
                    && rw.jif_ts().isParamsRuntimeRep(
                            ((JifSubstType) ci.container()).base())) {
                jpt = (JifPolyType) ((JifSubstType) ci.container()).base();
            }
            if (jpt != null) {
                Expr placeholder =
                        rw.java_nf().NullLit(Position.compilerGenerated());
                for (@SuppressWarnings("unused")
                ParamInstance pi : jpt.params()) {
                    arguments.add(placeholder);
                }
            }
            arguments.addAll(n.arguments());
            return rw.java_nf().ConstructorCall(n.position(), n.kind(),
                    n.qualifier(), arguments);
        }

        String name = ClassDeclToJavaExt_c.constructorTranslatedName(ct);

        if (kind == ConstructorCall.THIS) {
            return rw.qq().parseStmt("this.%s(%LE);", name, n.arguments());
        } else {
            return rw.qq().parseStmt("this.%s(%LE);", name, n.arguments());
        }
    }
}
