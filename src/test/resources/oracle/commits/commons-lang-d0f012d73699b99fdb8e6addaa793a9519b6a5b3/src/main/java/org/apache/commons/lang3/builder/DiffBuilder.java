/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.lang3.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;

/**
 * Assists in implementing {@link Diffable#diff(Object)} methods.
 *
 * <p>
 * To use this class, write code as follows:
 * </p>
 *
 * <pre>
 * public class Person implements Diffable&lt;Person&gt; {
 *   String name;
 *   int age;
 *   boolean smoker;
 *
 *   ...
 *
 *   public DiffResult diff(Person obj) {
 *     // No need for null check, as NullPointerException correct if obj is null
 *     return new DiffBuilder(this, obj, ToStringStyle.SHORT_PREFIX_STYLE)
 *       .append("name", this.name, obj.name)
 *       .append("age", this.age, obj.age)
 *       .append("smoker", this.smoker, obj.smoker)
 *       .build();
 *   }
 * }
 * </pre>
 *
 * <p>
 * The {@link ToStringStyle} passed to the constructor is embedded in the
 * returned {@link DiffResult} and influences the style of the
 * {@code DiffResult.toString()} method. This style choice can be overridden by
 * calling {@link DiffResult#toString(ToStringStyle)}.
 * </p>
 * <p>
 * See {@link ReflectionDiffBuilder} for a reflection based version of this class.
 * </p>
 *
 * @param <T> type of the left and right object.
 * @see Diffable
 * @see Diff
 * @see DiffResult
 * @see ToStringStyle
 * @see ReflectionDiffBuilder
 * @since 3.3
 */
public class DiffBuilder<T> implements Builder<DiffResult<T>> {

    private static final class SDiff<T> extends Diff<T> {

        private static final long serialVersionUID = 1L;
        private final transient Supplier<T> leftS;
        private final transient Supplier<T> rightS;

        private SDiff(final String fieldName, final Supplier<T> leftS, final Supplier<T> rightS, final Class<T> type) {
            super(fieldName, type);
            this.leftS = leftS;
            this.rightS = rightS;
        }

        @Override
        public T getLeft() {
            return leftS.get();
        }

        @Override
        public T getRight() {
            return rightS.get();
        }

    }
    static final String TO_STRING_FORMAT = "%s differs from %s";

    private final List<Diff<?>> diffs;
    private final boolean equals;
    private final T left;
    private final T right;
    private final ToStringStyle style;
    private final String toStringFormat;

    /**
     * Constructs a builder for the specified objects with the specified style.
     *
     * <p>
     * If {@code lhs == rhs} or {@code lhs.equals(rhs)} then the builder will
     * not evaluate any calls to {@code append(...)} and will return an empty
     * {@link DiffResult} when {@link #build()} is executed.
     * </p>
     *
     * <p>
     * This delegates to {@link #DiffBuilder(Object, Object, ToStringStyle, boolean)}
     * with the testTriviallyEqual flag enabled.
     * </p>
     *
     * @param lhs
     *            {@code this} object
     * @param rhs
     *            the object to diff against
     * @param style
     *            the style will use when outputting the objects, {@code null}
     *            uses the default
     * @throws NullPointerException
     *             if {@code lhs} or {@code rhs} is {@code null}
     */
    public DiffBuilder(final T lhs, final T rhs, final ToStringStyle style) {
        this(lhs, rhs, style, true);
    }

    /**
     * Constructs a builder for the specified objects with the specified style.
     *
     * <p>
     * If {@code lhs == rhs} or {@code lhs.equals(rhs)} then the builder will
     * not evaluate any calls to {@code append(...)} and will return an empty
     * {@link DiffResult} when {@link #build()} is executed.
     * </p>
     *
     * @param lhs
     *            {@code this} object
     * @param rhs
     *            the object to diff against
     * @param style
     *            the style will use when outputting the objects, {@code null}
     *            uses the default
     * @param testObjectsEquals
     *            If true, this will test if lhs and rhs are the same or equal.
     *            All of the append(fieldName, lhs, rhs) methods will abort
     *            without creating a field {@link Diff} if the trivially equal
     *            test is enabled and returns true.  The result of this test
     *            is never changed throughout the life of this {@link DiffBuilder}.
     * @throws NullPointerException
     *             if {@code lhs} or {@code rhs} is {@code null}
     * @since 3.4
     */
    public DiffBuilder(final T lhs, final T rhs, final ToStringStyle style, final boolean testObjectsEquals) {
        this.left = Objects.requireNonNull(lhs, "lhs");
        this.right = Objects.requireNonNull(rhs, "rhs");
        this.diffs = new ArrayList<>();
        this.toStringFormat = DiffBuilder.TO_STRING_FORMAT;
        this.style = style != null ? style : ToStringStyle.DEFAULT_STYLE;
        // Don't compare any fields if objects equal
        this.equals = testObjectsEquals && Objects.equals(lhs, rhs);
    }

