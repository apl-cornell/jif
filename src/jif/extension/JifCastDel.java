package jif.extension;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jif.JifOptions;
import jif.types.JifClassType;
import jif.types.JifPolyType;
import jif.types.JifSubstType;
import jif.types.JifTypeSystem;
import jif.visit.JifTypeChecker;
import polyglot.ast.Cast;
import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.SerialVersionUID;
import polyglot.util.SubtypeSet;
import polyglot.visit.NodeVisitor;
import polyglot.visit.TypeChecker;

/** The Jif extension of the <code>Cast</code> node.
 *
 *  @see polyglot.ast.Cast_c
 */
public class JifCastDel extends JifDel_c implements JifPreciseClassDel {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifCastDel() {
    }

    private Set<Type> exprPreciseClasses = null;
    private boolean isToSubstJifClass = false;
    private boolean isClassCastExceptionFatal = false;

    public boolean isToSubstJifClass() {
        return this.isToSubstJifClass;
    }

    @Override
    public NodeVisitor typeCheckEnter(TypeChecker tc) throws SemanticException {
        JifTypeChecker jtc = (JifTypeChecker) super.typeCheckEnter(tc);
        return jtc.inferClassParameters(true);
    }

    @Override
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        // prevent casting to arrays of parameterized types
        Cast c = (Cast) this.node();
        JifTypeSystem ts = (JifTypeSystem) tc.typeSystem();
        Type castType = c.castType().type();

        if (ts.isLabeled(castType)) {
            throw new SemanticException("Cannot cast to a labeled type.",
                    c.position());
        }

        if (!ts.isParamsRuntimeRep(castType)) {
            if ((castType instanceof JifSubstType
                    && !((JifSubstType) castType).actuals().isEmpty())
                    || (castType instanceof JifPolyType
                            && !((JifPolyType) castType).params().isEmpty()))
                throw new SemanticException(
                        "Cannot cast to " + castType + ", since it does "
                                + "not represent the parameters at runtime.",
                        c.position());
        }

        if (castType.isArray()) {
            JifOptions opt = (JifOptions) ts.extensionInfo().getOptions();
            if (!opt.skipLabelChecking) {
                throw new SemanticException(
                        "Jif does not currently support casts to arrays.",
                        c.position());
            }
            //XXX: Allow cast to array. Print warning?
        }

        this.isToSubstJifClass = (castType instanceof JifSubstType
                && !((JifSubstType) castType).actuals().isEmpty());

        ts.labelTypeCheckUtil().typeCheckType(tc, castType);
        return super.typeCheck(tc);
    }

    @Override
    public List<Type> throwTypes(TypeSystem ts) {
        Cast c = (Cast) this.node();

        List<Type> ex = new ArrayList<Type>(super.throwTypes(ts));
        if (!throwsClassCastException()) {
            ex.remove(ts.ClassCastException());
            return ex;
        }
        if (c.castType().type() instanceof JifClassType) {
            LabelTypeCheckUtil ltcu = ((JifTypeSystem) ts).labelTypeCheckUtil();
            ex.addAll(ltcu.throwTypes((JifClassType) c.castType().type()));
        }

        return ex;
    }

    @Override
    public void setFatalExceptions(TypeSystem ts, SubtypeSet fatalExceptions) {
        super.setFatalExceptions(ts, fatalExceptions);
        if (fatalExceptions.contains(ts.ClassCastException()))
            isClassCastExceptionFatal = true;
    }

    public boolean throwsClassCastException() {
        if (isClassCastExceptionFatal) return false;

        Cast c = (Cast) this.node();
        Type castType = c.castType().type();
        JifTypeSystem ts = (JifTypeSystem) castType.typeSystem();
        if (exprPreciseClasses != null) {
            for (Type t : exprPreciseClasses) {
                if (typeCastGuaranteed(ts, castType, t)) {
                    return false;
                }
            }
        }
        if (typeCastGuaranteed(ts, castType, c.expr().type())) {
            return false;
        }

        return c.castType().type() instanceof JifClassType;
    }

    /**
     * Will casting from exprType to castType always succeed?
     */
    private static boolean typeCastGuaranteed(JifTypeSystem ts, Type castType,
            Type exprType) {
        if (ts.equalsNoStrip(castType, exprType)) {
            return true;
        }
        if (castType instanceof JifClassType && SubtypeChecker
                .polyTypeForClass((JifClassType) castType).params().isEmpty()) {
            // cast type is not parameterized.
            if (!(exprType instanceof JifClassType)
                    || SubtypeChecker.polyTypeForClass((JifClassType) exprType)
                            .params().isEmpty()) {
                // if the expr is definitely a subtype of the
                // cast type, no class cast exception will be throw.
                if (castType.typeSystem().isSubtype(exprType, castType)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 
     */
    @Override
    public Expr getPreciseClassExpr() {
        return ((Cast) node()).expr();
    }

    /**
     * 
     */
    @Override
    public void setPreciseClass(Set<Type> preciseClasses) {
        this.exprPreciseClasses = preciseClasses;
    }
}
