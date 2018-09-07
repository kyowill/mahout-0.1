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

package org.apache.mahout.cf.taste.neighborhood;

import org.apache.mahout.cf.taste.common.Refreshable;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.model.User;

import java.util.Collection;

/**
 * <p>Implementations of this interface compute a "neighborhood" of {@link User}s like a
 * given {@link User}. This neighborhood can be used to compute recommendations then.</p>
 */
public interface UserNeighborhood extends Refreshable {

  /**
   * @param userID ID of user for which a neighborhood will be computed
   * @return {@link Collection} of {@link User}s in the neighborhood
   * @throws org.apache.mahout.cf.taste.common.TasteException if an error occurs while accessing data
   */
  Collection<User> getUserNeighborhood(Object userID) throws TasteException;

}
