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

package org.apache.mahout.cf.taste.impl.model.jdbc;

import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.StackObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * <p>A wrapper {@link DataSource} which pools connections. Why can't Jakarta Commons DBCP provide this directly?</p>
 */
public final class ConnectionPoolDataSource implements DataSource {

  private static final Logger log = LoggerFactory.getLogger(ConnectionPoolDataSource.class);

  private final DataSource delegate;

  public ConnectionPoolDataSource(DataSource underlyingDataSource) {
    if (underlyingDataSource == null) {
      throw new IllegalArgumentException("underlyingDataSource is null");
    }
    PoolableObjectFactory poolFactory = new DataSourceConnectionFactory(underlyingDataSource);
    ObjectPool connectionPool = new StackObjectPool(poolFactory);
    this.delegate = new PoolingDataSource(connectionPool);
  }

  @Override
  public Connection getConnection() throws SQLException {
    return delegate.getConnection();
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return delegate.getConnection(username, password);
  }

  @Override
  public PrintWriter getLogWriter() throws SQLException {
    return delegate.getLogWriter();
  }

  @Override
  public void setLogWriter(PrintWriter printWriter) throws SQLException {
    delegate.setLogWriter(printWriter);
  }

  @Override
  public void setLoginTimeout(int timeout) throws SQLException {
    delegate.setLoginTimeout(timeout);
  }

  @Override
  public int getLoginTimeout() throws SQLException {
    return delegate.getLoginTimeout();
  }

  // These two methods are new in JDK 6, so they are added to allow it to compile in JDK 6. Really, they
  // should also delegate to the 'delegate' object. But that would then *only* compile in JDK 6. So for
  // now they are dummy implementations which do little.

  /**
   * @throws SQLException always
   */
  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    throw new SQLException("Unsupported operation");
  }

  /**
   * @return false always
   */
  @Override
  public boolean isWrapperFor(Class<?> iface) {
    return false;
  }

  private static class DataSourceConnectionFactory implements PoolableObjectFactory {

    private final DataSource dataSource;

    private DataSourceConnectionFactory(DataSource dataSource) {
      this.dataSource = dataSource;
    }

    @Override
    public Object makeObject() throws SQLException {
      log.debug("Obtaining pooled connection");
      return dataSource.getConnection();
    }

    @Override
    public void destroyObject(Object o) throws SQLException {
      log.debug("Closing pooled connection");
      ((Connection) o).close();
    }

    @Override
    public boolean validateObject(Object o) {
      return true;
    }

    @Override
    public void activateObject(Object o) {
      // do nothing
    }

    @Override
    public void passivateObject(Object o) {
      // do nothing
    }
  }

}
