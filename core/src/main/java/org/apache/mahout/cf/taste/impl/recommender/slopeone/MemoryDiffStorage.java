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

package org.apache.mahout.cf.taste.impl.recommender.slopeone;

import org.apache.mahout.cf.taste.common.Refreshable;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.common.Weighting;
import org.apache.mahout.cf.taste.impl.common.CompactRunningAverage;
import org.apache.mahout.cf.taste.impl.common.CompactRunningAverageAndStdDev;
import org.apache.mahout.cf.taste.impl.common.FastMap;
import org.apache.mahout.cf.taste.impl.common.FullRunningAverage;
import org.apache.mahout.cf.taste.impl.common.FullRunningAverageAndStdDev;
import org.apache.mahout.cf.taste.impl.common.RefreshHelper;
import org.apache.mahout.cf.taste.impl.common.RunningAverage;
import org.apache.mahout.cf.taste.impl.common.RunningAverageAndStdDev;
import org.apache.mahout.cf.taste.impl.common.FastSet;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.Item;
import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.cf.taste.model.User;
import org.apache.mahout.cf.taste.recommender.slopeone.DiffStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>An implementation of {@link DiffStorage} that merely stores item-item diffs in memory.
 * It is fast, but can consume a great deal of memory.</p>
 */
public final class MemoryDiffStorage implements DiffStorage {

  private static final Logger log = LoggerFactory.getLogger(MemoryDiffStorage.class);

  private final DataModel dataModel;
  private final boolean stdDevWeighted;
  private final boolean compactAverages;
  private final long maxEntries;
  private final FastMap<Object, FastMap<Object, RunningAverage>> averageDiffs;
  private final Map<Object, RunningAverage> averageItemPref;
  private final FastSet<Item> allRecommendableItemIDs;
  private final ReadWriteLock buildAverageDiffsLock;
  private final RefreshHelper refreshHelper;

  /**
   * <p>Creates a new {@link MemoryDiffStorage}.</p>
   *
   * <p>See {@link org.apache.mahout.cf.taste.impl.recommender.slopeone.SlopeOneRecommender} for the
   * meaning of <code>stdDevWeighted</code>. If <code>compactAverages</code>
   * is set, this uses alternate data structures ({@link CompactRunningAverage} versus
   * {@link FullRunningAverage}) that use almost 50% less memory but store item-item
   * averages less accurately. <code>maxEntries</code> controls the maximum number of item-item average
   * preference differences that will be tracked internally. After the limit is reached,
   * if a new item-item pair is observed in the data it will be ignored. This is recommended for large datasets.
   * The first <code>maxEntries</code>
   * item-item pairs observed in the data are tracked. Assuming that item ratings are reasonably distributed
   * among users, this should only ignore item-item pairs that are very infrequently co-rated by a user.
   * The intuition is that data on these infrequently co-rated item-item pairs is less reliable and should
   * be the first that is ignored. This parameter can be used to limit the memory requirements of
   * {@link SlopeOneRecommender}, which otherwise grow as the square
   * of the number of items that exist in the {@link DataModel}. Memory requirements can reach gigabytes
   * with only about 10000 items, so this may be necessary on larger datasets.
   *
   * @param dataModel
   * @param stdDevWeighted see {@link org.apache.mahout.cf.taste.impl.recommender.slopeone.SlopeOneRecommender}
   * @param compactAverages if <code>true</code>,
   * use {@link CompactRunningAverage} instead of {@link FullRunningAverage} internally
   * @param maxEntries maximum number of item-item average preference differences to track internally
   * @throws IllegalArgumentException if <code>maxEntries</code> is not positive or <code>dataModel</code>
   * is null
   */
  public MemoryDiffStorage(DataModel dataModel,
                           Weighting stdDevWeighted,
                           boolean compactAverages,
                           long maxEntries) throws TasteException {
    if (dataModel == null) {
      throw new IllegalArgumentException("dataModel is null");
    }
    if (maxEntries <= 0L) {
      throw new IllegalArgumentException("maxEntries must be positive");
    }
    this.dataModel = dataModel;
    this.stdDevWeighted = stdDevWeighted == Weighting.WEIGHTED;
    this.compactAverages = compactAverages;
    this.maxEntries = maxEntries;
    this.averageDiffs = new FastMap<Object, FastMap<Object, RunningAverage>>();
    this.averageItemPref = new FastMap<Object, RunningAverage>();
    this.buildAverageDiffsLock = new ReentrantReadWriteLock();
    this.allRecommendableItemIDs = new FastSet<Item>(dataModel.getNumItems());
    this.refreshHelper = new RefreshHelper(new Callable<Object>() {
      @Override
      public Object call() throws TasteException {
        buildAverageDiffs();
        return null;
      }
    });
    refreshHelper.addDependency(dataModel);
    buildAverageDiffs();
  }

