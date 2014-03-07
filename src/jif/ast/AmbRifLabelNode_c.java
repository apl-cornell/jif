/* new-begin */
package jif.ast;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jif.types.JifTypeSystem;
import jif.types.label.ConfPolicy;
import jif.types.label.RifReaderPolicy;
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

public class AmbRifLabelNode_c extends LabelNode_c implements RifLabelNode {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected List<RifPolicyNode> policies;
    protected boolean disambiguated = false;

    public AmbRifLabelNode_c(Position pos, List<RifPolicyNode> policies) {
        super(pos);
        this.policies = policies;
    }

    @Override
    public List<RifPolicyNode> policies() {
        return this.policies;
    }

    protected AmbRifLabelNode_c reconstruct(List<RifPolicyNode> policies) {

        if (!CollectionUtil.equals(policies, this.policies)) {
            AmbRifLabelNode_c n = (AmbRifLabelNode_c) copy();
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

    @Override
    public Node disambiguate(AmbiguityRemover sc) throws SemanticException {
        JifTypeSystem ts = (JifTypeSystem) sc.typeSystem();
        JifNodeFactory nf = (JifNodeFactory) sc.nodeFactory();
        Set<RifReaderPolicy> confPolicies =
                new LinkedHashSet<RifReaderPolicy>();

        for (RifPolicyNode c : this.policies) {
            if (!c.isDisambiguated()) {
                sc.job().extensionInfo().scheduler().currentGoal()
                        .setUnreachableThisRun();
                return this;
            }
            confPolicies.add((RifReaderPolicy) c.policy());
        }
        ConfPolicy cp = ts.rifjoinConfPolicy(position, confPolicies);
        return nf.CanonicalLabelNode(position,
                ts.pairLabel(position, cp, ts.topIntegPolicy(position)));
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
