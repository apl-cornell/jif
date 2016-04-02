package jif.types;

import java.io.IOException;

import polyglot.main.Report;
import polyglot.types.Type;
import polyglot.types.TypeObject;
import polyglot.types.VarInstance_c;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** An implementation of the <code>ParamInstance</code> interface.
 */
public class ParamInstance_c extends VarInstance_c implements ParamInstance {
    private static final long serialVersionUID = SerialVersionUID.generate();

    JifClassType container;
    Kind kind;

    public ParamInstance_c(JifTypeSystem ts, Position pos,
            JifClassType container, Kind kind, String name) {

        super(ts, pos, ts.Public().Static().Final(),
                kind == PRINCIPAL ? ts.Principal() : ts.Label(), name);
        this.kind = kind;
        this.container = container;
    }

    @Override
    public JifClassType container() {
        return container;
    }

    @Override
    public boolean equalsImpl(TypeObject o) {
        if (o instanceof ParamInstance) {
            ParamInstance that = (ParamInstance) o;
            return super.equalsImpl(that) && this.kind.equals(that.kind())
                    && this.container.fullName()
                            .equals(that.container().fullName());
        }

        return false;
    }

    @Override
    public ParamInstance container(JifClassType container) {
        ParamInstance_c n = (ParamInstance_c) copy();
        n.container = container;
        return n;
    }

    @Override
    public Kind kind() {
        return kind;
    }

    @Override
    public ParamInstance kind(Kind kind) {
        ParamInstance_c n = (ParamInstance_c) copy();
        n.kind = kind;
        return n;
    }

    @Override
    public ParamInstance name(String name) {
        ParamInstance_c n = (ParamInstance_c) copy();
        n.name = name;
        return n;
    }

    @Override
    public boolean isPrincipal() {
        return kind.isPrincipal();
    }

    @Override
    public boolean isLabel() {
        return isInvariantLabel() || isCovariantLabel();
    }

    @Override
    public boolean isInvariantLabel() {
        return kind.isInvariantLabel();
    }

    @Override
    public boolean isCovariantLabel() {
        return kind.isCovariantLabel();
    }

    @Override
    public String toString() {
        if (Report.should_report(Report.debug, 1)) {
            return kind + " " + container().name() + "." + name();
        }
        return kind + " " + name();
    }

    @SuppressWarnings("unused")
    private static final long writeObjectVersionUID = 1L;

    protected void writeObject(java.io.ObjectOutputStream out)
            throws IOException {
        // If you update this method in an incompatible way, increment
        // writeObjectVersionUID.

        out.writeObject(container);
        if (kind == INVARIANT_LABEL)
            out.writeInt(0);
        else if (kind == COVARIANT_LABEL)
            out.writeInt(1);
        else if (kind == PRINCIPAL)
            out.writeInt(2);
        else throw new IOException("invalid kind");
    }

    @SuppressWarnings("unused")
    private static final long readObjectVersionUID = 1L;

    protected void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        // If you update this method in an incompatible way, increment
        // readObjectVersionUID.

        this.container = (JifClassType) in.readObject();
        int k = in.readInt();
        switch (k) {
        case 0:
            kind = INVARIANT_LABEL;
            break;
        case 1:
            kind = COVARIANT_LABEL;
            break;
        case 2:
            kind = PRINCIPAL;
            break;
        default:
            throw new IOException("invalid kind");
        }
    }

    @Override
    public void setType(Type t) {
        //Do nothing
    }

    @Override
    public String fullName() {
        return name();
    }
}
