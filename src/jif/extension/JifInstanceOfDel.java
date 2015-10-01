package jif.extension;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jif.types.JifClassType;
import jif.types.JifPolyType;
import jif.types.JifSubstType;
import jif.types.JifTypeSystem;
import polyglot.ast.Instanceof;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.SerialVersionUID;
import polyglot.visit.TypeChecker;

/** The Jif extension of the <code>Cast</code> node.
 *
 *  @see polyglot.ast.Cast_c
 */
public class JifInstanceOfDel extends JifDel_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifInstanceOfDel() {
    }

    private boolean isToSubstJifClass = false;

    public boolean isToSubstJifClass() {
        return this.isToSubstJifClass;
    }

    @Override
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        // prevent instanceof to arrays of parameterized types
        Instanceof io = (Instanceof) this.node();
        JifTypeSystem ts = (JifTypeSystem) tc.typeSystem();
        Type compareType = io.compareType().type();
        if (ts.isLabeled(compareType)) {
            throw new SemanticException(
                    "Cannot perform instanceof on a labeled type.",
                    io.position());
        }

        if (!ts.isParamsRuntimeRep(compareType)) {
            if ((compareType instanceof JifSubstType
                    && !((JifSubstType) compareType).actuals().isEmpty())
                    || (compareType instanceof JifPolyType
                            && !((JifPolyType) compareType).params().isEmpty()))
                throw new SemanticException(
                        "Cannot perform instanceof on " + compareType
                                + ", since it does "
                                + "not represent the parameters at runtime.",
                        io.position());
        }
        if (compareType.isArray()) {
            throw new SemanticException(
                    "Jif does not currently support instanceof to arrays.",
                    io.position());
//            while (compareType.isArray()) {
//                compareType = ts.unlabel(compareType.toArray().base());
//            }
//            if (compareType instanceof JifSubstType && ((JifSubstType)compareType).entries().hasNext()) {
//                throw new SemanticException("Jif does not currently support instanceof to an array of a parameterized type.", io.position());
//            }
        }
        this.isToSubstJifClass = (compareType instanceof JifSubstType
                && ((JifSubstType) compareType).entries().hasNext());

        ts.labelTypeCheckUtil().typeCheckType(tc, compareType);
        return super.typeCheck(tc);
    }

    @Override
    public List<Type> throwTypes(TypeSystem ts) {
        List<Type> ex = new ArrayList<Type>(super.throwTypes(ts));
        Instanceof io = (Instanceof) this.node();
        if (io.compareType().type() instanceof JifClassType) {
            LabelTypeCheckUtil ltcu = ((JifTypeSystem) ts).labelTypeCheckUtil();
            ex.addAll(ltcu.throwTypes((JifClassType) io.compareType().type()));
        }
        for (Iterator<Type> it = ex.iterator(); it.hasNext();)
            if (fatalExceptions.contains(it.next())) it.remove();
        return ex;
    }
}
