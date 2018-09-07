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

package org.apache.mahout.cf.taste.example.netflix;

import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.User;
import org.apache.mahout.cf.taste.model.Item;
import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.cf.taste.common.Refreshable;
import org.apache.mahout.cf.taste.impl.common.FileLineIterable;
import org.apache.mahout.cf.taste.impl.common.ArrayIterator;
import org.apache.mahout.cf.taste.impl.model.GenericPreference;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public final class NetflixFileDataModel implements DataModel {

  private final File dataDirectory;
  private final List<NetflixMovie> movies;

  public NetflixFileDataModel(File dataDirectory) throws IOException {
		if (dataDirectory == null) {
			throw new IllegalArgumentException("dataDirectory is null");
		}
		if (!dataDirectory.exists() || !dataDirectory.isDirectory()) {
			throw new FileNotFoundException(dataDirectory.toString());
		}

    this.dataDirectory = dataDirectory;
    movies = NetflixMovie.readMovies(dataDirectory);
  }

  @Override
  public Iterable<? extends User> getUsers() {
    throw new UnsupportedOperationException(); // TODO
  }

  @Override
  public User getUser(Object id) {
    throw new UnsupportedOperationException(); // TODO
  }

  @Override
  public Iterable<? extends Item> getItems() {
    return movies;
  }

  @Override
  public Item getItem(Object id) {
    return movies.get((Integer) id - 1);
  }

  @Override
  public Iterable<? extends Preference> getPreferencesForItem(Object itemID) {
    return new ArrayIterator<Preference>(getPreferencesForItemAsArray(itemID));
  }

  @Override
  public Preference[] getPreferencesForItemAsArray(Object itemID) {
    StringBuilder itemIDPadded = new StringBuilder(5);
    itemIDPadded.append(itemID);
    while (itemIDPadded.length() < 5) {
      itemIDPadded.insert(0, '0');
    }
    List<Preference> prefs = new ArrayList<Preference>();
    Item movie = getItem(itemID);
    File movieFile = new File(new File(dataDirectory, "training_set"), "mv_00" + itemIDPadded + ".txt");
    for (String line : new FileLineIterable(movieFile, true)) {
      int firstComma = line.indexOf((int) ',');
      Integer userID = Integer.valueOf(line.substring(0, firstComma));
      int secondComma = line.indexOf((int) ',', firstComma + 1);
      double rating = Double.parseDouble(line.substring(firstComma + 1, secondComma));
      prefs.add(new GenericPreference(getUser(userID), movie, rating));
    }
    return prefs.toArray(new Preference[prefs.size()]);
  }

  @Override
  public int getNumItems() {
    return movies.size();
  }

  @Override
  public int getNumUsers() {
    throw new UnsupportedOperationException(); // TODO
  }

  @Override
  public int getNumUsersWithPreferenceFor(Object... itemIDs) {
    throw new UnsupportedOperationException(); // TODO
  }

  /**
   * @throws UnsupportedOperationException
   */
  @Override
  public void setPreference(Object userID, Object itemID, double value) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException
   */
  @Override
  public void removePreference(Object userID, Object itemID) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void refresh(Collection<Refreshable> alreadyRefreshed) {
    // do nothing
  }

  @Override
  public String toString() {
    return "NetflixFileDataModel";
  }

}
