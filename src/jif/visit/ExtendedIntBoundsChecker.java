package jif.visit;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jif.visit.IntegerBoundsChecker.Bound;
import jif.visit.IntegerBoundsChecker.Bounds;
import polyglot.ast.NodeFactory;
import polyglot.frontend.Job;
import polyglot.types.TypeSystem;

public class ExtendedIntBoundsChecker extends IntegerBoundsChecker {

	public ExtendedIntBoundsChecker(Job job) {
		super(job);
	}

	public ExtendedIntBoundsChecker(Job job, TypeSystem ts, NodeFactory nf) {
		super(job, ts, nf);
	}
	
    /**
     * Extends IntegerBoundsChecker.Bounds with upper bound information.
     */
	protected static class Bounds extends IntegerBoundsChecker.Bounds {

        protected final Long numericUpperBound; // is always strict
        protected final Set<Bound> upperBounds;
        
		public Bounds() {
			super();
			numericUpperBound = null;
			upperBounds = Collections.emptySet();
		}
		
		public Bounds(Long numericLowerBound, Long numericUpperBound, 
				Set<Bound> lowerBounds, Set<Bound> upperBounds) {
			super(numericLowerBound, lowerBounds);
			this.numericUpperBound = numericUpperBound;
			this.upperBounds = upperBounds;
		}

        public boolean equals(Object o) {
        	if (!super.equals(o)) {
        		return false;
        	}
        	
            if (o instanceof Bounds) {
                Bounds that = (Bounds) o;
                
                if (this.numericUpperBound == that.numericUpperBound || 
                        (this.numericUpperBound != null && 
                        this.numericUpperBound.equals(that.numericUpperBound))) {
                    return this.upperBounds.equals(that.upperBounds);
                }
            }
            
            return false;
        }

        public int hashCode() {
            return super.hashCode() ^ 
                (numericUpperBound == null ? 0 : numericUpperBound.hashCode()) ^
                upperBounds.hashCode();
        }

        /**
         * Merge two bounds. The merge is conservative, meaning that
         * the numeric (greatest lower) bound is the lower of the two,
         * and the set of locals is the intersection of both. Similarly for
         * the upper bounds.
         */
        public Bounds merge(Bounds b1) {
            Bounds b0 = this;
            IntegerBoundsChecker.Bounds lowerMerge = super.merge(b1);
            
            if (lowerMerge == b0) {
                if (b0.numericUpperBound == null || 
                        (b1.numericUpperBound != null && 
                                b0.numericUpperBound >= b1.numericUpperBound)) {                
                    if (b1.upperBounds.containsAll(b0.upperBounds)) {
                        // the merge is just b0, so save some time and memory...
                        return b0;
                    }
                }
            }
            
            Set<Bound> bnds = new HashSet<Bound>();
            bnds.addAll(b0.upperBounds);
            bnds.retainAll(b1.upperBounds);
            Long numBnd = b0.numericUpperBound;
            
            if (b1.numericUpperBound == null || (numBnd != null && 
                    numBnd < b1.numericUpperBound)) {
                numBnd = b1.numericUpperBound;
            }
            
            return new Bounds(lowerMerge.numericLowerBound, numBnd, 
                    lowerMerge.lowerBounds, bnds);
        }

        /**
         * Merge two bounds. The merge is not conservative, meaning that the
         * facts in both branches are true. So the numeric (greatest lower)
         * bound is the greater of the two, and the set of locals is the union
         * of both. Similarly for upper bounds.
         */
        public Bounds mergeNonconservative(Bounds b1) {
            Bounds b0 = this;
            IntegerBoundsChecker.Bounds lowerMerge = super.merge(b1);
            
            if (lowerMerge == b0) {
                if (b1.numericUpperBound == null || 
                        (b0.numericUpperBound != null && 
                                b0.numericUpperBound <= b1.numericUpperBound)) {
                    if (b0.upperBounds.containsAll(b1.upperBounds)) {
                        // the merge is just b0, so save some time and memory...
                        return b0;
                    }
                }
            }
            
            Set<Bound> bnds = new HashSet<Bound>();
            bnds.addAll(b0.upperBounds);
            bnds.addAll(b1.upperBounds);
            Long numBnd = b0.numericUpperBound;
            
            if (numBnd == null || (b1.numericUpperBound != null && 
                    numBnd > b1.numericUpperBound)) {
                numBnd = b1.numericUpperBound;
            }

            return new Bounds(lowerMerge.numericLowerBound, numBnd, 
                    lowerMerge.lowerBounds, bnds);
        }

	}

}
