/*
 * Copyright (c) 2016 University Nice Sophia Antipolis
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

package org.btrplace.scheduler.choco.transition;

import org.btrplace.model.Node;
import org.btrplace.model.NodeState;
import org.btrplace.plan.ReconfigurationPlan;
import org.btrplace.plan.event.ShutdownNode;
import org.btrplace.scheduler.SchedulerException;
import org.btrplace.scheduler.choco.ReconfigurationProblem;
import org.btrplace.scheduler.choco.extensions.FastIFFEq;
import org.btrplace.scheduler.choco.extensions.FastImpliesEq;
import org.btrplace.scheduler.choco.extensions.TaskMonitor;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

/**
 * Model an action that allow a node to boot if necessary.
 * An estimation of the action duration must be provided through a
 * {@link org.btrplace.scheduler.choco.duration.ActionDurationEvaluator} accessible from
 * {@link org.btrplace.scheduler.choco.ReconfigurationProblem#getDurationEvaluators()} with the key {@code ShutdownNode.class}
 * <p>
 * The action is modeled as follow:
 * <ul>
 * <li>Definition of the node state. If the node is offline, then no VMs can run on it:
 * <ul>
 * <li>{@link #getState()} = {0,1}</li>
 * <li>{@link #getState()} = 0 -> {@code btrplace.solver.choco.ReconfigurationProblem.getNbRunningVMs()[nIdx] = 0}</li>
 * </ul>
 * </li>
 * <li>The action duration equals 0 if the node stays online. Otherwise, it equals the evaluated action duration {@code d}
 * retrieved from {@link org.btrplace.scheduler.choco.ReconfigurationProblem#getDurationEvaluators()}:
 * <ul>
 * <li>{@link #getDuration()} = {0,d}</li>
 * <li>{@link #getDuration()} = {@link #getState()} * d</li>
 * </ul>
 * </li>
 * <li>The action starts and ends necessarily before the end of the reconfiguration problem. Their difference
 * equals the action duration. If the node stays online then the action starts and ends at moment 0.
 * <ul>
 * <li>{@link #getStart()} < {@link org.btrplace.scheduler.choco.ReconfigurationProblem#getEnd()}</li>
 * <li>{@link #getEnd()} < {@link org.btrplace.scheduler.choco.ReconfigurationProblem#getEnd()}</li>
 * <li>{@link #getEnd()} = {@link #getStart()} + {@link #getDuration()}</li>
 * </ul>
 * </li>
 * <li>The node can consume hosting VMs and the beginning of the reconfiguration plan. If the node goes offline, it stops hosting VMs at
 * the beginning of the action. Otherwise, it equals the end of the reconfiguration process so that it is always capable
 * of hosting VMs.
 * <ul>
 * <li>{@link #getHostingStart()} = {@link org.btrplace.scheduler.choco.ReconfigurationProblem#getStart()}</li>
 * <li>{@code T} = { {@link #getStart()}, {@link org.btrplace.scheduler.choco.ReconfigurationProblem#getEnd()} }; {@link #getHostingEnd()} = T[{@link #getState()}]</li>
 * </ul>
 * </li>
 * </ul>
 * <p>
 * If the reconfiguration problem has a solution, a {@link org.btrplace.plan.event.ShutdownNode} action is inserted
 * into the resulting reconfiguration plan if the node is turned offline.
 *
 * @author Fabien Hermenier, Tu Dang
 */
public class ShutdownableNode implements NodeTransition {

    public static final String PREFIX = "shutdownableNode(";
    private Node node;
    private BoolVar isOnline;
    private BoolVar isOffline;
    private IntVar duration;
    private IntVar end;
    private IntVar hostingStart;
    private IntVar hostingEnd;
    private IntVar start;

    /**
     * Make a new model.
     *
     * @param rp the RP to use as a basis.
     * @param e  the node managed by the action
     * @throws org.btrplace.scheduler.SchedulerException if an error occurred
     */
    public ShutdownableNode(ReconfigurationProblem rp, Node e) throws SchedulerException {
        this.node = e;
        Model csp = rp.getModel();

        /*
            - If the node is hosting running VMs, it is necessarily online
            - If the node is offline, it is sure it cannot host any running VMs
        */
        isOnline = csp.boolVar(rp.makeVarLabel(PREFIX, e, ").online"));
        isOffline = isOnline.not();
        csp.post(new FastImpliesEq(isOffline, rp.getNbRunningVMs().get(rp.getNode(e)), 0));

        /*
        * D = {0, d}
        * D = St * d;
        */
        int d = rp.getDurationEvaluators().evaluate(rp.getSourceModel(), ShutdownNode.class, e);
        duration = csp.intVar(rp.makeVarLabel(PREFIX, e, ").duration"), new int[]{0, d});
        csp.post(new FastIFFEq(isOnline, duration, 0));

        //The moment of shutdown action consume
        /* As */
        start = rp.makeUnboundedDuration(PREFIX, e, ").start");
        //The moment of shutdown action end
        /* Ae */
        end = rp.makeUnboundedDuration(PREFIX, e, ").end");

        csp.post(csp.arithm(end, "<=", rp.getEnd()));

        /* Ae = As + D */
        TaskMonitor.build(start, duration, end);


        //The node is already online, so it can host VMs at the beginning of the RP
        hostingStart = rp.getStart();
        //The moment the node can no longer host VMs varies depending on its next state
        hostingEnd = rp.makeUnboundedDuration(PREFIX, e, ").hostingEnd");

        /*
          T = { As, RP.end}
          He = T[St]
         */
        csp.post(csp.element(hostingEnd, new IntVar[]{start, rp.getEnd()}, isOnline, 0));
    }


    @Override
    public boolean insertActions(Solution s, ReconfigurationPlan plan) {
        if (s.getIntVal(isOffline) == 1) {
            plan.add(new ShutdownNode(node, s.getIntVal(hostingEnd), s.getIntVal(end)));
        }
        return true;
    }

    @Override
    public String toString() {
        return "shutdownable(node=" + node + ", online=" + getState() + ")";
    }

    @Override
    public Node getNode() {
        return node;
    }

    @Override
    public IntVar getStart() {
        return start;
    }

    @Override
    public IntVar getEnd() {
        return end;
    }

    @Override
    public IntVar getDuration() {
        return duration;
    }

    @Override
    public BoolVar getState() {
        return isOnline;
    }

    @Override
    public IntVar getHostingStart() {
        return hostingStart;
    }

    @Override
    public IntVar getHostingEnd() {
        return hostingEnd;
    }

    @Override
    public NodeState getSourceState() {
        return NodeState.ONLINE;
    }

    /**
     * The builder devoted to a online->(online|offline) transition.
     */
    public static class Builder extends NodeTransitionBuilder {

        /**
         * New builder
         */
        public Builder() {
            super("shutdownable", NodeState.ONLINE);
        }

        @Override
        public NodeTransition build(ReconfigurationProblem r, Node n) throws SchedulerException {
            return new ShutdownableNode(r, n);
        }
    }

}
