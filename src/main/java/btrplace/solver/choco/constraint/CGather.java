/*
 * Copyright (c) 2012 University of Nice Sophia-Antipolis
 *
 * This file is part of btrplace.
 *
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

package btrplace.solver.choco.constraint;

import btrplace.model.Mapping;
import btrplace.model.Model;
import btrplace.model.SatConstraint;
import btrplace.model.constraint.Gather;
import btrplace.solver.SolverException;
import btrplace.solver.choco.*;
import choco.cp.solver.CPSolver;
import choco.kernel.solver.ContradictionException;
import choco.kernel.solver.variables.integer.IntDomainVar;

import java.util.*;

/**
 * Choco implementation of {@link btrplace.model.constraint.Gather}.
 *
 * @author Fabien Hermenier
 */
public class CGather implements ChocoSatConstraint {

    private Gather cstr;

    /**
     * Make a new constraint.
     *
     * @param g the constraint to rely on
     */
    public CGather(Gather g) {
        cstr = g;
    }

    @Override
    public boolean inject(ReconfigurationProblem rp) throws SolverException {
        List<Slice> l = new ArrayList<Slice>();
        for (UUID vm : cstr.getInvolvedVMs()) {
            VMActionModel a = rp.getVMAction(vm);
            Slice dSlice = a.getDSlice();
            if (dSlice != null) {
                l.add(dSlice);
            }
        }
        CPSolver s = rp.getSolver();
        if (cstr.isContinuous()) {
            //Check for already running VMs
            Mapping map = rp.getSourceModel().getMapping();
            UUID loc = null;
            for (UUID vm : cstr.getInvolvedVMs()) {
                if (map.getRunningVMs().contains(vm)) {
                    UUID node = map.getVMLocation(vm);
                    if (loc == null) {
                        loc = node;
                    } else if (!loc.equals(node)) {
                        rp.getLogger().error("Some VMs in '{}' are already running but not co-located", cstr.getInvolvedVMs());
                        return false;
                    }
                }
            }
            int nIdx = rp.getNode(loc);
            for (UUID vm : cstr.getInvolvedVMs()) {
                if (rp.getFutureRunningVMs().contains(vm)) {
                    Slice dSlice = rp.getVMAction(vm).getDSlice();
                    try {
                        dSlice.getHoster().setVal(nIdx);
                    } catch (ContradictionException ex) {
                        rp.getLogger().error("Unable to maintain the co-location of all the future-running VMs in '{}': ", cstr.getInvolvedVMs(), ex.getMessage());
                        return false;
                    }
                }
            }
        } else {
            for (int i = 0; i < l.size(); i++) {
                for (int j = 0; j < i; j++) {
                    Slice s1 = l.get(i);
                    Slice s2 = l.get(j);
                    IntDomainVar i1 = s1.getHoster();
                    IntDomainVar i2 = s2.getHoster();
                    if (i1.isInstantiated() && i2.isInstantiated()) {
                        if (i1.getVal() != i2.getVal()) {
                            rp.getLogger().error("Unable to force VM '" + s1.getSubject() + "' to be co-located with '" + s2.getSubject() + "'");
                            return false;
                        }
                    } else {
                        try {
                            if (i1.isInstantiated()) {
                                i2.setVal(i1.getVal());
                            } else if (i2.isInstantiated()) {
                                i1.setVal(i2.getVal());
                            } else {
                                s.post(s.eq(i1, i2));
                            }
                        } catch (ContradictionException ex) {
                            rp.getLogger().error("Unable to force VM '" + s1.getSubject() + "' to be co-located with '" + s2.getSubject() + "'");
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return cstr.toString();
    }


    @Override
    public Set<UUID> getMisPlacedVMs(Model m) {
        if (!cstr.isSatisfied(m).equals(SatConstraint.Sat.SATISFIED)) {
            return new HashSet<UUID>(cstr.getInvolvedVMs());
        }
        return Collections.emptySet();
    }

    /**
     * The builder associated to that constraint.
     */
    public static class Builder implements ChocoSatConstraintBuilder {
        @Override
        public Class<? extends SatConstraint> getKey() {
            return Gather.class;
        }

        @Override
        public CGather build(SatConstraint cstr) {
            return new CGather((Gather) cstr);
        }
    }
}
