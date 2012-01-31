package jif.translate;

import jif.extension.JifBinaryDel;
import jif.types.JifMethodInstance;
import jif.types.JifTypeSystem;
import polyglot.ast.Binary;
import polyglot.ast.Expr;
import polyglot.ast.Precedence;

public class BinaryToJavaExt_c extends ExprToJavaExt_c {
    @Override
    public Expr exprToJava(JifToJavaRewriter rw) {
        Binary        b   = (Binary) node();
        JifTypeSystem ts = (JifTypeSystem)rw.typeSystem(); 

        JifMethodInstance method = JifBinaryDel.equivalentMethod(ts, b.operator());
        
        if (method == null) {
            // operator is a java operator.
            Precedence precedence = b.precedence();
            b = rw.java_nf().Binary(b.position(), b.left(), b.operator(), b.right());
            b = b.precedence(precedence);
            return b;
        }
        else {
            // operator translates to a method call.
            StringBuilder call = new StringBuilder();
            call.append(method.container().toString());
            call.append(".");
            call.append(method.name());
            call.append("(%E,%E)");
            
            return rw.qq().parseExpr(call.toString(), b.left(), b.right());
        }
    }
}
