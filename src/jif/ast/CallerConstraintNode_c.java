package jif.ast;

import java.util.LinkedList;
import java.util.List;

import jif.types.CallerConstraint;
import jif.types.CallerConstraint_c;
import jif.types.JifTypeSystem;
import jif.types.principal.Principal;
import polyglot.ast.Ext;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.CodeWriter;
import polyglot.util.CollectionUtil;
import polyglot.util.InternalCompilerError;
import polyglot.util.ListUtil;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.Translator;

/**
 * An implementation of the <code>CallerConstraint</code> interface.
 */
public class CallerConstraintNode_c extends ConstraintNode_c<CallerConstraint>
        implements CallerConstraintNode {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected List<PrincipalNode> principals;

//    @Deprecated
    public CallerConstraintNode_c(Position pos,
            List<PrincipalNode> principals) {
        this(pos, principals, null);
    }

    public CallerConstraintNode_c(Position pos, List<PrincipalNode> principals,
            Ext ext) {
        super(pos, ext);
        this.principals = ListUtil.copy(principals, true);
    }

    @Override
    public List<PrincipalNode> principals() {
        return this.principals;
    }

    @Override
    public CallerConstraintNode principals(List<PrincipalNode> principals) {
        return principals(this, principals);
    }

    protected <N extends CallerConstraintNode_c> N principals(N n,
            List<PrincipalNode> principals) {
        if (CollectionUtil.equals(n.principals, principals)) return n;
        n = copyIfNeeded(n);
        n.principals = ListUtil.copy(principals, true);
        if (constraint() != null) {
            List<Principal> l = new LinkedList<Principal>();
            for (PrincipalNode p : principals) {
                l.add(p.principal());
            }
            n.setConstraint(((CallerConstraint_c) constraint()).principals(l));
        }
        return n;
    }

    protected <N extends CallerConstraintNode_c> N reconstruct(N n,
            List<PrincipalNode> principals) {
        n = principals(n, principals);
        return n;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        List<PrincipalNode> principals = visitList(this.principals, v);
        return reconstruct(this, principals);
    }

    /**
     * @throws SemanticException  
     */
    @Override
    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        if (constraint() == null) {
            JifTypeSystem ts = (JifTypeSystem) ar.typeSystem();

            List<Principal> l = new LinkedList<Principal>();

            for (PrincipalNode p : this.principals) {
                l.add(p.principal());
            }

            return constraint(ts.callerConstraint(position(), l));
        }

        return this;
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write("caller(");

        for (PrincipalNode p : principals) {
            print(p, w, tr);
            w.write(",");
            w.allowBreak(0, " ");
        }

        w.write(")");
    }

    @Override
    public void translate(CodeWriter w, Translator tr) {
        throw new InternalCompilerError("Cannot translate " + this);
    }
}
