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

package org.apache.mahout.cf.taste.hadoop;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;

import java.io.IOException;
import java.util.Iterator;

public final class SlopeOneDiffsToAveragesReducer
    extends MapReduceBase
    implements Reducer<ItemItemWritable, DoubleWritable, ItemItemWritable, DoubleWritable> {

  @Override
  public void reduce(ItemItemWritable key,
                     Iterator<DoubleWritable> values,
                     OutputCollector<ItemItemWritable, DoubleWritable> output,
                     Reporter reporter) throws IOException {
    int count = 0;
    double total = 0.0;
    while (values.hasNext()) {
      total += values.next().get();
      count++;
    }
    output.collect(key, new DoubleWritable((total / count)));
  }

}