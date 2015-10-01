package jif;

import jif.visit.LabelCheckPass;
import jif.visit.LabelChecker;
import polyglot.frontend.Job;
import polyglot.frontend.Pass;
import polyglot.frontend.goals.SourceFileGoal;

public class LabelCheckGoal extends SourceFileGoal {
    private final boolean warningsEnabled;

    public LabelCheckGoal(Job job, boolean warningsEnabled) {
        super(job);
        this.warningsEnabled = warningsEnabled;
    }

    @Override
    public Pass createPass(polyglot.frontend.ExtensionInfo extInfo) {
        ExtensionInfo jifext = (ExtensionInfo) extInfo;
        LabelChecker lc = jifext.createLabelChecker(this.job(), warningsEnabled,
                !jifext.getJifOptions().solveGlobally,
                !jifext.getJifOptions().solveGlobally, true);
        return new LabelCheckPass(this, this.job(), lc);
    }
}
