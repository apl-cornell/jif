package jif.types.label;

import polyglot.ast.Expr;
import polyglot.main.Report;
import polyglot.types.Type;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/**
 * Represents an access path that is not final, and thus not interpreted: the
 * type system does not track it precisely. These access paths arise during
 * method instantiation and field label instantiation. For example, given the
 * declaration
 * 
 * <pre>
 * 
 * 
 *      class C {
 *         final label{} lb;
 *         int{*this.lb} f;
 *         ...
 *      }
 * 
 * 
 * </pre>
 * 
 * and the field access
 * 
 * <pre>
 * 
 * 
 * 
 *      bar().f;
 * 
 * 
 * 
 * </pre>
 * 
 * where bar is a method that returns an object of class C, the type system
 * needs to allow the field access even though access path "bar()" is not final.
 * This is achieved through the use of AccessPathUninterpreted, so that the
 * label of the field access bar().f is "{* <>.lb}", where <>is a
 * AccessPathUninterpreted.
 * 
 * @see jif.types.label.AccessPath
 * @see jif.ast.JifInstantiator
 */
public class AccessPathUninterpreted extends AccessPathRoot {
    private static final long serialVersionUID = SerialVersionUID.generate();

    String expr;
    private final boolean allowSubst;

    public AccessPathUninterpreted(Expr expr, Position pos) {
        this(String.valueOf(expr), pos);
    }

    public AccessPathUninterpreted(String expr, Position pos) {
        this(expr, pos, false);
    }

    public AccessPathUninterpreted(String expr, Position pos,
            boolean allowSubst) {
        super(pos);
        this.expr = expr;
        this.allowSubst = allowSubst;
    }

    @Override
    public boolean isCanonical() {
        return true;
    }

    @Override
    public boolean isNeverNull() {
        return false;
    }

    @Override
    public boolean isUninterpreted() {
        return true;
    }

    @Override
    public AccessPath subst(AccessPathRoot r, AccessPath e) {
        if (allowSubst) {
            if (r == this) return e;
        }
        return this;
    }

    @Override
    public String toString() {
        if (Report.should_report(Report.debug, 1)) {
            return "<uninterpreted path: " + expr + ">";
        }
        return "<uninterp: " + expr + ">";
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public Type type() {
        return null;
    }
}
