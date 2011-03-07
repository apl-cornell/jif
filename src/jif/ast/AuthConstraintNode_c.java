package jif.ast;

import java.util.*;

import jif.types.AuthConstraint;
import jif.types.AuthConstraint_c;
import jif.types.JifTypeSystem;
import jif.types.principal.Principal;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.*;
import polyglot.visit.*;

/** An implementation of the <code>AuthConstraintNode</code> interface.
 */
public class AuthConstraintNode_c extends ConstraintNode_c<AuthConstraint>
        implements AuthConstraintNode {
    
    protected List<PrincipalNode> principals;

    public AuthConstraintNode_c(Position pos, List<PrincipalNode> principals) {
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
    public AuthConstraintNode principals(List<PrincipalNode> principals) {
	AuthConstraintNode_c n = (AuthConstraintNode_c) copy();
        n.principals =
                Collections.unmodifiableList(new ArrayList<PrincipalNode>(
                        principals));
        if (constraint()!=null) {
            List<Principal> l = new LinkedList<Principal>();
            for (PrincipalNode p : principals) {
                l.add(p.principal());
            }
            n.setConstraint(((AuthConstraint_c)constraint()).principals(l));
        }
	return n;
    }

    protected AuthConstraintNode_c reconstruct(List<PrincipalNode> principals) {
	if (! CollectionUtil.equals(principals, this.principals)) {
            List<PrincipalNode> newPrincipals =
                    Collections.unmodifiableList(new ArrayList<PrincipalNode>(
                            principals));
            return (AuthConstraintNode_c)this.principals(newPrincipals);
	}
	return this;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
	@SuppressWarnings("unchecked")
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

            return constraint(ts.authConstraint(position(), l));
        }

        return this;
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write("authority(");

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
