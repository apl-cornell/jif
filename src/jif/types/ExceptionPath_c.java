package jif.types;

import polyglot.types.Type;
import polyglot.types.TypeObject;

/** An implementation of the <code>ExceptionPath</code> interface. 
 */
public class ExceptionPath_c implements ExceptionPath {
    Type type;

    public ExceptionPath_c(Type type) {
        this.type = type;
    }

    @Override
    public Type exception() {
        return type;
    }

    @Override
    public String toString() {
        return type.toString();
    }

    public boolean equalsImpl(TypeObject o) {
        if (!(o instanceof ExceptionPath)) {
            return false;
        } else {
            return this.type.equals(((ExceptionPath) o).exception());
        }
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }
}
