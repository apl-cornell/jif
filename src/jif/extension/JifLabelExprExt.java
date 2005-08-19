package jif.extension;

import java.util.ArrayList;
import java.util.List;

import jif.ast.Jif_c;
import jif.ast.LabelExpr;
import jif.translate.ToJavaExt;
import jif.types.*;
import jif.types.label.Label;
import jif.visit.LabelChecker;
import polyglot.ast.Node;
import polyglot.types.SemanticException;

public class JifLabelExprExt extends Jif_c
{
    public JifLabelExprExt(ToJavaExt toJava) {
        super(toJava);
    }

    public Node labelCheck(LabelChecker lc) throws SemanticException
    {
        LabelExpr le = (LabelExpr) node();
        Label l = le.label().label();
        JifContext A = lc.jifContext();
        JifTypeSystem ts = lc.jifTypeSystem();

        List throwTypes = new ArrayList(le.del().throwTypes(ts));
        // make sure the label is runtime representable
        lc.constrain(new LabelConstraint(new NamedLabel("label_expr",
                                                        l),
                                         LabelConstraint.LEQ,
                                         new NamedLabel("RUNTIME_REPRESENTABLE",
                                                        ts.runtimeLabel()),
                                         A.labelEnv(),
                                         le.position()) {
            public String msg() {
                return "Label expression not representable at runtime.";
            }
            public String detailMsg() {
                return "A label expression must be representable at runtime. Arg labels and \"this\" labels are not represented at runtime.";
            }
        });

        A = (JifContext) le.enterScope(A, null);

        PathMap X1 = l.labelCheck(A);
        throwTypes.removeAll(l.throwTypes(ts));

        A = (JifContext) A.pop();
        checkThrowTypes(throwTypes);
        return X(le, X1);
    }

}
