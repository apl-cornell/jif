package jif.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import jif.types.CallerConstraint;
import jif.types.CallerConstraint_c;
import jif.types.JifTypeSystem;
import jif.types.principal.Principal;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.CodeWriter;
import polyglot.util.CollectionUtil;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.Translator;

/**
 * An implementation of the <code>CallerConstraint</code> interface.
 */
public class CallerConstraintNode_c extends ConstraintNode_c<CallerConstraint>
        implements CallerConstraintNode {

    protected List<PrincipalNode> principals;

    public CallerConstraintNode_c(Position pos, List<PrincipalNode> principals) {
        super(pos);
        this.principals =
                Collections.unmodifiableList(new ArrayList<PrincipalNode>(
                        principals));
    }

    @Override
    public List<PrincipalNode> principals() {
        return this.principals;
    }

    @Override
    public CallerConstraintNode principals(List<PrincipalNode> principals) {
        CallerConstraintNode_c n = (CallerConstraintNode_c) copy();
        n.principals =
                Collections.unmodifiableList(new ArrayList<PrincipalNode>(
                        principals));
        if (constraint() != null) {
            List<Principal> l = new LinkedList<Principal>();
            for (PrincipalNode p : principals) {
                l.add(p.principal());
            }
            n.setConstraint(((CallerConstraint_c) constraint()).principals(l));
        }
        return n;
    }

    protected CallerConstraintNode_c reconstruct(List<PrincipalNode> principals) {
        if (!CollectionUtil.equals(principals, this.principals)) {
            List<PrincipalNode> newPrincipals =
                    Collections.unmodifiableList(new ArrayList<PrincipalNode>(
                            principals));
            return (CallerConstraintNode_c) this.principals(newPrincipals);
        }

        return this;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        List<PrincipalNode> principals = visitList(this.principals, v);
        return reconstruct(principals);
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
