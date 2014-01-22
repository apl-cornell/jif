package jif.ast;

import polyglot.ast.Id;

public interface RifTransitionNode extends RifComponentNode {

    public Id name();

    public Id lstate();

    public Id rstate();

}
