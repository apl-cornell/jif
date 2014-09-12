/* new-begin */
package jif.ast;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import jif.types.JifTypeSystem;
import jif.types.RifComponent;
import jif.types.RifFSM_c;
import jif.types.label.Policy;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.CodeWriter;
import polyglot.util.CollectionUtil;
import polyglot.util.ListUtil;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;

public class RifiPolicyNode_c extends PolicyNode_c implements RifiPolicyNode {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected List<RifComponentNode> components;

    public RifiPolicyNode_c(Position pos, List<RifComponentNode> components) {
        super(pos, (Policy) null); //this is not very principled!
        this.components = components;
    }

    @Override
    public List<RifComponentNode> components() {
        return this.components;
    }

    protected RifiPolicyNode_c reconstruct(List<RifComponentNode> components) {
        if (!CollectionUtil.equals(components, this.components)) {
            RifiPolicyNode_c n = (RifiPolicyNode_c) copy();
            n.components = ListUtil.copy(components, true);
            return n;
        }

        return this;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        List<RifComponentNode> lnew = visitList(this.components, v);
        return reconstruct(lnew);
    }

    protected Policy producePolicy(JifTypeSystem ts,
            List<RifComponent> components) {
        return ts.rifwriterPolicy(position(), new RifFSM_c(components));
    }

    @Override
    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        JifTypeSystem ts = (JifTypeSystem) ar.typeSystem();
        List<RifComponent> l = new LinkedList<RifComponent>();

        for (RifComponentNode c : this.components) {
            if (!c.isDisambiguated()) {
                ar.job().extensionInfo().scheduler().currentGoal()
                        .setUnreachableThisRun();
                return this;
            }
            if (c instanceof RifStateNode) {
                l.add(((RifStateNode) c).state());
            } else if (c instanceof RifTransitionNode) {
                l.add(((RifTransitionNode) c).transition());
            }
        }
        this.policy = producePolicy(ts, l);
        return this;
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {

        Iterator<RifComponentNode> ic = this.components.iterator();
        while (ic.hasNext()) {
            RifComponentNode c = ic.next();
            print(c, w, tr);
            if (ic.hasNext()) {
                w.write(",");
            }
        }

    }

}

/* new-end */

