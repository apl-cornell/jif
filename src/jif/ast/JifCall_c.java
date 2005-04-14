package jif.ast;

import java.util.List;

import jif.types.JifParsedPolyType;
import jif.types.JifTypeSystem;
import polyglot.ast.Receiver;
import polyglot.ext.jl.ast.Call_c;
import polyglot.types.MethodInstance;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.Position;

public class JifCall_c extends Call_c
{
    public JifCall_c(Position pos, Receiver target, String name, List args) {
        super(pos, target, name, args);
    }

    protected Type findContainer(TypeSystem ts, MethodInstance mi) {
        Type container = mi.container();
        System.out.println("Finding container for " + mi + " : " + container + " :: " + container.getClass());
        if (container instanceof JifParsedPolyType) {
            JifParsedPolyType jppt = (JifParsedPolyType)container;
            if (jppt.params().size() > 0) {
                // return the "null instantiation" of the base type,
                // to ensure that all TypeNodes contain either
                // a JifParsedPolyType with zero params, or a 
                // JifSubstClassType
                return ((JifTypeSystem)ts).nullInstantiate(position(), jppt.instantiatedFrom());
            }
        }
        return super.findContainer(ts, mi);
    }
}
