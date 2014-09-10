/*
 * Copyright (c) 2014 University Nice Sophia Antipolis
 *
 * This file is part of btrplace.
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package btrplace.solver;

import btrplace.CoreConstraint;
import btrplace.model.Instance;
import btrplace.model.Model;
import btrplace.model.constraint.OptConstraint;
import btrplace.model.constraint.SatConstraint;
import btrplace.plan.ReconfigurationPlan;

import java.util.Collection;

/**
 * Basic interface for a reconfiguration algorithm.
 *
 * @author Fabien Hermenier
 */
@CoreConstraint(name = "noVMsOnOfflineNodes", inv = "!(n : nodes) nodeState(n) /= online --> card(hosted(n)) = 0")
@CoreConstraint(name = "toRunning", inv = "!(v : vms) vmState(v) = running --> ^vmState(v) : {ready, running, sleeping}")
@CoreConstraint(name = "toReady", inv = "!(v : vms) vmState(v) = ready --> ^vmState(v) : {ready, running}")
@CoreConstraint(name = "toSleeping", inv = "!(v : vms) vmState(v) = sleeping --> ^vmState(v) : {running, sleeping}")
public interface ReconfigurationAlgorithm {
    /**
     * Compute a reconfiguration plan to reach a solution to the model
     *
     * @param i     the current model
     * @param cstrs the satisfaction-oriented constraints that must be considered
     * @param obj   the optimization-oriented constraint that must be considered
     * @return the plan to execute to reach the new solution or {@code null} if there is no
     * solution.
     * @throws SolverException if an error occurred while trying to solve the problem
     */
    ReconfigurationPlan solve(Model i, Collection<SatConstraint> cstrs, OptConstraint obj) throws SolverException;

    /**
     * Compute a reconfiguration plan to reach a solution to an instance
     *
     * @param i the instance to solve
     * @return the plan to execute to reach the new solution or {@code null} if there is no
     * solution.
     * @throws SolverException if an error occurred while trying to solve the problem
     */
    ReconfigurationPlan solve(Instance i) throws SolverException;
}
