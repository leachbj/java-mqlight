/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.ibm.mqlight.api.logging;

import java.lang.reflect.Constructor;

/**
 * Factory to obtain a {@link Logger} implementation.
 */
public abstract class LoggerFactory {

  public static final String LOGGER_FACTORY_IMPL_CLASS_NAME= "com.ibm.mqlight.api.impl.logging.LoggerFactoryImpl";

  /** Implementation of a {@link LoggerFactory} */
  private static final LoggerFactory impl;
  static {
    LoggerFactory loggerFactoryImpl;
    try {
      Class<?> classToInstantiate = Class.forName(LOGGER_FACTORY_IMPL_CLASS_NAME);
      Constructor<?> constructor = classToInstantiate.getDeclaredConstructor();
      constructor.setAccessible(true);
      loggerFactoryImpl = (LoggerFactory) constructor.newInstance();

    } catch (Exception exception) {
      // No FFDC Code Needed.
      // We may not have any FFDC instantiated so simply print the stack.
      exception.printStackTrace();
      // Assume we have no chained exception support.
      throw new Error(exception.toString());
    }

    impl = loggerFactoryImpl;
  }

  /**
   * Obtains a {@link Logger} implementation for the specified class.
   *
   * @param clazz Class to be associated with the logger instance.
   * @return {@link Logger} instance for trace and information logging.
   */
  public static Logger getLogger(Class<?> clazz) {
    return impl.getLoggerImpl(clazz);
  }

  /**
   * Obtains a {@link Logger} implementation for the specified {@link org.slf4j.Logger}.
   *
   * @param logger The {@link org.slf4j.Logger} to be associated with the logger instance.
   * @return {@link Logger} instance for trace and information logging.
   */
  public static Logger getLogger(org.slf4j.Logger logger) {
    return impl.getLoggerImpl(logger);
  }

  /**
   * Internal method to obtain the {@link Logger} implementation from the static {@link LoggerFactory} implementation.
   *
   * @param clazz Class to be associated with the logger instance.
   * @return {@link Logger} instance for trace and information logging.
   */
  protected abstract Logger getLoggerImpl(Class<?> clazz);

  /**
   * Internal method to obtain the {@link Logger} implementation from the static {@link LoggerFactory} implementation.
   *
   * @param logger The SLF4J logger instance.
   * @return {@link Logger} instance for trace and information logging.
   */
  protected abstract Logger getLoggerImpl(org.slf4j.Logger logger);
}
