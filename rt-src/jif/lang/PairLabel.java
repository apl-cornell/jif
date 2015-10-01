package jif.lang;

import java.util.HashSet;
import java.util.Set;

import jif.lang.PrincipalUtil.DelegationPair;

/**
 * A Label is the runtime representation of a Jif label. A Label consists of a
 * set of components, each of which is a {@link jif.lang.IntegPolicy Policy}.
 * 
 */
public final class PairLabel implements Label {
    private final ConfPolicy confPol;
    private final IntegPolicy integPol;
    private Integer hashCode = null;
    private LabelUtil labelUtil;

    public PairLabel(LabelUtil labelUtil, ConfPolicy confPol,
            IntegPolicy integPol) {
        this.labelUtil = labelUtil;
        this.confPol = confPol;
        this.integPol = integPol;
        if (confPol == null || integPol == null)
            throw new NullPointerException();
    }

    @Override
    public boolean relabelsTo(Label l, Set<DelegationPair> s) {
        if (l instanceof PairLabel) {
            PairLabel that = (PairLabel) l;
            if (this == that || this.equals(that)) return true;
            Set<DelegationPair> temp = new HashSet<DelegationPair>();
            if (labelUtil.relabelsTo(this.confPol, that.confPol, temp)
                    && labelUtil.relabelsTo(this.integPol, that.integPol,
                            temp)) {
                s.addAll(temp);
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (hashCode == null) {
            hashCode = new Integer(confPol.hashCode() ^ integPol.hashCode());
        }
        return hashCode.intValue();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PairLabel) {
            PairLabel that = (PairLabel) o;
            return this == that || (this.hashCode() == that.hashCode()
                    && this.confPol.equals(that.confPol)
                    && this.integPol.equals(that.integPol));
        }
        return false;
    }

    @Override
    public final String toString() {
        String c = confPol.toString();
        String i = integPol.toString();
        if (c.length() > 0 && i.length() > 0) {
            return "{" + c + "; " + i + "}";

        }
        // at least one of them is length 0.
        return "{" + c + i + "}";
    }

    @Override
    public final Label join(Label l) {
        return join(l, true);
    }

    @Override
    public final Label join(Label l, boolean simplify) {
        return labelUtil.join(this, l, simplify);
    }

    @Override
    public Label meet(Label l) {
        return meet(l, true);
    }

    @Override
    public Label meet(Label l, boolean simplify) {
        return labelUtil.meet(this, l, simplify);
    }

    @Override
    public ConfPolicy confPolicy() {
        return confPol;
    }

    @Override
    public IntegPolicy integPolicy() {
        return integPol;
    }
}
