package jif.types;

import jif.types.label.Label;
import jif.types.label.VarLabel;

//A trace consists of variable, the label that flows in or out, and the equation that caused the flow
public class InformationFlowTrace {
    public VarLabel varlbl;
    public LabelEquation equ;
    public Direction dir;
    public Label lblflows;

    public enum Direction {
        IN, BOTH
    };

    public InformationFlowTrace(VarLabel l, Label from, Direction dir,
            LabelEquation e) {
        this.varlbl = l;
        this.equ = e;
        this.dir = dir;
        this.lblflows = from;
    }

    @Override
    public String toString() {
        String dirstr = "";
        if (dir == Direction.IN) dirstr = "in";
        if (dir == Direction.BOTH) dirstr = "in/out";
        return "Label " + lblflows.toString() + "flows " + dirstr + " variable "
                + varlbl.name() + " according to constraint" + equ.toString();
    }
}
