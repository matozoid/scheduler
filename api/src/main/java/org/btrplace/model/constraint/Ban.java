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

package org.btrplace.model.constraint;

import org.btrplace.model.Node;
import org.btrplace.model.VM;

import java.util.*;

/**
 * A constraint to disallow the given VM, when running,
 * to be hosted on a given set of nodes.
 *
 * @author Fabien Hermenier
 */
public class Ban extends SimpleConstraint {

    private VM vm;

    private Collection<Node> nodes;

    /**
     * Make a new discrete constraint.
     *
     * @param vm    the VM identifiers
     * @param nodes the nodes identifiers
     */
    public Ban(VM vm, Collection<Node> nodes) {
        this(vm, nodes, false);
    }

    /**
     * Make a new constraint.
     *
     * @param vm         the VM identifiers
     * @param nodes      the nodes identifiers
     * @param continuous {@code true} for a continuous constraint.
     */
    public Ban(VM vm, Collection<Node> nodes, boolean continuous) {
        super(continuous);
        this.vm = vm;
        this.nodes = nodes;
    }

    @Override
    public Collection<Node> getInvolvedNodes() {
        return nodes;
    }

    @Override
    public Collection<VM> getInvolvedVMs() {
        return Collections.singleton(vm);
    }

    @Override
    public String toString() {
        return "ban(" + "vm=" + vm + ", nodes=" + nodes + ", " + (isContinuous() ? "continuous" : "discrete") + ")";
    }

    @Override
    public BanChecker getChecker() {
        return new BanChecker(this);
    }

    /**
     * Instantiate discrete constraints for a collection of VMs.
     *
     * @param vms   the VMs to integrate
     * @param nodes the hosts to disallow
     * @return the associated list of constraints
     */
    public static List<Ban> newBan(Collection<VM> vms, Collection<Node> nodes) {
        List<Ban> l = new ArrayList<>(vms.size());
        for (VM v : vms) {
            l.add(new Ban(v, nodes));
        }
        return l;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Ban ban = (Ban) o;
        return isContinuous() == ban.isContinuous() &&
                Objects.equals(vm, ban.vm) &&
                Objects.equals(nodes, ban.nodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vm, nodes, isContinuous());
    }
}
