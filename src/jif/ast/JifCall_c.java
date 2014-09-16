package jif.ast;

import java.util.List;

import jif.types.JifParsedPolyType;
import jif.types.JifTypeSystem;
import polyglot.ast.Call_c;
import polyglot.ast.Expr;
import polyglot.ast.Ext;
import polyglot.ast.Id;
import polyglot.ast.Receiver;
import polyglot.types.MethodInstance;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

//XXX should be replaced with extension
@Deprecated
public class JifCall_c extends Call_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

//    @Deprecated
    public JifCall_c(Position pos, Receiver target, Id name, List<Expr> args) {
        this(pos, target, name, args, null);
    }

    public JifCall_c(Position pos, Receiver target, Id name, List<Expr> args,
            Ext ext) {
        super(pos, target, name, args, ext);
    }

    @Override
    public Type findContainer(TypeSystem ts, MethodInstance mi) {
        Type container = mi.container();
        if (container instanceof JifParsedPolyType) {
            JifParsedPolyType jppt = (JifParsedPolyType) container;
            if (jppt.params().size() > 0) {
                // return the "null instantiation" of the base type,
                // to ensure that all TypeNodes contain either
                // a JifParsedPolyType with zero params, or a
                // JifSubstClassType
                return ((JifTypeSystem) ts).nullInstantiate(position(),
                        jppt.instantiatedFrom());
            }
        }
        return super.findContainer(ts, mi);
    }
}
