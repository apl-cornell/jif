package jif.extension;

import java.util.LinkedList;
import java.util.List;

import jif.ast.JifNew_c;
import polyglot.ast.Expr;
import polyglot.ast.ExprOps;
import polyglot.ast.Lang;
import polyglot.ast.New;
import polyglot.ast.NewOps;
import polyglot.ast.Special;
import polyglot.ast.TypeNode;
import polyglot.types.ClassType;
import polyglot.types.ConstructorInstance;
import polyglot.types.Context;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.CodeWriter;
import polyglot.util.InternalCompilerError;
import polyglot.util.SerialVersionUID;
import polyglot.util.SubtypeSet;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.TypeChecker;

public class JifNewDel extends JifDel_c implements NewOps {
    private static final long serialVersionUID = SerialVersionUID.generate();

    /**
     * This flag records whether the target of a field access is never
     * null. This flag is by default false, but may be set to true by the
     * dataflow analysis performed by jif.visit.NotNullChecker
     */
    private boolean isQualNeverNull = false;

    /**
     * Since the CFG may visit a node more than once, we need to take the
     * OR of all values set.
     */
    private boolean qualNeverNullAlreadySet = false;

    /**
     * This flag records if an NPE is fatal due to fail-on-exception.
     */
    private boolean isNPEfatal = false;

    public void setQualifierIsNeverNull(boolean neverNull) {
        if (!qualNeverNullAlreadySet) {
            isQualNeverNull = neverNull;
        } else {
            isQualNeverNull = isQualNeverNull && neverNull;
        }
        qualNeverNullAlreadySet = true;
    }

    public boolean qualIsNeverNull() {
        Expr r = ((New) node()).qualifier();
        return (r == null || r instanceof Special || isNPEfatal
                || isQualNeverNull);
    }

    /**
     *  List of Types of exceptions that might get thrown.
     * 
     * This differs from the method defined in New_c in that it does not
     * throw a null pointer exception if the qualifier is guaranteed to be
     * non-null.  Always returns all declared exceptions (expected by call checker).
     */
    @Override
    public List<Type> throwTypes(TypeSystem ts) {
        ConstructorInstance ci = ((New) node()).constructorInstance();
        if (ci == null) {
            throw new InternalCompilerError(node().position(),
                    "Null method instance after type " + "check.");
        }

        List<Type> l = new LinkedList<Type>();

        l.addAll(ci.throwTypes());

        // We may throw a null pointer exception except when the target
        // is "this" or "super", or the receiver is guaranteed to be non-null
        if (!qualIsNeverNull()
                && !fatalExceptions.contains(ts.NullPointerException())) {
            l.add(ts.NullPointerException());
        }
        return l;
    }

    @Override
    public void setFatalExceptions(TypeSystem ts, SubtypeSet fatalExceptions) {
        super.setFatalExceptions(ts, fatalExceptions);
        if (fatalExceptions.contains(ts.NullPointerException()))
            isNPEfatal = true;
    }

    @Override
    public TypeNode findQualifiedTypeNode(AmbiguityRemover ar, ClassType outer,
            TypeNode objectType) throws SemanticException {
        return ((JifNew_c) node()).findQualifiedTypeNode(ar, outer, objectType);
    }

    @Override
    public Expr findQualifier(AmbiguityRemover ar, ClassType ct)
            throws SemanticException {
        return ((JifNew_c) node()).findQualifier(ar, ct);
    }

    @Override
    public void typeCheckFlags(TypeChecker tc) throws SemanticException {
        ((JifNew_c) node()).typeCheckFlags(tc);
    }

    @Override
    public void typeCheckNested(TypeChecker tc) throws SemanticException {
        ((JifNew_c) node()).typeCheckNested(tc);
    }

    @Override
    public void printQualifier(CodeWriter w, PrettyPrinter tr) {
        ((JifNew_c) node()).printQualifier(w, tr);
    }

    @Override
    public void printArgs(CodeWriter w, PrettyPrinter tr) {
        ((JifNew_c) node()).printArgs(w, tr);
    }

    @Override
    public void printBody(CodeWriter w, PrettyPrinter tr) {
        ((JifNew_c) node()).printBody(w, tr);
    }

    @Override
    public ClassType findEnclosingClass(Context c, ClassType ct) {
        return ((JifNew_c) node()).findEnclosingClass(c, ct);
    }

    @Override
    public boolean constantValueSet(Lang lang) {
        return ((ExprOps) jl()).constantValueSet(lang);
    }

    @Override
    public boolean isConstant(Lang lang) {
        return ((ExprOps) jl()).isConstant(lang);
    }

    @Override
    public Object constantValue(Lang lang) {
        return ((ExprOps) jl()).constantValue(lang);
    }

    @Override
    public void printShortObjectType(CodeWriter w, PrettyPrinter tr) {
        ((NewOps) jl()).printShortObjectType(w, tr);
    }
}
