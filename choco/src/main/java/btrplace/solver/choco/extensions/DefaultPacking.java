/*
 * Copyright (c) 2013 University of Nice Sophia-Antipolis
 *
 * This file is part of btrplace.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package btrplace.solver.choco.extensions;

import btrplace.solver.SolverException;
import btrplace.solver.choco.ReconfigurationProblem;
import solver.Cause;
import solver.Solver;
import solver.constraints.IntConstraintFactory;
import solver.exception.ContradictionException;
import solver.variables.IntVar;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation that relies on the pack constraint bundled with Choco.
 * This is a pretty slow constraint... but it exists.
 *
 * @author Fabien Hermenier
 */
public class DefaultPacking implements Packing {

    private ReconfigurationProblem rp;

    private List<IntVar[]> loads;

    private List<IntVar[]> bins;

    private List<IntVar[]> sizes;

    private List<String> names;

    /**
     * A new constraint.
     *
     * @param p the associated problem
     */
    public DefaultPacking(ReconfigurationProblem p) {
        loads = new ArrayList<>();
        bins = new ArrayList<>();
        sizes = new ArrayList<>();
        names = new ArrayList<>();
        this.rp = p;

    }

    @Override
    public void addDim(String name, IntVar[] l, IntVar[] s, IntVar[] b) {
        this.loads.add(l);
        this.sizes.add(s);
        this.bins.add(b);
        this.names.add(name);
    }

    @Override
    public boolean commit() throws SolverException {
        Solver solver = rp.getSolver();
        int[][] iSizes = new int[sizes.size()][sizes.get(0).length];
        for (int i = 0; i < sizes.size(); i++) {
            IntVar[] s = sizes.get(i);
            int x = 0;
            for (IntVar ss : s) {
                iSizes[i][x++] = ss.getLB();
                try {
                    ss.instantiateTo(ss.getLB(), Cause.Null);
                } catch (ContradictionException ex) {
                    rp.getLogger().error("Unable post the packing constraint for dimension '{}': ", names.get(i));
                    return false;
                }
            }
            if (!rp.getFutureRunningVMs().isEmpty()) {
                solver.post(IntConstraintFactory.bin_packing(bins.get(0), iSizes[i], loads.get(i), 0));
            }
        }
        return true;
    }

    /** Builder associated to this constraint. */
    public static class Builder implements PackingBuilder {
        @Override
        public Packing build(ReconfigurationProblem p) {
            return new DefaultPacking(p);
        }
    }
}
