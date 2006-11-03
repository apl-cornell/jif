package jif.lang;


/**
 * A Label is the runtime representation of a Jif label. A Label consists of a
 * set of components, each of which is a {@link jif.lang.IntegPolicy Policy}.
 *  
 */
public final class PairLabel implements Label
{
    private final ConfPolicy confPol;
    private final IntegPolicy integPol;
    private Integer hashCode = null;
    
    public PairLabel(ConfPolicy confPol, IntegPolicy integPol) {
        this.confPol = confPol;
        this.integPol = integPol;
    }

    public boolean relabelsTo(Label l) {
        if (l instanceof PairLabel) {
            PairLabel that = (PairLabel)l;            
            if (this == that || this.equals(that)) return true;
            return (this.confPol.relabelsTo(that.confPol) &&
                    this.integPol.relabelsTo(that.integPol));
        }
        return false;
    }
    
    public int hashCode() {
        if (hashCode == null) {
            hashCode = new Integer(confPol.hashCode() ^ integPol.hashCode());
        }
        return hashCode.intValue();
    }
    
    public boolean equals(Object o) {
        if (o instanceof PairLabel) {
            PairLabel that = (PairLabel)o;
            return this.hashCode() == that.hashCode() && (this == that || (this.confPol.equals(that.confPol) &&
                    this.integPol.equals(that.integPol)));
        }
        return false;
    }
    public final String toString() {
        String c = confPol.toString();
        String i = integPol.toString();
        if (c.length() > 0 && i.length() > 0) {
            return "{" + c + "; " + i + "}";
           
        }
        // at least one of them is length 0.
        return "{" + c  + i + "}";
    }
    
    public final Label join(Label l) {
        return LabelUtil.join(this, l);
    }

    public Label meet(Label l) {
        return LabelUtil.meet(this, l);
    }

    public ConfPolicy confPolicy() {
        return confPol;
    }

    public IntegPolicy integPolicy() {
        return integPol;
    }
}