  @Override
  public RunningAverage getDiff(Object itemID1, Object itemID2) {
    Map<Object, RunningAverage> level2Map = averageDiffs.get(itemID1);
    RunningAverage average = null;
    if (level2Map != null) {
      average = level2Map.get(itemID2);
    }
    boolean inverted = false;
    if (average == null) {
      level2Map = averageDiffs.get(itemID2);
      if (level2Map != null) {
        average = level2Map.get(itemID1);
        inverted = true;
      }
    }
    if (inverted) {
      if (average == null) {
        return null;
      }
      return stdDevWeighted ?
             new InvertedRunningAverageAndStdDev((RunningAverageAndStdDev) average) :
             new InvertedRunningAverage(average);
    } else {
      return average;
    }
  }

  @Override
  public RunningAverage[] getDiffs(Object userID, Object itemID, Preference[] prefs) {
    try {
      buildAverageDiffsLock.readLock().lock();
      int size = prefs.length;
      RunningAverage[] result = new RunningAverage[size];
      for (int i = 0; i < size; i++) {
        result[i] = getDiff(prefs[i].getItem().getID(), itemID);
      }
      return result;
    } finally {
      buildAverageDiffsLock.readLock().unlock();
    }
  }

  @Override
  public RunningAverage getAverageItemPref(Object itemID) {
    return averageItemPref.get(itemID);
  }

  @Override
  public void updateItemPref(Object itemID, double prefDelta, boolean remove) {
    if (!remove && stdDevWeighted) {
      throw new UnsupportedOperationException("Can't update only when stdDevWeighted is set");
    }
    try {
      buildAverageDiffsLock.readLock().lock();
      for (Map.Entry<Object, FastMap<Object, RunningAverage>> entry : averageDiffs.entrySet()) {
        boolean matchesItemID1 = itemID.equals(entry.getKey());
        for (Map.Entry<Object, RunningAverage> entry2 : entry.getValue().entrySet()) {
          RunningAverage average = entry2.getValue();
          if (matchesItemID1) {
            if (remove) {
              average.removeDatum(prefDelta);
            } else {
              average.changeDatum(-prefDelta);
            }
          } else if (itemID.equals(entry2.getKey())) {
            if (remove) {
              average.removeDatum(-prefDelta);
            } else {
              average.changeDatum(prefDelta);
            }
          }
        }
      }
      RunningAverage itemAverage = averageItemPref.get(itemID);
      if (itemAverage != null) {
        itemAverage.changeDatum(prefDelta);
      }
    } finally {
      buildAverageDiffsLock.readLock().unlock();
    }
  }

  @Override
  public Set<Item> getRecommendableItems(Object userID) throws TasteException {
    User user = dataModel.getUser(userID);
    Set<Item> result = allRecommendableItemIDs.clone();
    Iterator<Item> it = result.iterator();
    while (it.hasNext()) {
      if (user.getPreferenceFor(it.next().getID()) != null) {
        it.remove();
      }
    }
    return result;
  }

