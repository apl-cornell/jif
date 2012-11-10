package jif.extension;

import java.util.ArrayList;
import java.util.List;

import jif.ast.LabelExpr;
import jif.translate.ToJavaExt;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.PathMap;
import jif.types.SemanticDetailedException;
import jif.types.label.Label;
import jif.visit.LabelChecker;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.SerialVersionUID;

public class JifLabelExprExt extends JifExprExt {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifLabelExprExt(ToJavaExt toJava) {
        super(toJava);
    }

    @Override
    public Node labelCheck(LabelChecker lc) throws SemanticException {
        LabelExpr le = (LabelExpr) node();
        Label l = le.label().label();
        JifContext A = lc.jifContext();
        JifTypeSystem ts = lc.jifTypeSystem();

        List<Type> throwTypes = new ArrayList<Type>(le.del().throwTypes(ts));

        // make sure the label is runtime representable
        if (!l.isRuntimeRepresentable()) {
            throw new SemanticDetailedException(
                    "Label expression not representable at runtime.",
                    "A label expression must be representable at runtime. Arg labels and \"this\" labels are not represented at runtime.",
                    le.position());
        }

        A = (JifContext) le.del().enterScope(A);

        PathMap X1 = l.labelCheck(A, lc);
        throwTypes.removeAll(l.throwTypes(ts));

        A = (JifContext) A.pop();
        checkThrowTypes(throwTypes);
        return updatePathMap(le, X1);
    }

}
