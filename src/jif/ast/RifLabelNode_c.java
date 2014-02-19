/* new-begin */
package jif.ast;

import java.util.Iterator;
import java.util.List;

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

public class RifLabelNode_c extends LabelNode_c implements RifLabelNode {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected List<RifPolicyNode> policies;
    protected boolean disambiguated = false;

    public RifLabelNode_c(Position pos, List<RifPolicyNode> policies) {
        super(pos);
        this.policies = policies;
    }

    @Override
    public List<RifPolicyNode> policies() {
        return this.policies;
    }

    protected RifLabelNode_c reconstruct(List<RifPolicyNode> policies) {
        if (!CollectionUtil.equals(policies, this.policies)) {
            RifLabelNode_c n = (RifLabelNode_c) copy();
            n.policies = ListUtil.copy(policies, true);
            return n;
        }

        return this;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        List<RifPolicyNode> lnew = visitList(this.policies, v);
        return reconstruct(lnew);
    }

    /*
    protected Policy producePolicy(JifTypeSystem ts, List<List<RifComponentNode>> components) {
        return ts.something();
    }*/

    @Override
    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {

        for (RifPolicyNode c : this.policies) {
            if (!c.isDisambiguated()) {
                ar.job().extensionInfo().scheduler().currentGoal()
                        .setUnreachableThisRun();
                return this;
            }
        }

        this.disambiguated = true;
        return this;
    }

    @Override
    public boolean isDisambiguated() {
        return this.disambiguated;
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write("rif[");
        Iterator<RifPolicyNode> ic = this.policies.iterator();
        while (ic.hasNext()) {
            RifPolicyNode c = ic.next();
            print(c, w, tr);
            if (ic.hasNext()) {
                w.write(";");
            }
        }
        w.write("]");
    }
}

/* new-end */