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

package org.apache.mahout.cf.taste.impl.similarity;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.common.Weighting;
import org.apache.mahout.cf.taste.similarity.ItemSimilarity;
import org.apache.mahout.cf.taste.impl.model.GenericItem;
import org.apache.mahout.cf.taste.impl.model.GenericPreference;
import org.apache.mahout.cf.taste.impl.model.GenericUser;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.cf.taste.model.User;

import java.util.Collections;

/**
 * <p>Tests {@link EuclideanDistanceSimilarity}.</p>
 */
public final class EuclideanDistanceSimilarityTest extends SimilarityTestCase {

  public void testFullCorrelation1() throws Exception {
    User user1 = getUser("test1", 3.0, -2.0);
    User user2 = getUser("test2", 3.0, -2.0);
    DataModel dataModel = getDataModel(user1, user2);
    double correlation = new EuclideanDistanceSimilarity(dataModel).userSimilarity(user1, user2);
    assertCorrelationEquals(1.0, correlation);
  }

  public void testFullCorrelation1Weighted() throws Exception {
    User user1 = getUser("test1", 3.0, -2.0);
    User user2 = getUser("test2", 3.0, -2.0);
    DataModel dataModel = getDataModel(user1, user2);
    double correlation = new EuclideanDistanceSimilarity(dataModel, Weighting.WEIGHTED).userSimilarity(user1, user2);
    assertCorrelationEquals(1.0, correlation);
  }

  public void testFullCorrelation2() throws Exception {
    User user1 = getUser("test1", 3.0, 3.0);
    User user2 = getUser("test2", 3.0, 3.0);
    DataModel dataModel = getDataModel(user1, user2);
    double correlation = new EuclideanDistanceSimilarity(dataModel).userSimilarity(user1, user2);
    assertTrue(Double.isNaN(correlation));
  }

  public void testNoCorrelation1() throws Exception {
    User user1 = getUser("test1", 3.0, -2.0);
    User user2 = getUser("test2", -3.0, 2.0);
    DataModel dataModel = getDataModel(user1, user2);
    double correlation = new EuclideanDistanceSimilarity(dataModel).userSimilarity(user1, user2);
    assertCorrelationEquals(0.424465381883345, correlation);
  }

  public void testNoCorrelation1Weighted() throws Exception {
    User user1 = getUser("test1", 3.0, -2.0);
    User user2 = getUser("test2", -3.0, 2.0);
    DataModel dataModel = getDataModel(user1, user2);
    double correlation = new EuclideanDistanceSimilarity(dataModel, Weighting.WEIGHTED).userSimilarity(user1, user2);
    assertCorrelationEquals(0.8081551272944483, correlation);
  }

  public void testNoCorrelation2() throws Exception {
    Preference pref1 = new GenericPreference(null, new GenericItem<String>("1"), 1.0);
    GenericUser<String> user1 = new GenericUser<String>("test1", Collections.singletonList(pref1));
    Preference pref2 = new GenericPreference(null, new GenericItem<String>("2"), 1.0);
    GenericUser<String> user2 = new GenericUser<String>("test2", Collections.singletonList(pref2));
    DataModel dataModel = getDataModel(user1, user2);
    double correlation = new EuclideanDistanceSimilarity(dataModel).userSimilarity(user1, user2);
    assertTrue(Double.isNaN(correlation));
  }

  public void testNoCorrelation3() throws Exception {
    User user1 = getUser("test1", 90.0, 80.0, 70.0);
    User user2 = getUser("test2", 70.0, 80.0, 90.0);
    DataModel dataModel = getDataModel(user1, user2);
    double correlation = new EuclideanDistanceSimilarity(dataModel).userSimilarity(user1, user2);
    assertCorrelationEquals(0.3606507916004517, correlation);
  }

  public void testSimple() throws Exception {
    User user1 = getUser("test1", 1.0, 2.0, 3.0);
    User user2 = getUser("test2", 2.0, 5.0, 6.0);
    DataModel dataModel = getDataModel(user1, user2);
    double correlation = new EuclideanDistanceSimilarity(dataModel).userSimilarity(user1, user2);
    assertCorrelationEquals(0.5896248568217328, correlation);
  }

