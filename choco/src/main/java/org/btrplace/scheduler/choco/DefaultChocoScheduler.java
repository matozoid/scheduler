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

package org.btrplace.scheduler.choco;

import org.btrplace.model.Instance;
import org.btrplace.model.Model;
import org.btrplace.model.constraint.Fence;
import org.btrplace.model.constraint.MinMTTR;
import org.btrplace.model.constraint.OptConstraint;
import org.btrplace.model.constraint.SatConstraint;
import org.btrplace.model.view.network.Network;
import org.btrplace.plan.ReconfigurationPlan;
import org.btrplace.plan.event.MigrateVM;
import org.btrplace.scheduler.SchedulerException;
import org.btrplace.scheduler.choco.constraint.ConstraintMapper;
import org.btrplace.scheduler.choco.duration.DurationEvaluators;
import org.btrplace.scheduler.choco.runner.InstanceSolver;
import org.btrplace.scheduler.choco.runner.SolvingStatistics;
import org.btrplace.scheduler.choco.runner.single.SingleRunner;
import org.btrplace.scheduler.choco.transition.TransitionFactory;
import org.btrplace.scheduler.choco.view.ModelViewMapper;
import org.btrplace.scheduler.choco.view.SolverViewBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link ChocoScheduler}.
 * A same instance cannot be used to solve multiple problems simultaneously.
 * <p>
 * By default, the algorithm relies on a {@link SingleRunner} solver.
 *
 * @author Fabien Hermenier
 */
public class DefaultChocoScheduler implements ChocoScheduler {

    private Parameters params;

    private InstanceSolver runner;

    /**
     * Make a new algorithm.
     *
     * @param ps the parameters to use to configure the algorithm
     */
    public DefaultChocoScheduler(DefaultParameters ps) {
        params = ps;
        runner = new SingleRunner();
    }

    /**
     * Make a new algorithm with default parameters.
     */
    public DefaultChocoScheduler() {
        this(new DefaultParameters());
    }

    @Override
    public Parameters doOptimize(boolean b) {
        return params.doOptimize(b);
    }

    @Override
    public boolean doOptimize() {
        return params.doOptimize();
    }

    @Override
    public Parameters setTimeLimit(int t) {
        return params.setTimeLimit(t);
    }

    @Override
    public int getTimeLimit() {
        return params.getTimeLimit();
    }

    @Override
    public Parameters doRepair(boolean b) {
        return params.doRepair(b);
    }

    @Override
    public boolean doRepair() {
        return params.doRepair();
    }

    @Override
    public ReconfigurationPlan solve(Model i, Collection<SatConstraint> cstrs) throws SchedulerException {
        return solve(i, cstrs, new MinMTTR());
    }

    @Override
    public ReconfigurationPlan solve(Instance i) throws SchedulerException {
        return solve(i.getModel(), i.getSatConstraints(), i.getOptConstraint());
    }

    @Override
    public ReconfigurationPlan solve(Model i, Collection<SatConstraint> cstrs, OptConstraint opt) throws SchedulerException {
        
        // If a network view is attached, ensure that all the migrations' destination node are defined
        Network net = (Network) i.getView(Network.VIEW_ID);
        if  (net != null) {
            
            // The network view is useless to take placement decisions
            i.detach(net);

            // Solve a first time using placement oriented MinMTTR optimisation constraint
            ReconfigurationPlan p = runner.solve(params, new Instance(i, cstrs, new MinMTTR()));
            if (p == null) return null;

            // Remember each migration's destination node
            if (!p.getActions().isEmpty()) {

                // Add fence constraints for each destination node chosen
                List<SatConstraint> newCstrs = p.getActions().stream()
                        .filter(a -> a instanceof MigrateVM)
                        .map(a -> new Fence(((MigrateVM) a).getVM(),
                                            Collections.singleton(((MigrateVM) a).getDestinationNode())))
                        .collect(Collectors.toList());

                // Add the new Fence constraints to the list
                if (!newCstrs.isEmpty()) cstrs.addAll(newCstrs);
            }

            // Re-attach the network view
            i.attach(net);
        }

        // Solve and return the computed plan
        return runner.solve(params, new Instance(i, cstrs, opt));
    }

    @Override
    public ConstraintMapper getConstraintMapper() {
        return params.getConstraintMapper();
    }

    @Override
    public DurationEvaluators getDurationEvaluators() {
        return params.getDurationEvaluators();
    }

    @Override
    public SolvingStatistics getStatistics() throws SchedulerException {
        return runner.getStatistics();
    }

    @Override
    public Parameters setMaxEnd(int end) {
        return params.setMaxEnd(end);
    }

    @Override
    public int getMaxEnd() {
        return params.getMaxEnd();
    }

    @Override
    public ModelViewMapper getViewMapper() {
        return params.getViewMapper();
    }

    @Override
    public Parameters setViewMapper(ModelViewMapper m) {
        return params.setViewMapper(m);
    }

    @Override
    public Parameters setVerbosity(int lvl) {
        return params.setVerbosity(lvl);
    }

    @Override
    public Parameters setConstraintMapper(ConstraintMapper map) {
        return params.setConstraintMapper(map);
    }

    @Override
    public Parameters setDurationEvaluators(DurationEvaluators d) {
        return params.setDurationEvaluators(d);
    }

    @Override
    public int getVerbosity() {
        return params.getVerbosity();
    }

    @Override
    public InstanceSolver getInstanceSolver() {
        return runner;
    }

    @Override
    public void setInstanceSolver(InstanceSolver p) {
        runner = p;
    }

    @Override
    public void setTransitionFactory(TransitionFactory amf) {
        params.setTransitionFactory(amf);
    }

    @Override
    public TransitionFactory getTransitionFactory() {
        return params.getTransitionFactory();
    }

    @Override
    public void addSolverViewBuilder(SolverViewBuilder b) {
        params.addSolverViewBuilder(b);
    }

    @Override
    public boolean removeSolverViewBuilder(SolverViewBuilder b) {
        return params.removeSolverViewBuilder(b);
    }

    @Override
    public Collection<SolverViewBuilder> getSolverViews() {
        return params.getSolverViews();
    }
}
