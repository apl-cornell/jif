package jif.parse;

import polyglot.ast.Node;
import polyglot.visit.NodeVisitor;

/**
 * An <code>UnwrapVisitor</code> rewrites the AST to remove any Wrapped
 * nodes resulting from the parser.
 *
 * A visitor which tries to unwrap every <code>Wrapper</code> object in
 *  the node it visits.
 */
public class UnwrapVisitor extends NodeVisitor {
    boolean error;

    public boolean isError() {
        return error;
    }

    @Override
    public Node override(Node n) {
        if (!error && n instanceof Wrapper) {
            try {
                return ((Wrapper) n).amb.toExpr();
            } catch (Exception e) {
                error = true;
            }
        }

        if (error) {
            return n;
        }

        return null;
    }
}
