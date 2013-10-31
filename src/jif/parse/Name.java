package jif.parse;

import polyglot.ast.Expr;
import polyglot.ast.Id;
import polyglot.ast.PackageNode;
import polyglot.ast.Prefix;
import polyglot.ast.QualifierNode;
import polyglot.ast.Receiver;
import polyglot.ast.TypeNode;
import polyglot.util.Position;

/**
 * A <code>Name</code> represents a <code>Amp</code> of the form "n | P.n".
 * This could either be a field access or a type.  Both could be preceded by
 * ambiguous qualifiers.
 */
public class Name extends Amb {
    // prefix.name
    public String name;
    public Amb prefix;

    public Name(Grm parser, Position pos, String name) throws Exception {
        this(parser, pos, null, name);
    }

    public String name() {
        return name;
    }

    public Amb prefix() {
        return prefix;
    }

    public Name(Grm parser, Position pos, Amb prefix, String name)
            throws Exception {
        super(parser, pos);
        this.prefix = prefix;
        this.name = name;

        if (prefix instanceof LabeledExpr) parser.die(pos);
        if (prefix instanceof Array) parser.die(pos);
    }

    @Override
    public Expr toExpr() throws Exception {
        if (prefix == null) {
            if ("this".equals(name)) {
                return parser.nf.This(pos);
            }
            Id id = parser.nf.Id(pos, name);
            return parser.nf.AmbExpr(pos, id);
        }

        Id id = parser.nf.Id(pos, name);
        return parser.nf.Field(pos, prefix.toReceiver(), id);
    }

    @Override
    public Receiver toReceiver() throws Exception {
        if (prefix == null) {
            if ("this".equals(name)) {
                return parser.nf.This(pos);
            }
            Id id = parser.nf.Id(pos, name);
            return parser.nf.AmbReceiver(pos, id);
        }

        Id id = parser.nf.Id(pos, name);
        return parser.nf.AmbReceiver(pos, prefix.toPrefix(), id);
    }

    @Override
    public Prefix toPrefix() throws Exception {
        if (prefix == null) {
            if ("this".equals(name)) {
                return parser.nf.This(pos);
            }
            Id id = parser.nf.Id(pos, name);
            return parser.nf.AmbPrefix(pos, id);
        }

        Id id = parser.nf.Id(pos, name);
        return parser.nf.AmbPrefix(pos, prefix.toPrefix(), id);
    }

    @Override
    public PackageNode toPackage() throws Exception {
        return parser.nf.PackageNode(pos, parser.ts.packageForName(toName()));
    }

    @Override
    public TypeNode toType() throws Exception {
        Id id = parser.nf.Id(pos, name);

        if (prefix == null) {
            return parser.nf.AmbTypeNode(pos, id);
        }

        return parser.nf.AmbTypeNode(pos, prefix.toQualifier(), id);
    }

    @Override
    public QualifierNode toQualifier() throws Exception {
        Id id = parser.nf.Id(pos, name);

        if (prefix == null) {
            return parser.nf.AmbQualifierNode(pos, id);
        }

        return parser.nf.AmbQualifierNode(pos, prefix.toQualifier(), id);
    }

    @Override
    public TypeNode toClassType() throws Exception {
        return toType();
    }

    @Override
    public TypeNode toUnlabeledType() throws Exception {
        return toType();
    }

    @Override
    public Id toIdentifier() throws Exception {
        if (prefix != null) {
            parser.die(pos);
        }

        return parser.nf.Id(pos, name);
    }

    @Override
    public String toName() throws Exception {
        if (prefix == null) {
            return name;
        }

        return prefix.toName() + "." + name;
    }

    @Override
    public String toString() {
        try {
            return toName();
        } catch (Exception e) {
            return super.toString();
        }
    }
}