  public void testSimpleWeighted() throws Exception {
    User user1 = getUser("test1", 1.0, 2.0, 3.0);
    User user2 = getUser("test2", 2.0, 5.0, 6.0);
    DataModel dataModel = getDataModel(user1, user2);
    double correlation = new EuclideanDistanceSimilarity(dataModel, Weighting.WEIGHTED).userSimilarity(user1, user2);
    assertCorrelationEquals(0.8974062142054332, correlation);
  }

  public void testFullItemCorrelation1() throws Exception {
    User user1 = getUser("test1", 3.0, 3.0);
    User user2 = getUser("test2", -2.0, -2.0);
    DataModel dataModel = getDataModel(user1, user2);
    double correlation =
            new EuclideanDistanceSimilarity(dataModel).itemSimilarity(dataModel.getItem("0"), dataModel.getItem("1"));
    assertCorrelationEquals(1.0, correlation);
  }

  public void testFullItemCorrelation2() throws Exception {
    User user1 = getUser("test1", 3.0, 3.0);
    User user2 = getUser("test2", 3.0, 3.0);
    DataModel dataModel = getDataModel(user1, user2);
    double correlation =
            new EuclideanDistanceSimilarity(dataModel).itemSimilarity(dataModel.getItem("0"), dataModel.getItem("1"));
    // Yeah, undefined in this case
    assertTrue(Double.isNaN(correlation));
  }

  public void testNoItemCorrelation1() throws Exception {
    User user1 = getUser("test1", 3.0, -3.0);
    User user2 = getUser("test2", -2.0, 2.0);
    DataModel dataModel = getDataModel(user1, user2);
    double correlation =
            new EuclideanDistanceSimilarity(dataModel).itemSimilarity(dataModel.getItem("0"), dataModel.getItem("1"));
    assertCorrelationEquals(0.424465381883345, correlation);
  }

  public void testNoItemCorrelation2() throws Exception {
    Preference pref1 = new GenericPreference(null, new GenericItem<String>("1"), 1.0);
    GenericUser<String> user1 = new GenericUser<String>("test1", Collections.singletonList(pref1));
    Preference pref2 = new GenericPreference(null, new GenericItem<String>("2"), 1.0);
    GenericUser<String> user2 = new GenericUser<String>("test2", Collections.singletonList(pref2));
    DataModel dataModel = getDataModel(user1, user2);
    double correlation =
            new EuclideanDistanceSimilarity(dataModel).itemSimilarity(dataModel.getItem("1"), dataModel.getItem("2"));
    assertTrue(Double.isNaN(correlation));
  }

  public void testNoItemCorrelation3() throws Exception {
    User user1 = getUser("test1", 90.0, 70.0);
    User user2 = getUser("test2", 80.0, 80.0);
    User user3 = getUser("test3", 70.0, 90.0);
    DataModel dataModel = getDataModel(user1, user2, user3);
    double correlation =
            new EuclideanDistanceSimilarity(dataModel).itemSimilarity(dataModel.getItem("0"), dataModel.getItem("1"));
    assertCorrelationEquals(0.3606507916004517, correlation);
  }

  public void testSimpleItem() throws Exception {
    User user1 = getUser("test1", 1.0, 2.0);
    User user2 = getUser("test2", 2.0, 5.0);
    User user3 = getUser("test3", 3.0, 6.0);
    DataModel dataModel = getDataModel(user1, user2, user3);
    double correlation =
            new EuclideanDistanceSimilarity(dataModel).itemSimilarity(dataModel.getItem("0"), dataModel.getItem("1"));
    assertCorrelationEquals(0.5896248568217328, correlation);
  }

  public void testSimpleItemWeighted() throws Exception {
    User user1 = getUser("test1", 1.0, 2.0);
    User user2 = getUser("test2", 2.0, 5.0);
    User user3 = getUser("test3", 3.0, 6.0);
    DataModel dataModel = getDataModel(user1, user2, user3);
    ItemSimilarity itemSimilarity = new EuclideanDistanceSimilarity(dataModel, Weighting.WEIGHTED);
    double correlation = itemSimilarity.itemSimilarity(dataModel.getItem("0"), dataModel.getItem("1"));
    assertCorrelationEquals(0.8974062142054332, correlation);
  }

  public void testRefresh() throws TasteException {
    // Make sure this doesn't throw an exception
    new EuclideanDistanceSimilarity(getDataModel()).refresh(null);
  }

}