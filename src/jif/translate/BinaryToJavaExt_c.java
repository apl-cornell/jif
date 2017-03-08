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
import polyglot.util.SerialVersionUID;
import polyglot.visit.NodeVisitor;

public class BinaryToJavaExt_c extends ExprToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    private Type lhsType;

    @Override
    public NodeVisitor toJavaEnter(JifToJavaRewriter rw)
            throws SemanticException {
        Binary b = (Binary) node();
        this.lhsType = b.left().type();
        return super.toJavaEnter(rw);
    }

    @Override
    public Expr exprToJava(JifToJavaRewriter rw) throws SemanticException {
        Binary b = (Binary) node();
        JifTypeSystem ts = (JifTypeSystem) rw.typeSystem();

        if (b.operator() == JifBinaryDel.ACTSFOR) {
            return actsforToJava(rw, false);
        }

        if (b.operator() == JifBinaryDel.EQUIV) {
            if (ts.isImplicitCastValid(this.lhsType, ts.Principal())) {
                return actsforToJava(rw, true);
            } else if (ts.isLabel(this.lhsType)) {
                return labelTestToJava(rw, true);
            }
        }

        if (b.operator() == Binary.LE && ts.isLabel(this.lhsType)) {
            return labelTestToJava(rw, false);
        }

        Precedence precedence = b.precedence();
        b = rw.java_nf().Binary(b.position(), b.left(), b.operator(),
                b.right());
        b = b.precedence(precedence);
        return b;
    }

    /**
     * @throws SemanticException  
     */
    public Expr actsforToJava(JifToJavaRewriter rw, boolean isEquiv)
            throws SemanticException {
        JifTypeSystem ts = rw.jif_ts();
        Binary b = (Binary) node();
        String className;
        if (ts.isLabel(lhsType)) {
            className = rw.runtimeLabelUtil();
        } else {
            className = ts.PrincipalUtilClassName();
        }

        String meth = isEquiv ? "equivalentTo" : "actsFor";
        String comparison = className + "." + meth + "((%E), (%E))";
        return rw.qq().parseExpr(comparison, b.left(), b.right());
    }

    /**
     * @throws SemanticException  
     */
    public Expr labelTestToJava(JifToJavaRewriter rw, boolean isEquiv)
            throws SemanticException {
        Binary b = (Binary) node();
        String meth = isEquiv ? "equivalentTo" : "relabelsTo";
        String comparison = rw.runtimeLabelUtil() + "." + meth + "((%E), (%E))";

        return rw.qq().parseExpr(comparison, b.left(), b.right());
    }

}
