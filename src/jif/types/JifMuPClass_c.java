package jif.types;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import polyglot.ext.param.types.MuPClass_c;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** An implementation of the <code>JifParsedPolyType</code> interface.
 */
public class JifMuPClass_c extends MuPClass_c<ParamInstance, Param> {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected JifMuPClass_c() {
        super();
    }

    public JifMuPClass_c(JifTypeSystem ts, Position pos) {
        super(ts, pos);
    }

    @Override
    public List<ParamInstance> formals() {
        JifPolyType pt = (JifPolyType) clazz;

        return new ArrayList<ParamInstance>(pt.params());
    }

    @Override
    public String toString() {
        String s = "";

        for (Iterator<ParamInstance> i = formals().iterator(); i.hasNext();) {
            ParamInstance pi = i.next();
            s += pi.name();

            if (i.hasNext()) {
                s += ", ";
            }
        }

        if (!s.equals("")) {
            s = "[" + s + "]";
        }

        return clazz.toString() + s;
    }
}
