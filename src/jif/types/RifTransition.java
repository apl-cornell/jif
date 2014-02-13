package jif.types;

import polyglot.ast.Id;

public interface RifTransition extends RifComponent {

    public Id name();

    public Id lstate();

    public Id rstate();

}
