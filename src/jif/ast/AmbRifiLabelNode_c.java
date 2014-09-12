/* new-begin */
package jif.ast;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jif.types.JifTypeSystem;
import jif.types.label.IntegPolicy;
import jif.types.label.RifIntegPolicy;
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

public class AmbRifiLabelNode_c extends LabelNode_c implements RifiLabelNode {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected List<RifiPolicyNode> policies;
    protected boolean disambiguated = false;

    public AmbRifiLabelNode_c(Position pos, List<RifiPolicyNode> policies) {
        super(pos);
        this.policies = policies;
    }

    @Override
    public List<RifiPolicyNode> policies() {
        return this.policies;
    }

    protected AmbRifiLabelNode_c reconstruct(List<RifiPolicyNode> policies) {

        if (!CollectionUtil.equals(policies, this.policies)) {
            AmbRifiLabelNode_c n = (AmbRifiLabelNode_c) copy();
            n.policies = ListUtil.copy(policies, true);
            return n;
        }

        return this;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        List<RifiPolicyNode> lnew = visitList(this.policies, v);
        return reconstruct(lnew);
    }

    @Override
    public Node disambiguate(AmbiguityRemover sc) throws SemanticException {
        JifTypeSystem ts = (JifTypeSystem) sc.typeSystem();
        JifNodeFactory nf = (JifNodeFactory) sc.nodeFactory();
        Set<IntegPolicy> integPolicies = new LinkedHashSet<IntegPolicy>();

        for (RifiPolicyNode c : this.policies) {
            if (!c.isDisambiguated()) {
                sc.job().extensionInfo().scheduler().currentGoal()
                        .setUnreachableThisRun();
                return this;
            }
            integPolicies.add((RifIntegPolicy) c.policy());
        }
        IntegPolicy ip = ts.rifjoinIntegPolicy(position, integPolicies);
        return nf.CanonicalLabelNode(position,
                ts.pairLabel(position, ts.bottomConfPolicy(position), ip));
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write("rif[");
        Iterator<RifiPolicyNode> ic = this.policies.iterator();
        while (ic.hasNext()) {
            RifiPolicyNode c = ic.next();
            print(c, w, tr);
            if (ic.hasNext()) {
                w.write(";");
            }
        }
        w.write("]");
    }
}

/* new-end */

