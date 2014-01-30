/* new-begin */
package jif.ast;

import java.util.Iterator;
import java.util.List;

import polyglot.ast.Id;
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

public class RifStateNode_c extends RifComponentNode_c implements RifStateNode {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected Id name;
    protected List<PrincipalNode> principals;

    public RifStateNode_c(Position pos, Id name, List<PrincipalNode> principals) {
        super(pos);
        this.name = name;
        this.principals = principals;
    }

    @Override
    public Id name() {
        return this.name;
    }

    @Override
    public List<PrincipalNode> principals() {
        return this.principals;
    }

    protected RifStateNode_c reconstruct(Id name, List<PrincipalNode> principals) {
        if (name != this.name
                || !CollectionUtil.equals(principals, this.principals)) {
            RifStateNode_c n = (RifStateNode_c) copy();
            n.name = name;
            n.principals = ListUtil.copy(principals, true);
            return n;
        }

        return this;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        List<PrincipalNode> readers = visitList(this.principals, v);
        return reconstruct(this.name, readers);
    }

    @Override
    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        /*  List<Principal> l = new LinkedList<Principal>();

          for (PrincipalNode r : this.principals) {
              if (!r.isDisambiguated()) {
                  ar.job().extensionInfo().scheduler().currentGoal()
                          .setUnreachableThisRun();
                  return this;
              }
              l.add(r.principal());
          } */
        return this;
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        print(this.name, w, tr);
        w.write(":");
        w.allowBreak(0, " ");
        w.write("{");
        for (Iterator<PrincipalNode> i = this.principals.iterator(); i
                .hasNext();) {
            PrincipalNode n = i.next();
            print(n, w, tr);
            if (i.hasNext()) {
                w.write(",");
                w.allowBreak(0, " ");
            }
        }
        w.write("}");
    }

}

/* new-end */
