package jif.types;

public class FailedConstraintSnapshot {
    Equation failedConstraint;
    VarMap bounds;

    public FailedConstraintSnapshot(Equation equ, VarMap bounds) {
        failedConstraint = equ;
        this.bounds = bounds;
    }
}
