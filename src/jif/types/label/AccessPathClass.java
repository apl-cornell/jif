package jif.types.label;

import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.PathMap;
import jif.visit.LabelChecker;
import polyglot.main.Report;
import polyglot.types.ClassType;
import polyglot.types.Type;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/**
 * Represents a final access path rooted at a class, e.g. "Foo[Alice]".
 * 
 * @see jif.types.label.AccessPath
 */
public class AccessPathClass extends AccessPathRoot {
    private static final long serialVersionUID = SerialVersionUID.generate();

    private ClassType ct;

    public AccessPathClass(ClassType ct, Position pos) {
        super(pos);
        this.ct = ct;
    }

    @Override
    public boolean isCanonical() {
        return true;
    }

    @Override
    public boolean isNeverNull() {
        return true;
    }

    @Override
    public AccessPath subst(AccessPathRoot r, AccessPath e) {
        return this;
    }

    @Override
    public String toString() {
        if (Report.should_report(Report.debug, 2)) {
            return ct.fullName();
        }
        return ct.name();
    }

    @Override
    public String exprString() {
        return ct.fullName();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof AccessPathClass) {
            return ct.equals(((AccessPathClass) o).ct);
        }
        return false;
    }

    @Override
    public Type type() {
        return ct;
    }

    @Override
    public int hashCode() {
        return -2030;
    }

    @Override
    public PathMap labelcheck(JifContext A, LabelChecker lc) {
        JifTypeSystem ts = (JifTypeSystem) A.typeSystem();

        // there is no information gained by accessing a class statically.
        return ts.pathMap().N(A.pc()).NV(A.pc());

    }
}
