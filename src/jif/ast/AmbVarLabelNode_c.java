package jif.ast;

import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.JifVarInstance;
import jif.types.ParamInstance;
import jif.types.PrincipalInstance;
import jif.types.label.CovariantParamLabel;
import jif.types.label.ParamLabel;
import polyglot.ast.Ext;
import polyglot.ast.Id;
import polyglot.ast.Node;
import polyglot.types.Context;
import polyglot.types.SemanticException;
import polyglot.types.VarInstance;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;

/** An implementation of the <code>AmbVarLabelNode</code> interface. 
 */
public class AmbVarLabelNode_c extends AmbLabelNode_c
        implements AmbVarLabelNode {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected Id name;

    public AmbVarLabelNode_c(Position pos, Id name) {
        this(pos, name, null);
    }

    public AmbVarLabelNode_c(Position pos, Id name, Ext ext) {
        super(pos, ext);
        this.name = name;
    }

    @Override
    public String toString() {
        return name + "{amb}";
    }

    @Override
    public String name() {
        return this.name.id();
    }

    public AmbVarLabelNode name(String name) {
        return name(this, name);
    }

    protected <N extends AmbVarLabelNode_c> N name(N n, String name) {
        if (n.name.id().equals(name)) return n;
        return id(n, n.name.id(name));
    }

    public AmbVarLabelNode id(Id id) {
        return id(this, id);
    }

    protected <N extends AmbVarLabelNode_c> N id(N n, Id id) {
        if (n.name == id) return n;
        n = copyIfNeeded(n);
        n.name = id;
        return n;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        if (this.name == null) return this;

        Id name = visitChild(this.name, v);
        return reconstruct(this, name);
    }

    protected <N extends AmbVarLabelNode_c> N reconstruct(N n, Id name) {
        n = id(n, name);
        return n;
    }

    @Override
    public Node disambiguate(AmbiguityRemover sc) throws SemanticException {
        Context c = sc.context();
        JifTypeSystem ts = (JifTypeSystem) sc.typeSystem();
        JifNodeFactory nf = (JifNodeFactory) sc.nodeFactory();

        if ("provider".equals(name.id())) {
            JifContext jc = (JifContext) c;
            return nf.CanonicalLabelNode(position, jc.provider());
        }

        VarInstance vi = c.findVariable(name.id());

        if (vi instanceof JifVarInstance) {
            JifVarInstance jvi = (JifVarInstance) vi;
            return nf.CanonicalLabelNode(position(), jvi.label());
        }

        if (vi instanceof ParamInstance) {
            ParamInstance pi = (ParamInstance) vi;

            if (pi.isCovariantLabel()) {
                CovariantParamLabel pl = ts.covariantLabel(position(), pi);
                pl.setDescription("label parameter " + pi.name() + " of class "
                        + pi.container().fullName());
                return nf.CanonicalLabelNode(position(), pl);
            }
            if (pi.isInvariantLabel()) {
                ParamLabel pl = ts.paramLabel(position(), pi);
                pl.setDescription("label parameter " + pi.name() + " of class "
                        + pi.container().fullName());

                return nf.CanonicalLabelNode(position(), pl);
            }
            if (pi.isPrincipal()) {
                throw new SemanticException(
                        "Cannot use the external principal " + name
                                + " as a label. (The label \"{" + name
                                + ": }\" may have " + "been intended.)",
                        this.position());
            }
        }

        if (vi instanceof PrincipalInstance) {
            throw new SemanticException(
                    "Cannot use the external principal " + name
                            + " as a label. (The label \"{" + name
                            + ": }\" may have " + "been intended.)",
                    this.position());
        }

        throw new SemanticException(vi + " cannot be used as a label.",
                this.position());
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write(name.id());
    }
}
