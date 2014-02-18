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

package btrplace.solver.choco.constraint.minMTTR;

import btrplace.model.Mapping;
import btrplace.model.Node;
import btrplace.model.VM;
import btrplace.solver.choco.ReconfigurationProblem;
import btrplace.solver.choco.Slice;
import btrplace.solver.choco.actionModel.ActionModel;
import btrplace.solver.choco.actionModel.VMActionModel;
import memory.IStateInt;
import solver.search.strategy.selectors.VariableSelector;
import solver.variables.IntVar;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;


/**
 * A heuristic that first focus on scheduling the VMs
 * on nodes that are the source of actions liberating resources.
 * <p/>
 * For performance reason, the VM placement is put into a cache
 * that must be invalidated each time the placement is modified
 *
 * @author Fabien Hermenier
 */
public class OnStableNodeFirst implements VariableSelector<IntVar> {

    private IntVar[] hoster;

    private IntVar[] starts;

    private List<VM> vms;

    private int[] oldPos;

    private BitSet[] outs;

    private BitSet[] ins;

    private CMinMTTR obj;

    private IStateInt firstFree;

    private ReconfigurationProblem rp;

    private IntVar last;

    /**
     * Make a new heuristics
     *
     * @param lbl     the heuristic label (for debugging purpose)
     * @param rp      the problem to rely on
     * @param actions the actions to consider.
     * @param o       the objective to rely on
     */
    public OnStableNodeFirst(String lbl, ReconfigurationProblem rp, List<ActionModel> actions, CMinMTTR o) {

        this.rp = rp;
        firstFree = rp.getSolver().getEnvironment().makeInt(0);
        this.obj = o;
        Mapping cfg = rp.getSourceModel().getMapping();

        VMActionModel[] vmActions = rp.getVMActions();

        hoster = new IntVar[vmActions.length];
        starts = new IntVar[vmActions.length];

        this.vms = new ArrayList<>(rp.getFutureRunningVMs());

        oldPos = new int[hoster.length];
        outs = new BitSet[rp.getNodes().length];
        ins = new BitSet[rp.getNodes().length];
        for (int i = 0; i < rp.getNodes().length; i++) {
            outs[i] = new BitSet();
            ins[i] = new BitSet();
        }

        for (int i = 0; i < hoster.length; i++) {
            VMActionModel action = vmActions[i];
            Slice slice = action.getDSlice();
            if (slice != null) {
                IntVar h = slice.getHoster();
                IntVar s = slice.getStart();
                hoster[i] = h;
                if (s != rp.getEnd()) {
                    starts[i] = s;
                }
                VM vm = action.getVM();
                Node n = cfg.getVMLocation(vm);
                if (n == null) {
                    oldPos[i] = -1;
                } else {
                    oldPos[i] = rp.getNode(n);
                    //VM i was on node n
                    outs[rp.getNode(n)].set(i);
                }
            }
        }
    }

    private BitSet stays, move;

    /**
     * Invalidate the VM placement.
     * This must be called each time the placement is modified to
     * clear the VM placement cache.
     */
    public void invalidPlacement() {
        stays = null;
        move = null;
    }

    /**
     * For each node, we fill ins to indicate the VMs
     * that go on this node. We also fulfill stays and move
     * TODO: stays and move seems redundant !
     */
    private void makeIncomings() {
        if (stays == null && move == null) {
            for (BitSet in : ins) {
                in.clear();
            }
            stays = new BitSet();
            move = new BitSet();
            for (int i = 0; i < hoster.length; i++) {
                if (hoster[i] != null && hoster[i].instantiated()) {
                    int newPos = hoster[i].getValue();
                    if (oldPos[i] != -1 && newPos != oldPos[i]) {
                        //The VM has move
                        ins[newPos].set(i);
                        move.set(i);
                    } else if (newPos == oldPos[i]) {
                        stays.set(i);
                    }
                }
            }
        }
    }

    @Override
    public IntVar getVariable() {

        makeIncomings();
        IntVar v = getVMtoLeafNode();
        if (v == null) {
            last = null;
            return null;
        }

        v = getMovingVM();
        if (v != null) {
            obj.postCostConstraints();
            return v;
        }

        IntVar early = getEarlyVar();
        last = early != null ? early : minInf();
        return last;

    }

    @Override
    public boolean hasNext() {
        return last != null;
    }

    @Override
    public void advance() {
    }

    @Override
    public IntVar[] getScope() {
        return starts;
    }

    /**
     * Get the start moment for a VM that moves
     *
     * @return a start moment, or {@code null} if all the moments  are already instantiated
     */
    private IntVar getMovingVM() {
        //VMs that are moving
        for (int i = move.nextSetBit(0); i >= 0; i = move.nextSetBit(i + 1)) {
            if (starts[i] != null && !starts[i].instantiated()) {
                if (oldPos[i] != hoster[i].getValue()) {
                    return starts[i];
                }
            }
        }
        return null;
    }

    private IntVar minInf() {
        IntVar best = null;
        for (int i = firstFree.get(); i < starts.length; i++) {
            IntVar v = starts[i];
            if (i < vms.size() - 1) {
                VM vm = vms.get(i);
                if (vm != null && v != null) {
                    if (!v.instantiated()) {
                        if (best == null || best.getLB() < v.getLB()) {
                            best = v;
                            if (best.getLB() == 0) {
                                break;
                            }
                        }
                    } else {
                        firstFree.add(1);
                    }
                }
            }
        }
        if (best == null) {
            //Plug the cost constraints
            obj.postCostConstraints();
        }
        return best;
    }

    /**
     * Get the earliest un-instantiated start moment
     *
     * @return the variable, or {@code null} if all the start moments are already instantiated
     */
    private IntVar getEarlyVar() {
        IntVar earlyVar = null;
        for (int i = stays.nextSetBit(0); i >= 0; i = stays.nextSetBit(i + 1)) {
            if (starts[i] != null && !starts[i].instantiated()) {
                if (earlyVar == null) {
                    earlyVar = starts[i];
                } else {
                    if (earlyVar.getLB() > starts[i].getLB()) {
                        earlyVar = starts[i];
                    }
                }
            }
        }
        return earlyVar;
    }

    /**
     * Get the start moment for a VM that move to
     * a node where no VM will leave this node.
     * This way, we are pretty sure the action can be scheduled at 0.
     *
     * @return a start moment, or {@code null} if there is no more un-schedule actions to leaf nodes
     */
    private IntVar getVMtoLeafNode() {
        for (int x = 0; x < outs.length; x++) {
            if (outs[x].cardinality() == 0) {
                //no outgoing VMs, can be launched directly.
                BitSet in = ins[x];
                for (int i = in.nextSetBit(0); i >= 0; i = in.nextSetBit(i + 1)) {
                    if (starts[i] != null && !starts[i].instantiated()) {
                    }
                }
            }
        }
        return null;
    }
}
