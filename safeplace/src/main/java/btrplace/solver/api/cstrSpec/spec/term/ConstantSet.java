package btrplace.solver.api.cstrSpec.spec.term;

import btrplace.solver.api.cstrSpec.spec.type.Type;
import btrplace.solver.api.cstrSpec.verification.spec.SpecModel;

import java.util.Collections;
import java.util.Set;

/**
 * @author Fabien Hermenier
 */
public class ConstantSet extends Primitive {

    private Set cnt;

    public ConstantSet(String lbl, Type t, Set cnt) {
        super(lbl, t);
        this.cnt = Collections.unmodifiableSet(cnt);
    }

    @Override
    public Set eval(SpecModel m) {
        return cnt;
    }
}
