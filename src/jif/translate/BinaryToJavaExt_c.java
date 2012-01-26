package jif.translate;

import java.util.ArrayList;
import java.util.List;

import jif.extension.JifBinaryDel;
import jif.extension.JifBinaryExt;
import jif.types.JifTypeSystem;
import polyglot.ast.Binary;
import polyglot.ast.Expr;
import polyglot.ast.Precedence;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.visit.NodeVisitor;

public class BinaryToJavaExt_c extends ExprToJavaExt_c {
    private Type lhsType;

    @Override
    public NodeVisitor toJavaEnter(JifToJavaRewriter rw) throws SemanticException {
        Binary b = (Binary) node();
        this.lhsType = b.left().type();
        return super.toJavaEnter(rw);
    }

    @Override
    public Expr exprToJava(JifToJavaRewriter rw) throws SemanticException {
        Binary       b   = (Binary) node();
        JifBinaryExt ext = (JifBinaryExt) b.ext(); 
        JifTypeSystem ts = (JifTypeSystem)rw.typeSystem(); 

        if (ext.isActsFor()) {
            return actsforToJava(rw, false);
        }

        if (b.operator() == JifBinaryDel.EQUIV) {
            if (ts.isImplicitCastValid(this.lhsType, ts.PrincipalType())) {
                return actsforToJava(rw, true);
            }
            else if (ts.isLabel(this.lhsType)) {
                return labelTestToJava(rw, true);
            }
        }
        
        if (ext.isRelabelsTo()) {
            return labelTestToJava(rw, false);
        }
        
        Precedence precedence = b.precedence();
        b = rw.java_nf().Binary(b.position(), b.left(), b.operator(), b.right());
        b = b.precedence(precedence);
        return b;
    }
    
    /**
     * @throws SemanticException  
     */
    public Expr actsforToJava(JifToJavaRewriter rw, boolean isEquiv) throws SemanticException {
        JifTypeSystem ts = rw.jif_ts();
        Binary b = (Binary) node();
        String className;
        if (ts.isLabel(lhsType)) {
            className = rw.runtimeLabelUtil();
        } else {
            className = ts.PrincipalUtilClassName();
        }

        String meth = isEquiv?"equivalentTo":"actsFor";
        String comparison = className + "." + meth + "((%E), (%E))";
        List<Expr> l = new ArrayList<Expr>(2);
        l.add(b.left());
        l.add(b.right());
        return rw.qq().parseExpr(comparison, l);
    }
    /**
     * @throws SemanticException  
     */
    public Expr labelTestToJava(JifToJavaRewriter rw, boolean isEquiv) throws SemanticException {
        Binary b = (Binary) node();
        String meth = isEquiv?"equivalentTo":"relabelsTo";
        String comparison = rw.runtimeLabelUtil() + "." + meth + "((%E), (%E))";
        
        List<Expr> l = new ArrayList<Expr>(2);
        l.add(b.left());
        l.add(b.right());
        return rw.qq().parseExpr(comparison, l);
    }
    
}
