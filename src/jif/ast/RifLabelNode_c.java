/* new-begin */
package jif.ast;

import java.util.Iterator;
import java.util.LinkedList;
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

    protected List<List<RifComponentNode>> components;

    public RifLabelNode_c(Position pos, List<List<RifComponentNode>> components) {
        super(pos);
        this.components = components;
    }

    @Override
    public List<List<RifComponentNode>> components() {
        return this.components;
    }

    protected RifLabelNode_c reconstruct(List<List<RifComponentNode>> components) {
        if (!CollectionUtil.equals(components, this.components)) {
            RifLabelNode_c n = (RifLabelNode_c) copy();
            n.components = ListUtil.copy(components, true);
            return n;
        }

        return this;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        List<List<RifComponentNode>> newcomponents =
                new LinkedList<List<RifComponentNode>>();
        for (List<RifComponentNode> l : this.components) {
            List<RifComponentNode> lnew = visitList(l, v);
            newcomponents.add(lnew);
        }
        return reconstruct(newcomponents);
    }

    /*
    protected Policy producePolicy(JifTypeSystem ts, List<List<RifComponentNode>> components) {
        return ts.something();
    }*/

    @Override
    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        return this;
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        Iterator<List<RifComponentNode>> il = this.components.iterator();
        w.write("rif[");
        while (il.hasNext()) {
            List<RifComponentNode> l = il.next();
            Iterator<RifComponentNode> ic = l.iterator();
            while (ic.hasNext()) {
                RifComponentNode c = ic.next();
                print(c, w, tr);
                if (ic.hasNext()) {
                    w.write(",");
                }
            }
            if (il.hasNext()) {
                w.write(";");
                w.allowBreak(0, " ");
            }
        }
        w.write("]");
    }
}

/* new-end */
