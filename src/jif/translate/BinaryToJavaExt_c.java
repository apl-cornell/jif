package jif.translate;

import java.util.ArrayList;
import java.util.List;

import jif.extension.JifBinaryDel;
import jif.types.JifTypeSystem;
import polyglot.ast.Binary;
import polyglot.ast.Expr;
import polyglot.ast.Precedence;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.visit.NodeVisitor;

public class BinaryToJavaExt_c extends ExprToJavaExt_c {
    private Type lhsType;

    public NodeVisitor toJavaEnter(JifToJavaRewriter rw) throws SemanticException {
        Binary b = (Binary) node();
        this.lhsType = b.left().type();
        return super.toJavaEnter(rw);
    }

    public Expr exprToJava(JifToJavaRewriter rw) throws SemanticException {
        Binary b = (Binary) node();
        JifTypeSystem ts = (JifTypeSystem)rw.typeSystem(); 

        if (b.operator() == JifBinaryDel.ACTSFOR) {
            return actsforToJava(rw, false);
        }

        if (b.operator() == JifBinaryDel.EQUIV) {
            if (ts.isImplicitCastValid(this.lhsType, ts.Principal())) {
                return actsforToJava(rw, true);
            }
            else if (ts.isLabel(this.lhsType)) {
                return labelTestToJava(rw, true);
            }
        }
        
        if (b.operator() == Binary.LE && ts.isLabel(this.lhsType)) {
            return labelTestToJava(rw, false);
        }
        
        Precedence precedence = b.precedence();
        b = (Binary) super.exprToJava(rw);
        b = b.precedence(precedence);
        return b;
    }
    
    public Expr actsforToJava(JifToJavaRewriter rw, boolean isEquiv) throws SemanticException {
        Binary b = (Binary) node();
        String meth = isEquiv?"equivalentTo":"actsFor";
        String comparison = "jif.lang.PrincipalUtil." + meth + "((%E), (%E))";
        List l = new ArrayList(2);
        l.add(b.left());
        l.add(b.right());
        return rw.qq().parseExpr(comparison, l);
    }
    public Expr labelTestToJava(JifToJavaRewriter rw, boolean isEquiv) throws SemanticException {
        Binary b = (Binary) node();
        String meth = isEquiv?"equivalentTo":"relabelsTo";
        String comparison = rw.runtimeLabelUtil() + ".singleton()." + meth + "((%E), (%E))";
        
        List l = new ArrayList(2);
        l.add(b.left());
        l.add(b.right());
        return rw.qq().parseExpr(comparison, l);
    }
    
}
