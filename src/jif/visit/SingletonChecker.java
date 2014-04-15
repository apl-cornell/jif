package jif.visit;

import jif.ast.JifExt;
import jif.ast.JifUtil;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.frontend.Job;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.visit.ErrorHandlingVisitor;
import polyglot.visit.NodeVisitor;

public class SingletonChecker extends ErrorHandlingVisitor {

    public SingletonChecker(Job job, TypeSystem ts, NodeFactory nf) {
        super(job, ts, nf);
    }

    @Override
    public Node leaveCall(Node old, Node n, NodeVisitor v)
            throws SemanticException {
        JifExt je = JifUtil.jifExt(n);
        je.checkSingletons(this);
        return super.leaveCall(old, n, v);
    }
}
