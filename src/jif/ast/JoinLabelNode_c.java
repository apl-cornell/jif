package jif.ast;

import java.util.*;

import jif.types.JifTypeSystem;
import jif.types.label.*;
import polyglot.ast.Node;
import polyglot.ast.Term;
import polyglot.types.SemanticException;
import polyglot.util.*;
import polyglot.visit.*;

/** An implementation of the <code>JoinLabel</code> interface.
 */
public class JoinLabelNode_c extends AmbLabelNode_c implements JoinLabelNode
{
    protected List<LabelComponentNode> components;

    public JoinLabelNode_c(Position pos, List<LabelComponentNode> components) {
        super(pos);
        this.components =
                Collections.unmodifiableList(new ArrayList<LabelComponentNode>(
                        components));
    }

    @Override
    public List<LabelComponentNode> components() {
        return this.components;
    }

    @Override
    public JoinLabelNode components(List<LabelComponentNode> components) {
        JoinLabelNode_c n = (JoinLabelNode_c) copy();
        n.components =
                Collections.unmodifiableList(new ArrayList<LabelComponentNode>(
                        components));
        return n;
    }

    protected JoinLabelNode_c reconstruct(List<LabelComponentNode> components) {
        if (! CollectionUtil.equals(components, this.components)) {
            JoinLabelNode_c n = (JoinLabelNode_c) copy();
            n.components =
                Collections.unmodifiableList(new ArrayList<LabelComponentNode>(
                        components));
            return n;
        }

        return this;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        @SuppressWarnings("unchecked")
        List<LabelComponentNode> components = visitList(this.components, v);
        return reconstruct(components);
    }

    /**
     * @throws SemanticException  
     */
    @Override
    public Node disambiguate(AmbiguityRemover sc) throws SemanticException {
        JifTypeSystem ts = (JifTypeSystem) sc.typeSystem();
        JifNodeFactory nf = (JifNodeFactory) sc.nodeFactory();

        Set<Label> s = new LinkedHashSet<Label>();

        Set<ConfPolicy> confPolicies = new LinkedHashSet<ConfPolicy>();
        Set<IntegPolicy> integPolicies = new LinkedHashSet<IntegPolicy>();
        for (LabelComponentNode n : this.components) {
            if (!n.isDisambiguated()) {
                sc.job().extensionInfo().scheduler().currentGoal().setUnreachableThisRun();
                return this;
            }
            if (n instanceof PolicyNode) {
                Policy pol = ((PolicyNode)n).policy();
                if (pol instanceof ConfPolicy) {
                    confPolicies.add((ConfPolicy) pol);
                }
                else {
                    integPolicies.add((IntegPolicy) pol);
                }
            }
            else if (n instanceof LabelNode) {
                s.add(((LabelNode)n).label());

            }
            else {
                throw new InternalCompilerError("Unexpected node: " + n);
            }
        }

        if (!confPolicies.isEmpty() || !integPolicies.isEmpty()) {                
            ConfPolicy cp = ts.bottomConfPolicy(position());
            IntegPolicy ip = ts.topIntegPolicy(position());
            if (!confPolicies.isEmpty()) {
                cp = ts.joinConfPolicy(position(), confPolicies);
            }
            if (!integPolicies.isEmpty()) {
                ip = ts.joinIntegPolicy(position(), integPolicies);
            }
            PairLabel pl = ts.pairLabel(position(), cp, ip);
            s.add(pl);           
        }

        return nf.CanonicalLabelNode(position(), ts.joinLabel(position(), s));
    }
    
    @Override
    public Term firstChild() {
        return null;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public List<Term> acceptCFG(CFGBuilder v, List succs) {
        return succs;
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
