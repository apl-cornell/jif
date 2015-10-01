package jif.visit;

import jif.types.JifContext;
import jif.types.JifFieldInstance;
import jif.types.JifTypeSystem;
import jif.types.label.Label;
import jif.types.principal.Principal;
import polyglot.ast.Expr;
import polyglot.ast.FieldDecl;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.frontend.Job;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;

public class FinalParams extends ContextVisitor {

    public FinalParams(Job job, TypeSystem ts, NodeFactory nf) {
        super(job, ts, nf);
    }

    @Override
    public Node leaveCall(Node old, Node n, NodeVisitor v)
            throws SemanticException {
        if (n instanceof FieldDecl) {
            JifTypeSystem ts = (JifTypeSystem) this.ts;
            FinalParams fp = (FinalParams) v;
            FieldDecl decl = (FieldDecl) n;
            final JifFieldInstance fi = (JifFieldInstance) decl.fieldInstance();
            Expr init = decl.init();
            if (init != null) {
                if (fi.flags().isFinal() && ts.isFinalAccessExprOrConst(init)) {
                    if (ts.isLabel(fi.type())) {
                        Label rhs_label = ts.exprToLabel(ts, init,
                                (JifContext) fp.context());
                        fi.setInitializer(rhs_label);
                    } else
                        if (ts.isImplicitCastValid(fi.type(), ts.Principal())) {
                        Principal rhs_principal = ts.exprToPrincipal(ts, init,
                                (JifContext) fp.context());
                        fi.setInitializer(rhs_principal);
                    }

                }
            }

        }
        return n;
    }
}
