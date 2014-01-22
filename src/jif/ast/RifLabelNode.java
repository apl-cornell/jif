package jif.ast;

import java.util.List;

public interface RifLabelNode extends LabelNode {
    public List<List<RifComponentNode>> components();

}