    private <F> DiffBuilder<T> add(final String fieldName, final Supplier<F> left, final Supplier<F> right, final Class<F> type) {
        diffs.add(new SDiff<>(Objects.requireNonNull(fieldName, "fieldName"), left, right, type));
        return this;
    }

    /**
     * Test if two {@code boolean}s are equal.
     *
     * @param fieldName
     *            the field name
     * @param lhs
     *            the left-hand side {@code boolean}
     * @param rhs
     *            the right-hand side {@code boolean}
     * @return this
     * @throws NullPointerException
     *             if field name is {@code null}
     */
    public DiffBuilder<T> append(final String fieldName, final boolean lhs, final boolean rhs) {
        return equals || lhs == rhs ? this : add(fieldName, () -> Boolean.valueOf(lhs), () -> Boolean.valueOf(rhs), Boolean.class);
    }

    /**
     * Test if two {@code boolean[]}s are equal.
     *
     * @param fieldName
     *            the field name
     * @param lhs
     *            the left-hand side {@code boolean[]}
     * @param rhs
     *            the right-hand side {@code boolean[]}
     * @return this
     * @throws NullPointerException
     *             if field name is {@code null}
     */
    public DiffBuilder<T> append(final String fieldName, final boolean[] lhs, final boolean[] rhs) {
        return equals || Arrays.equals(lhs, rhs) ? this : add(fieldName, () -> ArrayUtils.toObject(lhs), () -> ArrayUtils.toObject(rhs), Boolean[].class);
    }

    /**
     * Test if two {@code byte}s are equal.
     *
     * @param fieldName
     *            the field name
     * @param lhs
     *            the left-hand side {@code byte}
     * @param rhs
     *            the right-hand side {@code byte}
     * @return this
     * @throws NullPointerException
     *             if field name is {@code null}
     */
    public DiffBuilder<T> append(final String fieldName, final byte lhs, final byte rhs) {
        return equals || lhs == rhs ? this : add(fieldName, () -> Byte.valueOf(lhs), () -> Byte.valueOf(rhs), Byte.class);
    }

    /**
     * Test if two {@code byte[]}s are equal.
     *
     * @param fieldName
     *            the field name
     * @param lhs
     *            the left-hand side {@code byte[]}
     * @param rhs
     *            the right-hand side {@code byte[]}
     * @return this
     * @throws NullPointerException
     *             if field name is {@code null}
     */
    public DiffBuilder<T> append(final String fieldName, final byte[] lhs, final byte[] rhs) {
        return equals || Arrays.equals(lhs, rhs) ? this : add(fieldName, () -> ArrayUtils.toObject(lhs), () -> ArrayUtils.toObject(rhs), Byte[].class);
    }

    /**
     * Test if two {@code char}s are equal.
     *
     * @param fieldName
     *            the field name
     * @param lhs
     *            the left-hand side {@code char}
     * @param rhs
     *            the right-hand side {@code char}
     * @return this
     * @throws NullPointerException
     *             if field name is {@code null}
     */
    public DiffBuilder<T> append(final String fieldName, final char lhs, final char rhs) {
        return equals || lhs == rhs ? this : add(fieldName, () -> Character.valueOf(lhs), () -> Character.valueOf(rhs), Character.class);
    }

    /**
     * Test if two {@code char[]}s are equal.
     *
     * @param fieldName
     *            the field name
     * @param lhs
     *            the left-hand side {@code char[]}
     * @param rhs
     *            the right-hand side {@code char[]}
     * @return this
     * @throws NullPointerException
     *             if field name is {@code null}
     */
    public DiffBuilder<T> append(final String fieldName, final char[] lhs, final char[] rhs) {
        return equals || Arrays.equals(lhs, rhs) ? this : add(fieldName, () -> ArrayUtils.toObject(lhs), () -> ArrayUtils.toObject(rhs), Character[].class);
    }

