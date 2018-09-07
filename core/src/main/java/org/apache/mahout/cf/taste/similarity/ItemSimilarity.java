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

package org.apache.mahout.cf.taste.similarity;

import org.apache.mahout.cf.taste.common.Refreshable;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.model.Item;

/**
 * <p>Implementations of this interface define a notion of similarity between two
 * {@link Item}s. Implementations should return values in the range -1.0 to 1.0, with
 * 1.0 representing perfect similarity.</p>
 *
 * @see UserSimilarity
 */
public interface ItemSimilarity extends Refreshable {

  /**
   * <p>Returns the degree of similarity, of two {@link Item}s, based
   * on the preferences that {@link org.apache.mahout.cf.taste.model.User}s have expressed for the items.</p>
   *
   * @param item1 first item
   * @param item2 second item
   * @return similarity between the {@link Item}s, in [-1,1]
   * @throws TasteException if an error occurs while accessing the data
   */
  double itemSimilarity(Item item1, Item item2) throws TasteException;

}
