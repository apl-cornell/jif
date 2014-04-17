package jif.ast;

import jif.types.JifPolyType;
import jif.types.JifTypeSystem;
import jif.types.ParamInstance;
import jif.types.UninstTypeParam;
import polyglot.ast.Id;
import polyglot.ast.Node;
import polyglot.ast.Node_c;
import polyglot.ast.TypeNode;
import polyglot.types.Context;
import polyglot.types.SemanticException;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.Translator;
import polyglot.visit.TypeBuilder;

/** An implementation of the <code>ParamDecl</code> interface.
 */
public class ParamDecl_c extends Node_c implements ParamDecl {
    private static final long serialVersionUID = SerialVersionUID.generate();

    ParamInstance pi;
    Id name;
    ParamInstance.Kind kind;
    TypeNode upperBound;

    public ParamDecl_c(Position pos, ParamInstance.Kind kind, Id name) {
        super(pos);
        this.kind = kind;
        this.name = name;
    }

    public ParamDecl_c(Position pos, ParamInstance.Kind kind, Id name,
            TypeNode upperBound) {
        super(pos);
        this.kind = kind;
        this.name = name;
        this.upperBound = upperBound;
    }

    @Override
    public boolean isDisambiguated() {
        return pi != null && pi.isCanonical() && super.isDisambiguated()
                && (upperBound == null || upperBound.isDisambiguated());
    }

    @Override
    public ParamInstance.Kind kind() {
        return this.kind;
    }

    @Override
    public ParamDecl kind(ParamInstance.Kind kind) {
        ParamDecl_c n = (ParamDecl_c) copy();
        n.kind = kind;
        return n;
    }

    @Override
    public String name() {
        return this.name.id();
    }

    @Override
    public ParamDecl name(String name) {
        ParamDecl_c n = (ParamDecl_c) copy();
        n.name = n.name.id(name);
        return n;
    }

    @Override
    public ParamInstance paramInstance() {
        return pi;
    }

    @Override
    public ParamDecl paramInstance(ParamInstance pi) {
        ParamDecl_c n = (ParamDecl_c) copy();
        n.pi = pi;
        return n;
    }

    @Override
    public boolean isPrincipal() {
        return kind == ParamInstance.PRINCIPAL;
    }

    @Override
    public boolean isLabel() {
        return kind == ParamInstance.INVARIANT_LABEL
                || kind == ParamInstance.COVARIANT_LABEL;
    }

    @Override
    public boolean isInvariantLabel() {
        return kind == ParamInstance.INVARIANT_LABEL;
    }

    @Override
    public boolean isCovariantLabel() {
        return kind == ParamInstance.COVARIANT_LABEL;
    }

    @Override
    public boolean isTypeParam() {
        return kind == ParamInstance.TYPE;
    }

    public void leaveScope(Context c) {
        c.addVariable(pi);
    }

    @Override
    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        if (upperBound != null) {
            String name =
                    upperBound instanceof InstTypeNode ? ((InstTypeNode) upperBound)
                            .base().name() : upperBound.name();
            TypeNode ub =
                    ar.nodeFactory().CanonicalTypeNode(upperBound.position(),
                            ar.typeSystem().typeForName(name));
            upperBound = ub;
            if (pi.type() instanceof UninstTypeParam) {
                UninstTypeParam utp = (UninstTypeParam) pi.type();
                pi.setType(utp.upperBound(ub.type().toReference()));
            }
            return upperBound(ub).paramInstance(pi);
        }
        return this;
    }

    @Override
    public Node buildTypes(TypeBuilder tb) {
        JifTypeSystem ts = (JifTypeSystem) tb.typeSystem();

        JifPolyType ct = (JifPolyType) tb.currentClass();

        ParamInstance pi = ts.paramInstance(position(), ct, kind, name.id());

        return paramInstance(pi);
    }

    @Override
    public String toString() {
        return kind.toString();
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        if (kind == ParamInstance.COVARIANT_LABEL) {
            w.write("covariant label ");
        } else if (kind == ParamInstance.INVARIANT_LABEL) {
            w.write("label ");
        } else if (kind == ParamInstance.PRINCIPAL) {
            w.write("principal ");
        } else if (kind == ParamInstance.TYPE) {
            w.write("type ");
        }

        w.write(name.id());
    }

    @Override
    public void translate(CodeWriter w, Translator tr) {
    }

    @Override
    public TypeNode upperBound() {
        return upperBound;
    }

    private ParamDecl upperBound(TypeNode tn) {
        ParamDecl_c pd = (ParamDecl_c) copy();
        pd.upperBound = tn;
        return pd;
    }
}
