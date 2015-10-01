package jif.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jif.types.JifTypeSystem;
import jif.types.label.ConfPolicy;
import jif.types.label.IntegPolicy;
import jif.types.label.Label;
import jif.types.label.Policy;
import polyglot.ast.Ext;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.CodeWriter;
import polyglot.util.CollectionUtil;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;

/** An implementation of the <code>JoinLabel</code> interface.
 */
public class MeetLabelNode_c extends AmbLabelNode_c implements MeetLabelNode {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected List<LabelComponentNode> components;

//    @Deprecated
    public MeetLabelNode_c(Position pos, List<LabelComponentNode> components) {
        this(pos, components, null);
    }

    public MeetLabelNode_c(Position pos, List<LabelComponentNode> components,
            Ext ext) {
        super(pos, ext);
        this.components = Collections.unmodifiableList(
                new ArrayList<LabelComponentNode>(components));
    }

    @Override
    public List<LabelComponentNode> components() {
        return this.components;
    }

    @Override
    public MeetLabelNode components(List<LabelComponentNode> components) {
        MeetLabelNode_c n = (MeetLabelNode_c) copy();
        this.components = Collections.unmodifiableList(
                new ArrayList<LabelComponentNode>(components));
        return n;
    }

    protected MeetLabelNode_c reconstruct(List<LabelComponentNode> components) {
        if (!CollectionUtil.equals(components, this.components)) {
            MeetLabelNode_c n = (MeetLabelNode_c) copy();
            n.components = Collections.unmodifiableList(
                    new ArrayList<LabelComponentNode>(components));
            return n;
        }

        return this;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        List<LabelComponentNode> components = visitList(this.components, v);
        return reconstruct(components);
    }

    @Override
    public Node disambiguate(AmbiguityRemover sc) throws SemanticException {
        JifTypeSystem ts = (JifTypeSystem) sc.typeSystem();
        JifNodeFactory nf = (JifNodeFactory) sc.nodeFactory();

        Set<Label> labels = new LinkedHashSet<Label>();
        Set<ConfPolicy> confPolicies = new LinkedHashSet<ConfPolicy>();
        Set<IntegPolicy> integPolicies = new LinkedHashSet<IntegPolicy>();

        for (LabelComponentNode n : this.components) {
            if (!n.isDisambiguated()) {
                sc.job().extensionInfo().scheduler().currentGoal()
                        .setUnreachableThisRun();
                return this;
            }
            if (n instanceof PolicyNode) {
                Policy pol = ((PolicyNode) n).policy();
                if (pol instanceof ConfPolicy) {
                    confPolicies.add((ConfPolicy) pol);
                } else if (pol instanceof IntegPolicy) {
                    integPolicies.add((IntegPolicy) pol);
                } else {
                    throw new InternalCompilerError("Unexpected policy " + pol);
                }
            } else if (n instanceof LabelNode) {
                labels.add(((LabelNode) n).label());
            } else throw new InternalCompilerError("Unexpected node " + n);
        }
        if (!confPolicies.isEmpty() && !integPolicies.isEmpty()) {
            throw new SemanticException("Incompatible kinds of "
                    + "policies for the meet expression.", position);
        }
        if (!labels.isEmpty()
                && (!confPolicies.isEmpty() || !integPolicies.isEmpty())) {
            throw new SemanticException(
                    "Cannot take the meet of labels and policies.", position);
        }
        if (!confPolicies.isEmpty()) {
            return nf.PolicyNode(position,
                    ts.meetConfPolicy(position, confPolicies));
        }
        if (!integPolicies.isEmpty()) {
            return nf.PolicyNode(position,
                    ts.meetIntegPolicy(position, integPolicies));
        }
        return nf.CanonicalLabelNode(position(),
                ts.meetLabel(position(), labels));
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        for (Iterator<LabelComponentNode> i = this.components.iterator(); i
                .hasNext();) {
            LabelComponentNode n = i.next();
            print(n, w, tr);
            if (i.hasNext()) {
                w.write(";");
                w.allowBreak(0, " ");
            }
        }
    }
}
