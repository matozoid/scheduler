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

package org.btrplace.btrpsl.element;

import java.util.Objects;

/**
 * Denotes an number operand.
 * The value may an integer or a real, either positive or negative.
 * An integer can be expressed in base 10, 8 (octal) or 16 (hexadecimal) while
 * a real is necessarily expressed in base 10.
 *
 * @author Fabien Hermenier
 */
public class BtrpNumber extends DefaultBtrpOperand {

    /**
     * The current real value.
     */
    private double dVal;

    private final boolean isInteger;


    /**
     * Pre-made true value.
     */
    public static final BtrpNumber TRUE = new BtrpNumber(1, Base.BASE_10);

    /**
     * Pre-made false value.
     */
    public static final BtrpNumber FALSE = new BtrpNumber(0, Base.BASE_10);

    /**
     * The base of the integer.
     */
    private final Base base;

    /**
     * Make a new integer.
     *
     * @param v the current value
     * @param b the base used when printing the value
     */
    public BtrpNumber(int v, Base b) {
        this.base = b;
        dVal = v;
        this.isInteger = true;
    }

    /**
     * Make a float number.
     * The number will be represented in base 10.
     *
     * @param d the value.
     */
    public BtrpNumber(double d) {
        this.dVal = d;
        this.isInteger = false;
        this.base = Base.BASE_10;
    }

    /**
     * @return {@link org.btrplace.btrpsl.element.BtrpOperand.Type#NUMBER}
     */
    @Override
    public Type type() {
        return Type.NUMBER;
    }

    /**
     * Get the current base for the number
     *
     * @return the base
     */
    public Base getBase() {
        return this.base;
    }

    /**
     * Check the operand is a number. If not, throws a {@link UnsupportedOperationException} exception.
     *
     * @param e the operand to check
     */
    private void checkType(BtrpOperand e) {
        if (!(e instanceof BtrpNumber)) {
            throw new UnsupportedOperationException(e + " must be a '" + prettyType() + "' instead of a '" + e.prettyType() + "'");
        }
    }

    /**
     * @param nb the other value. Must be a number
     * @return an integer number if both operands are integer. A real number otherwise
     */
    @Override
    public BtrpNumber power(BtrpOperand nb) {
        checkType(nb);
        BtrpNumber x = (BtrpNumber) nb;
        if (x.dVal <= 0) {
            throw new UnsupportedOperationException(nb + " must be strictly positive");
        }
        double res = Math.pow(dVal, x.dVal);
        return isInteger && x.isInteger ? new BtrpNumber((int) res, base) : new BtrpNumber(res);
    }

    /**
     * @param other the other value. Must be a number
     */
    @Override
    public BtrpNumber plus(BtrpOperand other) {
        checkType(other);
        BtrpNumber x = (BtrpNumber) other;
        double res = dVal + x.dVal;
        return isInteger && x.isInteger ? new BtrpNumber((int) res, base) : new BtrpNumber(res);
    }

    /**
     * @param other the other value. Must be a number
     */
    @Override
    public BtrpNumber minus(BtrpOperand other) {
        checkType(other);
        BtrpNumber x = (BtrpNumber) other;
        double res = dVal - x.dVal;
        return isInteger && x.isInteger ? new BtrpNumber((int) res, base) : new BtrpNumber(res);
    }

    @Override
    public BtrpNumber negate() {
        return isInteger ? new BtrpNumber((int) -dVal, base) : new BtrpNumber(-dVal);
    }

    /**
     * @param other the other value. Must be a number
     */
    @Override
    public BtrpNumber times(BtrpOperand other) {
        checkType(other);
        BtrpNumber x = (BtrpNumber) other;
        double res = dVal * x.dVal;
        return isInteger && x.isInteger ? new BtrpNumber((int) res, base) : new BtrpNumber(res);
    }

    /**
     * @param other the other value. Must be a number
     */
    @Override
    public BtrpNumber div(BtrpOperand other) {
        checkType(other);
        BtrpNumber x = (BtrpNumber) other;
        if (Double.doubleToRawLongBits(x.dVal) == 0) {
            throw new ArithmeticException("Division by 0");
        }
        double res = dVal / x.dVal;
        return isInteger && x.isInteger ? new BtrpNumber((int) res, base) : new BtrpNumber(res);
    }

    /**
     * @param other the other value. Must be a number
     */
    @Override
    public BtrpNumber remainder(BtrpOperand other) {
        checkType(other);
        BtrpNumber x = (BtrpNumber) other;
        double res = dVal % x.dVal;
        return isInteger && x.isInteger ? new BtrpNumber((int) res, base) : new BtrpNumber(res);
    }

    /**
     * Check the equality of two elements.
     * Both are equals if they are an instance of a same class and if they contains
     * the same element.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BtrpNumber x = (BtrpNumber) o;
        return Double.doubleToRawLongBits(dVal) == Double.doubleToRawLongBits(x.dVal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dVal);
    }

    /**
     * Textual representation of the integer.
     * The base is considered.
     *
     * @return a String
     */
    @Override
    public String toString() {
        if (isInteger) {
            switch (base) {
                case BASE_8:
                    return Integer.toOctalString((int) dVal);
                case BASE_16:
                    return Integer.toHexString((int) dVal);
                default:
                    return Integer.toString((int) dVal);
            }
        }
        return Double.toString(dVal);
    }

    /**
     * @return {@code 0}
     */
    @Override
    public int degree() {
        return 0;
    }

    /**
     * The integer value of this operand.
     *
     * @return the integer given at instantiation if the number is an integer. {@code 0} otherwise
     */
    public int getIntValue() {
        return (int) dVal;
    }

    /**
     * The real value of this operand.
     *
     * @return the real given at instantiation if the number is a real. {@code 0} otherwise
     */
    public double getDoubleValue() {
        return dVal;
    }

    @Override
    public BtrpNumber eq(BtrpOperand other) {
        checkType(other);
        BtrpNumber x = (BtrpNumber) other;
        return Double.doubleToRawLongBits(dVal) == Double.doubleToRawLongBits(x.dVal) ? BtrpNumber.TRUE : BtrpNumber.FALSE;
    }

    @Override
    public BtrpNumber geq(BtrpOperand other) {
        checkType(other);
        BtrpNumber x = (BtrpNumber) other;
        return dVal >= x.dVal ? BtrpNumber.TRUE : BtrpNumber.FALSE;
    }

    @Override
    public BtrpNumber gt(BtrpOperand other) {
        checkType(other);
        BtrpNumber x = (BtrpNumber) other;
        return dVal > x.dVal ? BtrpNumber.TRUE : BtrpNumber.FALSE;
    }

    @Override
    public BtrpNumber not() {
        if (TRUE.equals(this)) {
            return FALSE;
        } else if (FALSE.equals(this)) {
            return TRUE;
        }
        throw new UnsupportedOperationException("Cannot negate a non-boolean");
    }

    @Override
    public BtrpNumber copy() {
        if (isInteger) {
            return new BtrpNumber((int) dVal, base);
        }
        return new BtrpNumber(dVal);
    }

    /**
     * Indicates whether or not the number is an integer.
     *
     * @return {@code true} if the number is an integer.
     */
    public boolean isInteger() {
        return isInteger;
    }

    /**
     * The number base.
     */
    public enum Base {
        /**
         * Octal value.
         */BASE_8,
        /**
         * Decimal value.
         */BASE_10,
        /**
         * Hexadecimal value.
         */BASE_16
    }
}
