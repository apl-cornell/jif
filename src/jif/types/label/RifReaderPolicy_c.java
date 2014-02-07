package jif.types.label;

import java.util.List;
import java.util.Set;

import jif.ast.RifComponentNode;
import jif.ast.RifStateNode;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.LabelSubstitution;
import jif.types.PathMap;
import jif.types.hierarchy.LabelEnv;
import jif.types.hierarchy.LabelEnv.SearchState;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** An implementation of the <code>PolicyLabel</code> interface.
 */
public class RifReaderPolicy_c extends Policy_c implements RifReaderPolicy {
    private static final long serialVersionUID = SerialVersionUID.generate();

    private final List<RifComponentNode> components; //this should not be the correct type! 
                                                     //be sure to extract the disambprincipals and not principals!

    public RifReaderPolicy_c(List<RifComponentNode> components,
            JifTypeSystem ts, Position pos) {
        super(ts, pos);
        this.components = components;
    }

    @Override
    public List<RifComponentNode> components() {
        return this.components;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public boolean isCanonical() {
        for (RifComponentNode c : this.components) {
            if (c instanceof RifStateNode) {
                for (Principal p : ((RifStateNode) c).disambprincipals()) {
                    if (!p.isCanonical()) {
                        return false;
                    }
                }
            }

        }
        return true;
    }

    @Override
    public boolean isRuntimeRepresentable() {
        for (RifComponentNode c : this.components) {
            if (c instanceof RifStateNode) {
                for (Principal p : ((RifStateNode) c).disambprincipals()) {
                    if (!p.isRuntimeRepresentable()) {
                        return false;
                    }
                }
            }

        }
        return true;
    }

    @Override
    protected Policy simplifyImpl() {
        return this;
    }

    @Override
    public boolean equalsImpl(TypeObject o) {
        /*  if (this == o) return true;
          if (o instanceof ReaderPolicy_c) {
              ReaderPolicy_c that = (ReaderPolicy_c) o;
              if (this.owner == that.owner
                      || (this.owner != null && this.owner.equals(that.owner))) {
                  return this.reader.equals(that.reader);
              }
          }*/
        return false;
    }

    @Override
    public int hashCode() {
        // return (owner == null ? 0 : owner.hashCode()) ^ reader.hashCode()
        //         ^ 948234;
        return 0;
    }

    @Override
    public boolean leq_(ConfPolicy p, LabelEnv env, SearchState state) {
        /*   if (this.isBottomConfidentiality() || p.isTopConfidentiality())
              return true;

          // if this policy is o:_, then o allows
          // all principals to read info, and thus does
          // not restrict who may read
          if (reader.isBottomPrincipal()) {
              return true;
          }
          if (p instanceof ReaderPolicy) {
              ReaderPolicy that = (ReaderPolicy) p;
              // this = { o  : .. ri .. }
              // that = { o' : .. rj' .. }

              // o' >= o
              if (!env.actsFor(that.owner(), this.owner)) {
                  return false;
              }

              return env.actsFor(that.reader(), this.owner())
                      || env.actsFor(that.reader(), this.reader());
          } */
        return false;
    }

    @Override
    public String toString(Set<Label> printedLabels) {
        /*  StringBuffer sb = new StringBuffer(owner.toString());
          sb.append("->");
          if (!reader.isTopPrincipal()) sb.append(reader.toString());
          return sb.toString(); */
        return "";
    }

    @Override
    public List<Type> throwTypes(TypeSystem ts) {
        /* List<Type> throwTypes = new ArrayList<Type>();
         throwTypes.addAll(owner.throwTypes(ts));
         throwTypes.addAll(reader.throwTypes(ts));
         return throwTypes;*/
        return null;
    }

    @Override
    public Policy subst(LabelSubstitution substitution)
            throws SemanticException {
        /*   boolean changed = false;

           Principal newOwner = owner.subst(substitution);
           if (newOwner != owner) changed = true;
           Principal newReader = reader.subst(substitution);
           if (newReader != reader) changed = true;

           if (!changed) return substitution.substPolicy(this);

           JifTypeSystem ts = (JifTypeSystem) typeSystem();
           ReaderPolicy newPolicy =
                   ts.readerPolicy(this.position(), newOwner, newReader);
           return substitution.substPolicy(newPolicy); */
        return null;
    }

    @Override
    public PathMap labelCheck(JifContext A, LabelChecker lc) {
        // check each principal in turn.
        /*   PathMap X = owner.labelCheck(A, lc);
           A.setPc(X.N(), lc);
           PathMap Xr = reader.labelCheck(A, lc);
           X = X.join(Xr);
           return X; */
        return null;
    }

    @Override
    public boolean isBottomConfidentiality() {
        // return owner.isBottomPrincipal() && reader.isBottomPrincipal();
        return false;
    }

    @Override
    public boolean isTopConfidentiality() {
        //return owner.isTopPrincipal() && reader.isTopPrincipal();
        return false;
    }

    @Override
    public boolean isTop() {
        return isTopConfidentiality();
    }

    @Override
    public boolean isBottom() {
        return isBottomConfidentiality();
    }

    @Override
    public ConfPolicy meet(ConfPolicy p) {
        JifTypeSystem ts = (JifTypeSystem) this.ts;
        return ts.meet(this, p);
    }

    @Override
    public ConfPolicy join(ConfPolicy p) {
        JifTypeSystem ts = (JifTypeSystem) this.ts;
        return ts.join(this, p);
    }
}
