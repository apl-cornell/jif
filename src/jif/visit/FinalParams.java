package jif.visit;

import jif.ast.DowngradeExpr;
import jif.ast.JifUtil;
import jif.ast.LabelExpr;
import jif.ast.PrincipalExpr;
import jif.ast.PrincipalNode;
import jif.types.JifContext;
import jif.types.JifFieldInstance;
import jif.types.JifTypeSystem;
import jif.types.label.Label;
import jif.types.principal.DynamicPrincipal;
import jif.types.principal.Principal;
import polyglot.ast.Cast;
import polyglot.ast.Expr;
import polyglot.ast.FieldDecl;
import polyglot.ast.Node;
import polyglot.ast.NullLit;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.InternalCompilerError;
import polyglot.visit.NodeVisitor;

public class FinalParams extends NodeVisitor {
    
    JifTypeSystem ts;
    public FinalParams(JifTypeSystem ts) {
        this.ts = ts;
    }
    
    @Override
    public Node leave(Node old, Node n, NodeVisitor v) {
        if (n instanceof FieldDecl) {
            FieldDecl decl = (FieldDecl) n;
            final JifFieldInstance fi = (JifFieldInstance) decl.fieldInstance();
            Expr init = decl.init();
            if (init != null) {
                if (fi.flags().isFinal() && isConst(init)) {
                    if (ts.isLabel(fi.type())) {
                        Label rhs_label = exprToLabel(init);
                        fi.setInitializer(rhs_label);
                    }
                    else if (ts.isImplicitCastValid(fi.type(), ts.PrincipalType())) {
                        Principal rhs_principal = exprToPrincipal(init);
                        fi.setInitializer(rhs_principal);
                    }
                    
                }
            }
            
        }
        return n;
    }
    
    private Label exprToLabel(Expr e)  {
        if (e instanceof LabelExpr) {
            return ((LabelExpr)e).label().label();
        }
        throw new InternalCompilerError("Expected a constant");
    }
    
    private Principal exprToPrincipal(Expr e) {
        if (e instanceof PrincipalNode) {
            return ((PrincipalNode)e).principal();
        }
        if (e instanceof PrincipalExpr) {
            return ((PrincipalExpr)e).principal().principal();
        }
        if (e instanceof Cast) {
            return exprToPrincipal(((Cast)e).expr());            
        }
        throw new InternalCompilerError("Expected a constant");
    }    
    
    private boolean isConst(Expr e) {
        return (e instanceof LabelExpr || 
            e instanceof PrincipalNode ||
            e instanceof PrincipalExpr ||
           (e instanceof Cast && isConst(((Cast)e).expr())));
    }
    
}