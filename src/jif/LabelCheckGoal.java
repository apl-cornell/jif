package jif;

import jif.ast.JifNodeFactory;
import jif.types.JifTypeSystem;
import jif.visit.LabelCheckPass;
import jif.visit.LabelChecker;
import polyglot.frontend.Job;
import polyglot.frontend.Pass;
import polyglot.frontend.goals.SourceFileGoal;

public class LabelCheckGoal extends SourceFileGoal {
    public LabelCheckGoal(Job job) {
        super(job);
    }

    public Pass createPass(polyglot.frontend.ExtensionInfo extInfo) {
        ExtensionInfo jifext = (ExtensionInfo)extInfo;
        final JifTypeSystem ts = (JifTypeSystem) jifext.typeSystem();
        final JifNodeFactory nf = (JifNodeFactory) jifext.nodeFactory();
        return new LabelCheckPass(this, this.job(), 
                                 new LabelChecker(this.job(), ts, nf, !jifext.getJifOptions().solveGlobally, !jifext.getJifOptions().solveGlobally));
    }
}