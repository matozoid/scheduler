package btrplace.solver.api.cstrSpec.test;

import btrplace.solver.api.cstrSpec.CTestCaseResult;
import btrplace.solver.api.cstrSpec.runner.CTestCaseReport;
import btrplace.solver.api.cstrSpec.runner.CTestCasesRunner;
import btrplace.solver.api.cstrSpec.runner.TestsScanner;

import java.util.List;

/**
 * @author Fabien Hermenier
 */
public class Test {

    private static int verbosity = 2;

    public static void main(String[] args) {
        TestsScanner scanner = new TestsScanner();
        long totalSt = System.currentTimeMillis();
        scanner.restrictToTest("Bench");

        List<CTestCasesRunner> runners = null;
        try {
            runners = scanner.scan();
        } catch (Exception e) {
        }

        if (runners.isEmpty()) {
            System.out.println("No tests found");
            System.exit(0);
        }
        boolean errHeader = false;
        int ok = 0, fp = 0, fn = 0;
        for (CTestCasesRunner runner : runners) {
            CTestCaseReport report = new CTestCaseReport(runner.id());
            long st = System.currentTimeMillis();
            for (CTestCaseResult res : runner) {
                report.add(res);
            }
            long ed = System.currentTimeMillis();
            report.report(runner.report());
            report.duration(ed - st);
            ok += report.ok();
            fp += report.fp();
            fn += report.fn();
            if (report.report() != null) {
                errHeader = true;
            }
            if (report.report() != null || report.fn() > 0 || report.fp() > 0 || verbosity > 1) {
                System.out.println(report.pretty());
                /*if (!errHeader && verbosity >= 1) {
                    System.out.println("Failed tests:");
                    errHeader = true;
                } */

            }
        }

        if (!errHeader && !runners.isEmpty()) {
            System.out.println("SUCCESS !");
        }
        long ed = System.currentTimeMillis();
        System.out.println("\nTests run: " + (ok + fp + fn) + "; F/P: " + fp + ", F/N: " + fn + " (" + (ed - totalSt) + " ms)");
    }
}
