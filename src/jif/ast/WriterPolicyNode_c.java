package jif.ast;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import jif.types.JifTypeSystem;
import jif.types.label.Policy;
import jif.types.principal.Principal;
import polyglot.ast.Ext;
import polyglot.ast.Node;
import polyglot.util.CodeWriter;
import polyglot.util.CollectionUtil;
import polyglot.util.InternalCompilerError;
import polyglot.util.ListUtil;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;

/** An implementation of the <code>PolicyLabel</code> interface.
 */
public class WriterPolicyNode_c extends PolicyNode_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected List<PrincipalNode> principals;

//    @Deprecated
    public WriterPolicyNode_c(Position pos, PrincipalNode owner,
            List<PrincipalNode> principals) {
        this(pos, owner, principals, null);
    }

    public WriterPolicyNode_c(Position pos, PrincipalNode owner,
            List<PrincipalNode> principals, Ext ext) {
        super(pos, owner, ext);
        this.principals = ListUtil.copy(principals, true);
    }

    public List<PrincipalNode> principals() {
        return this.principals;
    }

    public PolicyNode principals(List<PrincipalNode> principals) {
        WriterPolicyNode_c n = (WriterPolicyNode_c) copy();
        n.principals = ListUtil.copy(principals, true);
        return n;
    }

    protected Policy producePolicy(JifTypeSystem ts, Principal owner,
            List<Principal> principals) {
        return ts.writerPolicy(position(), owner, principals);
    }

    @Override
    public Node disambiguate(AmbiguityRemover ar) {
        JifTypeSystem ts = (JifTypeSystem) ar.typeSystem();
        if (!owner.isDisambiguated()) {
            ar.job().extensionInfo().scheduler().currentGoal()
                    .setUnreachableThisRun();
            return this;
        }

        Principal o = owner.principal();
        if (o == null) throw new InternalCompilerError("null owner "
                + owner.getClass().getName() + " " + owner.position());

        List<Principal> l = new LinkedList<Principal>();

        for (PrincipalNode r : this.principals) {
            if (!r.isDisambiguated()) {
                ar.job().extensionInfo().scheduler().currentGoal()
                        .setUnreachableThisRun();
                return this;
            }
            l.add(r.principal());
        }
        this.policy = producePolicy(ts, o, l);
        return this;
    }

    protected WriterPolicyNode_c reconstruct(PrincipalNode owner,
            List<PrincipalNode> principals) {
        if (owner != this.owner
                || !CollectionUtil.equals(principals, this.principals)) {
            WriterPolicyNode_c n = (WriterPolicyNode_c) copy();
            n.owner = owner;
            n.principals = ListUtil.copy(principals, true);
            return n;
        }

        return this;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        PrincipalNode owner = visitChild(this.owner, v);
        List<PrincipalNode> readers = visitList(this.principals, v);
        return reconstruct(owner, readers);
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        print(owner, w, tr);

        w.write("<-");

        for (Iterator<PrincipalNode> i = this.principals.iterator(); i
                .hasNext();) {
            PrincipalNode n = i.next();
            print(n, w, tr);
            if (i.hasNext()) {
                w.write(";");
                w.allowBreak(0, " ");
            }
        }
    }
}
