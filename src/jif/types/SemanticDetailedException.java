package jif.types;

import jif.JifOptions;
import polyglot.main.Options;
import polyglot.types.SemanticException;
import polyglot.util.Position;

/**
 * Allow a more detailed message if requested. 
 */
public class SemanticDetailedException extends SemanticException {

    public SemanticDetailedException() {
        super();
    }

    public SemanticDetailedException(Throwable cause) {
        super(cause);
    }

    public SemanticDetailedException(Position position) {
        super(position);
    }

    public SemanticDetailedException(String m) {
        super(m);
    }
    public SemanticDetailedException(String m, String detailed) {
        super(((JifOptions)Options.global).explainErrors?detailed:m);
    }

    public SemanticDetailedException(String m, Throwable cause) {
        super(m, cause);
    }
    public SemanticDetailedException(String m, String detailed, Throwable cause) {
        super(((JifOptions)Options.global).explainErrors?detailed:m,
                cause);
    }

    public SemanticDetailedException(String m, String detailed, Position position) {
        super(((JifOptions)Options.global).explainErrors?detailed:m,
                position);
    }

}
