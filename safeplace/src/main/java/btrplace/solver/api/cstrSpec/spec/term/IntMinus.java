package btrplace.solver.api.cstrSpec.spec.term;

import btrplace.solver.api.cstrSpec.verification.spec.SpecModel;

/**
 * @author Fabien Hermenier
 */
public class IntMinus extends Minus<Integer> {

    public IntMinus(Term<Integer> t1, Term<Integer> t2) {
        super(t1, t2);
    }

    @Override
    public Integer eval(SpecModel mo) {
        Integer o1 = a.eval(mo);
        Integer o2 = b.eval(mo);
        if (o1 == null || o2 == null) {
            return null;
        }
        return o1 - o2;
    }
}
