package jif;

import polyglot.main.Report;

/**
 * Extension information for jif extension.
 */
public class Topics {
    public static String jif = "jif";
    public static String solver = "solver";
    public static String error = "error";
    public static String labels = "labels";

    static {
        Report.topics.add(solver);
        Report.topics.add(jif);
        Report.topics.add(error);
        Report.topics.add(labels);
    }
}
