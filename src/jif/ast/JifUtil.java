package jif.ast;

import jif.types.PathMap;
import polyglot.ast.Cast;
import polyglot.ast.Expr;
import polyglot.ast.Ext;
import polyglot.ast.Node;

/**
 * An implementation of the <code>Jif</code> interface.
 */
public class JifUtil {
    // Some utility functions used to avoid casts.
    public static PathMap getPathMap(Node n) {
        JifExt ext = jifExt(n);
        return ext.X();
    }

    public static JifExt jifExt(Node n) {
        Ext ext = n.ext();
        while (ext != null && !(ext instanceof JifExt)) {
            ext = ext.ext();
        }
        return (JifExt) ext;
    }

    public static Node updatePathMap(Node n, PathMap X) {
        JifExt ext = jifExt(n);
        return updateJifExt(n, ext.X(X));
    }

    private static Node updateJifExt(Node n, JifExt jif) {
        return n.ext(updateJifExt(n.ext(), jif));
    }

    private static Ext updateJifExt(Ext e, JifExt jif) {
        if (e instanceof JifExt) return jif;
        if (e == null) return e;
        return e.ext(updateJifExt(e.ext(), jif));
    }

    /**
     * Returns the "effective expression" for expr. That is, it strips
     * away casts and downgrade expressions.
     */
    public static Expr effectiveExpr(Expr expr) {
        if (expr instanceof Cast) {
            return effectiveExpr(((Cast) expr).expr());
        }
        if (expr instanceof DowngradeExpr) {
            return effectiveExpr(((DowngradeExpr) expr).expr());
        }
        return expr;
    }
}
