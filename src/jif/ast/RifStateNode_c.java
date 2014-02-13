/* new-begin */
package jif.ast;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import jif.types.RifState;
import jif.types.RifState_c;
import jif.types.principal.Principal;
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
    protected RifState state;
    protected boolean current;

    public RifStateNode_c(Position pos, Id name,
            List<PrincipalNode> principals, String check) {
        super(pos);
        this.name = name;
        this.principals = principals;
        this.state = null;
        if (check == "current") {
            this.current = true;
        } else {
            this.current = false;
        }
    }

    @Override
    public Id name() {
        return this.name;
    }

    @Override
    public List<PrincipalNode> principals() {
        return this.principals;
    }

    @Override
    public RifState state() {
        return this.state;
    }

    @Override
    public boolean isCurrent() {
        return this.current;
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
        Id name = (Id) visitChild(this.name, v);
        List<PrincipalNode> readers = visitList(this.principals, v);
        return reconstruct(name, readers);
    }

    @Override
    public boolean isDisambiguated() {
        return this.state != null;
    }

    @Override
    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        List<Principal> l = new LinkedList<Principal>();

        for (PrincipalNode r : this.principals) {
            if (!r.isDisambiguated()) {
                ar.job().extensionInfo().scheduler().currentGoal()
                        .setUnreachableThisRun();
                return this;
            }
            l.add(r.principal());
        }
        RifState state = new RifState_c(this.name, l, this.current);
        this.state = state;
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