    /**
     * Append diffs from another {@link DiffResult}.
     *
     * <p>
     * Useful this method to compare properties which are
     * themselves Diffable and would like to know which specific part of
     * it is different.
     * </p>
     *
     * <pre>
     * public class Person implements Diffable&lt;Person&gt; {
     *   String name;
     *   Address address; // implements Diffable&lt;Address&gt;
     *
     *   ...
     *
     *   public DiffResult diff(Person obj) {
     *     return new DiffBuilder(this, obj, ToStringStyle.SHORT_PREFIX_STYLE)
     *       .append("name", this.name, obj.name)
     *       .append("address", this.address.diff(obj.address))
     *       .build();
     *   }
     * }
     * </pre>
     *
     * @param fieldName
     *            the field name
     * @param diffResult
     *            the {@link DiffResult} to append
     * @return this
     * @throws NullPointerException if field name is {@code null} or diffResult is {@code null}
     * @since 3.5
     */
    public DiffBuilder<T> append(final String fieldName, final DiffResult<T> diffResult) {
        Objects.requireNonNull(diffResult, "diffResult");
        if (equals) {
            return this;
        }
        diffResult.getDiffs().forEach(diff -> append(fieldName + "." + diff.getFieldName(), diff.getLeft(), diff.getRight()));
        return this;
    }

    /**
     * Test if two {@code double}s are equal.
     *
     * @param fieldName
     *            the field name
     * @param lhs
     *            the left-hand side {@code double}
     * @param rhs
     *            the right-hand side {@code double}
     * @return this
     * @throws NullPointerException
     *             if field name is {@code null}
     */
    public DiffBuilder<T> append(final String fieldName, final double lhs, final double rhs) {
        return equals || Double.doubleToLongBits(lhs) == Double.doubleToLongBits(rhs) ? this
                : add(fieldName, () -> Double.valueOf(lhs), () -> Double.valueOf(rhs), Double.class);
    }

    /**
     * Test if two {@code double[]}s are equal.
     *
     * @param fieldName
     *            the field name
     * @param lhs
     *            the left-hand side {@code double[]}
     * @param rhs
     *            the right-hand side {@code double[]}
     * @return this
     * @throws NullPointerException
     *             if field name is {@code null}
     */
    public DiffBuilder<T> append(final String fieldName, final double[] lhs, final double[] rhs) {
        return equals || Arrays.equals(lhs, rhs) ? this : add(fieldName, () -> ArrayUtils.toObject(lhs), () -> ArrayUtils.toObject(rhs), Double[].class);
    }

    /**
     * Test if two {@code float}s are equal.
     *
     * @param fieldName
     *            the field name
     * @param lhs
     *            the left-hand side {@code float}
     * @param rhs
     *            the right-hand side {@code float}
     * @return this
     * @throws NullPointerException
     *             if field name is {@code null}
     */
    public DiffBuilder<T> append(final String fieldName, final float lhs, final float rhs) {
        return equals || Float.floatToIntBits(lhs) == Float.floatToIntBits(rhs) ? this
                : add(fieldName, () -> Float.valueOf(lhs), () -> Float.valueOf(rhs), Float.class);
    }

    /**
     * Test if two {@code float[]}s are equal.
     *
     * @param fieldName
     *            the field name
     * @param lhs
     *            the left-hand side {@code float[]}
     * @param rhs
     *            the right-hand side {@code float[]}
     * @return this
     * @throws NullPointerException
     *             if field name is {@code null}
     */
    public DiffBuilder<T> append(final String fieldName, final float[] lhs, final float[] rhs) {
        return equals || Arrays.equals(lhs, rhs) ? this : add(fieldName, () -> ArrayUtils.toObject(lhs), () -> ArrayUtils.toObject(rhs), Float[].class);
    }

    /**
     * Test if two {@code int}s are equal.
     *
     * @param fieldName
     *            the field name
     * @param lhs
     *            the left-hand side {@code int}
     * @param rhs
     *            the right-hand side {@code int}
     * @return this
     * @throws NullPointerException
     *             if field name is {@code null}
     */
    public DiffBuilder<T> append(final String fieldName, final int lhs, final int rhs) {
        return equals || lhs == rhs ? this : add(fieldName, () -> Integer.valueOf(lhs), () -> Integer.valueOf(rhs), Integer.class);
    }

    /**
     * Test if two {@code int[]}s are equal.
     *
     * @param fieldName
     *            the field name
     * @param lhs
     *            the left-hand side {@code int[]}
     * @param rhs
     *            the right-hand side {@code int[]}
     * @return this
     * @throws NullPointerException
     *             if field name is {@code null}
     */
    public DiffBuilder<T> append(final String fieldName, final int[] lhs, final int[] rhs) {
        return equals || Arrays.equals(lhs, rhs) ? this : add(fieldName, () -> ArrayUtils.toObject(lhs), () -> ArrayUtils.toObject(rhs), Integer[].class);
    }

    /**
     * Test if two {@code long}s are equal.
     *
     * @param fieldName
     *            the field name
     * @param lhs
     *            the left-hand side {@code long}
     * @param rhs
     *            the right-hand side {@code long}
     * @return this
     * @throws NullPointerException
     *             if field name is {@code null}
     */
    public DiffBuilder<T> append(final String fieldName, final long lhs, final long rhs) {
        return equals || lhs == rhs ? this : add(fieldName, () -> Long.valueOf(lhs), () -> Long.valueOf(rhs), Long.class);
    }

