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

package org.apache.mahout.clustering.dirichlet;

import java.io.IOException;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;
import org.apache.mahout.matrix.DenseVector;
import org.apache.mahout.matrix.TimesFunction;
import org.apache.mahout.matrix.Vector;

public class DirichletMapper extends MapReduceBase implements
    Mapper<WritableComparable<?>, Text, Text, Text> {

  DirichletState<Vector> state;

  @Override
  public void map(WritableComparable<?> key, Text values,
      OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
    Vector v = DenseVector.decodeFormat(values.toString());
    // compute a normalized vector of probabilities that v is described by each model
    Vector pi = normalizedProbabilities(state, v);
    // then pick one model by sampling a Multinomial distribution based upon them
    // see: http://en.wikipedia.org/wiki/Multinomial_distribution
    int k = UncommonDistributions.rMultinom(pi);
    output.collect(new Text(String.valueOf(k)), values);
  }

  public void configure(DirichletState<Vector> state) {
    this.state = state;
  }

  @Override
  public void configure(JobConf job) {
    super.configure(job);
    state = getDirichletState(job);
  }

  public static DirichletState<Vector> getDirichletState(JobConf job) {
    String statePath = job.get(DirichletDriver.STATE_IN_KEY);
    String modelFactory = job.get(DirichletDriver.MODEL_FACTORY_KEY);
    String numClusters = job.get(DirichletDriver.NUM_CLUSTERS_KEY);
    String alpha_0 = job.get(DirichletDriver.ALPHA_0_KEY);

    try {
      DirichletState<Vector> state = DirichletDriver.createState(modelFactory,
          new Integer(numClusters), new Double(alpha_0));
      FileSystem fs = FileSystem.get(job);
      Path path = new Path(statePath);
      FileStatus[] status = fs.listStatus(path);
      for (FileStatus s : status) {
        SequenceFile.Reader reader = new SequenceFile.Reader(fs, s.getPath(),
            job);
        try {
          Text key = new Text();
          Text value = new Text();
          while (reader.next(key, value)) {
            int index = new Integer(key.toString());
            String formatString = value.toString();
            DirichletCluster<Vector> cluster = DirichletCluster
                .fromFormatString(formatString);
            state.clusters.set(index, cluster);
          }
        } finally {
          reader.close();
        }
      }
      // TODO: with more than one mapper, they will all have different mixtures. Will this matter?
      state.mixture = UncommonDistributions.rDirichlet(state.totalCounts());
      return state;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Compute a normalized vector of probabilities that v is described
   * by each model using the mixture and the model pdfs
   * 
   * @param state the DirichletState<Vector> of this iteration
   * @param v an Vector
   * @return the Vector of probabilities
   */
  private static Vector normalizedProbabilities(DirichletState<Vector> state, Vector v) {
    Vector pi = new DenseVector(state.numClusters);
    double max = 0;
    for (int k = 0; k < state.numClusters; k++) {
      double p = state.adjustedProbability(v, k);
      pi.set(k, p);
      if (max < p)
        max = p;
    }
    // normalize the probabilities by largest observed value
    pi.assign(new TimesFunction(), 1.0 / max);
    return pi;
  }
}