  private void buildAverageDiffs() throws TasteException {
    log.info("Building average diffs...");
    try {
      buildAverageDiffsLock.writeLock().lock();
      averageDiffs.clear();
      long averageCount = 0L;
      for (User user : dataModel.getUsers()) {
        averageCount = processOneUser(averageCount, user);
      }

      pruneInconsequentialDiffs();
      updateAllRecommendableItems();

    } finally {
      buildAverageDiffsLock.writeLock().unlock();
    }
  }

  private void pruneInconsequentialDiffs() {
    // Go back and prune inconsequential diffs. "Inconsequential" means, here, only represented by one
    // data point, so possibly unreliable
    Iterator<FastMap<Object, RunningAverage>> it1 = averageDiffs.values().iterator();
    while (it1.hasNext()) {
      FastMap<Object, RunningAverage> map = it1.next();
      Iterator<RunningAverage> it2 = map.values().iterator();
      while (it2.hasNext()) {
        RunningAverage average = it2.next();
        if (average.getCount() <= 1) {
          it2.remove();
        }
      }
      if (map.isEmpty()) {
        it1.remove();
      } else {
        map.rehash();
      }
    }
    averageDiffs.rehash();
  }

  private void updateAllRecommendableItems() throws TasteException {
    FastSet<Object> ids = new FastSet<Object>(dataModel.getNumItems());
    for (Map.Entry<Object, FastMap<Object, RunningAverage>> entry : averageDiffs.entrySet()) {
      ids.add(entry.getKey());
      for (Object id : entry.getValue().keySet()) {
        ids.add(id);
      }
    }
    allRecommendableItemIDs.clear();
    for (Object id : ids) {
      allRecommendableItemIDs.add(dataModel.getItem(id));
    }
    allRecommendableItemIDs.rehash();
  }

  private long processOneUser(long averageCount, User user) {
    log.debug("Processing prefs for user {}", user);
    // Save off prefs for the life of this loop iteration
    Preference[] userPreferences = user.getPreferencesAsArray();
    int length = userPreferences.length;
    for (int i = 0; i < length; i++) {
      Preference prefA = userPreferences[i];
      double prefAValue = prefA.getValue();
      Object itemIDA = prefA.getItem().getID();
      FastMap<Object, RunningAverage> aMap = averageDiffs.get(itemIDA);
      if (aMap == null) {
        aMap = new FastMap<Object, RunningAverage>();
        averageDiffs.put(itemIDA, aMap);
      }
      for (int j = i + 1; j < length; j++) {
        // This is a performance-critical block
        Preference prefB = userPreferences[j];
        Object itemIDB = prefB.getItem().getID();
        RunningAverage average = aMap.get(itemIDB);
        if (average == null && averageCount < maxEntries) {
          average = buildRunningAverage();
          aMap.put(itemIDB, average);
          averageCount++;
        }
        if (average != null) {
          average.addDatum(prefB.getValue() - prefAValue);
        }

      }
      RunningAverage itemAverage = averageItemPref.get(itemIDA);
      if (itemAverage == null) {
        itemAverage = buildRunningAverage();
        averageItemPref.put(itemIDA, itemAverage);
      }
      itemAverage.addDatum(prefAValue);
    }
    return averageCount;
  }

  private RunningAverage buildRunningAverage() {
    if (stdDevWeighted) {
      return compactAverages ? new CompactRunningAverageAndStdDev() : new FullRunningAverageAndStdDev();
    } else {
      return compactAverages ? new CompactRunningAverage() : new FullRunningAverage();
    }
  }

  @Override
  public void refresh(Collection<Refreshable> alreadyRefreshed) {
    refreshHelper.refresh(alreadyRefreshed);
  }

  @Override
  public String toString() {
    return "MemoryDiffStorage";
  }

}