    /**
     * Test if two {@code long[]}s are equal.
     *
     * @param fieldName
     *            the field name
     * @param lhs
     *            the left-hand side {@code long[]}
     * @param rhs
     *            the right-hand side {@code long[]}
     * @return this
     * @throws NullPointerException
     *             if field name is {@code null}
     */
    public DiffBuilder<T> append(final String fieldName, final long[] lhs, final long[] rhs) {
        return equals || Arrays.equals(lhs, rhs) ? this : add(fieldName, () -> ArrayUtils.toObject(lhs), () -> ArrayUtils.toObject(rhs), Long[].class);
    }

    /**
     * Test if two {@link Objects}s are equal.
     *
     * @param fieldName
     *            the field name
     * @param lhs
     *            the left-hand side {@link Object}
     * @param rhs
     *            the right-hand side {@link Object}
     * @return this
     * @throws NullPointerException
     *             if field name is {@code null}
     */
    public DiffBuilder<T> append(final String fieldName, final Object lhs, final Object rhs) {
        if (equals || lhs == rhs) {
            return this;
        }
        // rhs cannot be null, as lhs != rhs
        final Object objectToTest = lhs != null ? lhs : rhs;
        if (ObjectUtils.isArray(objectToTest)) {
            if (objectToTest instanceof boolean[]) {
                return append(fieldName, (boolean[]) lhs, (boolean[]) rhs);
            }
            if (objectToTest instanceof byte[]) {
                return append(fieldName, (byte[]) lhs, (byte[]) rhs);
            }
            if (objectToTest instanceof char[]) {
                return append(fieldName, (char[]) lhs, (char[]) rhs);
            }
            if (objectToTest instanceof double[]) {
                return append(fieldName, (double[]) lhs, (double[]) rhs);
            }
            if (objectToTest instanceof float[]) {
                return append(fieldName, (float[]) lhs, (float[]) rhs);
            }
            if (objectToTest instanceof int[]) {
                return append(fieldName, (int[]) lhs, (int[]) rhs);
            }
            if (objectToTest instanceof long[]) {
                return append(fieldName, (long[]) lhs, (long[]) rhs);
            }
            if (objectToTest instanceof short[]) {
                return append(fieldName, (short[]) lhs, (short[]) rhs);
            }
            return append(fieldName, (Object[]) lhs, (Object[]) rhs);
        }
        // Not array type
        if (Objects.equals(lhs, rhs)) {
            return this;
        }
        add(fieldName, () -> lhs, () -> rhs, Object.class);
        return this;
    }

    /**
     * Test if two {@code Object[]}s are equal.
     *
     * @param fieldName
     *            the field name
     * @param lhs
     *            the left-hand side {@code Object[]}
     * @param rhs
     *            the right-hand side {@code Object[]}
     * @return this
     * @throws NullPointerException
     *             if field name is {@code null}
     */
    public DiffBuilder<T> append(final String fieldName, final Object[] lhs, final Object[] rhs) {
        return equals || Arrays.equals(lhs, rhs) ? this : add(fieldName, () -> lhs, () -> rhs, Object[].class);
    }

    /**
     * Test if two {@code short}s are equal.
     *
     * @param fieldName
     *            the field name
     * @param lhs
     *            the left-hand side {@code short}
     * @param rhs
     *            the right-hand side {@code short}
     * @return this
     * @throws NullPointerException
     *             if field name is {@code null}
     */
    public DiffBuilder<T> append(final String fieldName, final short lhs, final short rhs) {
        return equals || lhs == rhs ? this : add(fieldName, () -> Short.valueOf(lhs), () -> Short.valueOf(rhs), Short.class);
    }

    /**
     * Test if two {@code short[]}s are equal.
     *
     * @param fieldName
     *            the field name
     * @param lhs
     *            the left-hand side {@code short[]}
     * @param rhs
     *            the right-hand side {@code short[]}
     * @return this
     * @throws NullPointerException
     *             if field name is {@code null}
     */
    public DiffBuilder<T> append(final String fieldName, final short[] lhs, final short[] rhs) {
        return equals || Arrays.equals(lhs, rhs) ? this : add(fieldName, () -> ArrayUtils.toObject(lhs), () -> ArrayUtils.toObject(rhs), Short[].class);
    }

    /**
     * Builds a {@link DiffResult} based on the differences appended to this
     * builder.
     *
     * @return a {@link DiffResult} containing the differences between the two
     *         objects.
     */
    @Override
    public DiffResult<T> build() {
        return new DiffResult<>(left, right, diffs, style, toStringFormat);
    }

}
