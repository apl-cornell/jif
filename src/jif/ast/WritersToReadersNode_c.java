package jif.ast;

import jif.types.JifTypeSystem;
import jif.types.label.IntegPolicy;
import jif.types.label.Policy;

import polyglot.ast.Ext;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;

/** An implementation of the <code>JoinLabel</code> interface.
 */
public class WritersToReadersNode_c extends PolicyNode_c implements WritersToReadersNode {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected LabelComponentNode component;

    public WritersToReadersNode_c(Position pos, LabelComponentNode component) {
      this(pos, component, null);
    }

    public WritersToReadersNode_c(Position pos, LabelComponentNode component, Ext ext) {
      super(pos, ext);
      this.component = component;
    }

    @Override
    public LabelComponentNode component() {
      return this.component;
    }

    @Override
    public WritersToReadersNode component(LabelComponentNode component) {
      WritersToReadersNode_c n = (WritersToReadersNode_c) copy();
      n.component = component;
      return n;
    }

    protected WritersToReadersNode_c reconstruct(LabelComponentNode component) {
      if (!this.component.equals(component)) {
        WritersToReadersNode_c n = (WritersToReadersNode_c) copy();
        n.component = component;
        return n;
      }
      return this;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
      LabelComponentNode sublabel = visitChild(this.component, v);
      return reconstruct(sublabel);
    }

    @Override
    public Node disambiguate(AmbiguityRemover sc) throws SemanticException {
      JifTypeSystem ts = (JifTypeSystem) sc.typeSystem();
      JifNodeFactory nf = (JifNodeFactory) sc.nodeFactory();

      if (!component.isDisambiguated()) {
          sc.job().extensionInfo().scheduler().currentGoal().setUnreachableThisRun();
          return this;
      }

      if (component instanceof LabelNode) {
        return nf.PolicyNode(position(),
            ts.writersToReadersPolicy(position(),
                ((LabelNode) component).label().integProjection()));
      } else if (component instanceof PolicyNode) {
        Policy p = ((PolicyNode) component).policy();
        if (p instanceof IntegPolicy) {
          return nf.PolicyNode(position(),
                  ts.writersToReadersPolicy(position(), (IntegPolicy) p));
        }
      }

      // In this case, we're dealing with writers to readers of a
      // confidentiality policy, which doesn't really do anything and becomes
      // the bottom confidentiality.
      return nf.PolicyNode(position(), ts.bottomIntegPolicy(position()));
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
      w.write("W2R(");
      print(component, w, tr);
      w.write(")");
    }
}
