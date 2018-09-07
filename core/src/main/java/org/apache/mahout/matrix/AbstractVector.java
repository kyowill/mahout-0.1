/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.mahout.matrix;

import org.apache.hadoop.io.WritableComparable;

/**
 * Implementations of generic capabilities like sum of elements and dot products
 */
public abstract class AbstractVector implements Vector {

  /**
   * Subclasses must override to return an appropriately sparse or dense result
   * 
   * @param rows
   *            the row cardinality
   * @param columns
   *            the column cardinality
   * @return a Matrix
   */
  protected abstract Matrix matrixLike(int rows, int columns);

  /**
   * Returns an iterator for traversing the Vector, but not in any particular
   * order. The actual implementations may make some guarantees about the order
   * in which the vector is traversed. Otherwise, the traversal order is
   * undefined.
   * 
   * @see java.lang.Iterable#iterator()
   */
  @Override
  public abstract java.util.Iterator<Vector.Element> iterator();

  @Override
  public Vector.Element getElement(int index) {
    return new Element(index);
  }

  public class Element implements Vector.Element {
    private final int ind;

    public Element(int ind) {
      this.ind = ind;
    }

    @Override
    public double get() {
      return getQuick(ind);
    }

    @Override
    public int index() {
      return ind;
    }

    @Override
    public void set(double value) {
      setQuick(ind, value);
    }
  }

  @Override
  public Vector divide(double x) {
    Vector result = copy();
    for (int i = 0; i < result.cardinality(); i++)
      result.setQuick(i, getQuick(i) / x);
    return result;
  }

  @Override
  public double dot(Vector x) {
    if (cardinality() != x.cardinality())
      throw new CardinalityException();
    double result = 0;
    for (int i = 0; i < cardinality(); i++)
      result += getQuick(i) * x.getQuick(i);
    return result;
  }

  @Override
  public double get(int index) {
    if (index >= 0 && index < cardinality())
      return getQuick(index);
    else
      throw new IndexException();
  }

  @Override
  public Vector minus(Vector x) {
    if (cardinality() != x.cardinality())
      throw new CardinalityException();
    Vector result = copy();
    for (int i = 0; i < result.cardinality(); i++)
      result.setQuick(i, getQuick(i) - x.getQuick(i));
    return result;
  }

  @Override
  public Vector normalize() {
    double divSq = Math.sqrt(dot(this));
    return divide(divSq);
  }

  @Override
  public Vector plus(double x) {
    Vector result = copy();
    for (int i = 0; i < result.cardinality(); i++)
      result.setQuick(i, getQuick(i) + x);
    return result;
  }

  @Override
  public Vector plus(Vector x) {
    if (cardinality() != x.cardinality())
      throw new CardinalityException();
    Vector result = copy();
    for (int i = 0; i < result.cardinality(); i++)
      result.setQuick(i, getQuick(i) + x.getQuick(i));
    return result;
  }

  @Override
  public void set(int index, double value) {
    if (index >= 0 && index < cardinality())
      setQuick(index, value);
    else
      throw new IndexException();
  }

  @Override
  public Vector times(double x) {
    Vector result = copy();
    for (int i = 0; i < result.cardinality(); i++)
      result.setQuick(i, getQuick(i) * x);
    return result;
  }

  @Override
  public Vector times(Vector x) {
    if (cardinality() != x.cardinality())
      throw new CardinalityException();
    Vector result = copy();
    for (int i = 0; i < result.cardinality(); i++)
      result.setQuick(i, getQuick(i) * x.getQuick(i));
    return result;
  }

  @Override
  public double zSum() {
    double result = 0;
    for (int i = 0; i < cardinality(); i++)
      result += getQuick(i);
    return result;
  }

  @Override
  public Vector assign(double value) {
    for (int i = 0; i < cardinality(); i++)
      setQuick(i, value);
    return this;
  }

  @Override
  public Vector assign(double[] values) {
    if (values.length != cardinality())
      throw new CardinalityException();
    for (int i = 0; i < cardinality(); i++)
      setQuick(i, values[i]);
    return this;
  }

  @Override
  public Vector assign(Vector other) {
    if (other.cardinality() != cardinality())
      throw new CardinalityException();
    for (int i = 0; i < cardinality(); i++)
      setQuick(i, other.getQuick(i));
    return this;
  }

  @Override
  public Vector assign(BinaryFunction f, double y) {
    for (int i = 0; i < cardinality(); i++) {
      setQuick(i, f.apply(getQuick(i), y));
    }
    return this;
  }

  @Override
  public Vector assign(UnaryFunction function) {
    for (int i = 0; i < cardinality(); i++)
      setQuick(i, function.apply(getQuick(i)));
    return this;
  }

  @Override
  public Vector assign(Vector other, BinaryFunction function) {
    if (other.cardinality() != cardinality())
      throw new CardinalityException();
    for (int i = 0; i < cardinality(); i++)
      setQuick(i, function.apply(getQuick(i), other.getQuick(i)));
    return this;
  }

  @Override
  public Matrix cross(Vector other) {
    Matrix result = matrixLike(cardinality(), other.cardinality());
    for (int row = 0; row < cardinality(); row++)
      result.assignRow(row, other.times(getQuick(row)));
    return result;
  }

  /**
   * Decodes a point from its WritableComparable<?> representation.
   * 
   * @param writableComparable
   *            a WritableComparable<?> produced by asWritableComparable. Note the
   *            payload remainder: it is optional, but can be present.
   * @return the n-dimensional point
   */
  public static Vector decodeVector(WritableComparable<?> writableComparable) {
    return decodeVector(writableComparable.toString());
  }

  /**
   * Decodes a point from its string representation.
   * 
   * @param formattedString
   *            a formatted String produced by asFormatString. Note the payload
   *            remainder: it is optional, but can be present.
   * @return the n-dimensional point
   */
  public static Vector decodeVector(String formattedString) {
    Vector result;
    if (formattedString.trim().startsWith("[s"))
      result = SparseVector.decodeFormat(formattedString);
    else
      result = DenseVector.decodeFormat(formattedString);
    return result;
  }

}
