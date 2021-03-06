/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.duktape;

import java.io.Closeable;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/** A simple EMCAScript (Javascript) interpreter. */
public final class Duktape implements Closeable {
  static {
    System.loadLibrary("duktape");
  }

  /**
   * Create a new interpreter instance. Calls to this method <strong>must</strong> matched with
   * calls to {@link #close()} on the returned instance to avoid leaking native memory.
   */
  public static Duktape create() {
    long context = createContext();
    if (context == 0) {
      throw new OutOfMemoryError("Cannot create Duktape instance");
    }
    return new Duktape(context);
  }

  private long context;

  private Duktape(long context) {
    this.context = context;
  }

  /**
   * Evaluate {@code script} and return any result.  {@code fileName} will be used in error
   * reporting.
   *
   * @throws DuktapeException if there is an error evaluating the script.
   */
  public synchronized String evaluate(String script, String fileName) {
    return evaluate(context, script, fileName);
  }
  /**
   * Evaluate {@code script} and return any result.
   *
   * @throws DuktapeException if there is an error evaluating the script.
   */
  public String evaluate(String script) {
    return evaluate(script, "?");
  }

  /**
   * Release the native resources associated with this object. You <strong>must</strong> call this
   * method for each instance to avoid leaking native memory.
   */
  @Override public synchronized void close() {
    if (context != 0) {
      long contextToClose = context;
      context = 0;
      destroyContext(contextToClose);
    }
  }

  @Override protected synchronized void finalize() throws Throwable {
    if (context != 0) {
      Logger.getLogger(getClass().getName()).warning("Duktape instance leaked!");
    }
  }

  private static native long createContext();
  private static native void destroyContext(long context);
  private static native String evaluate(long context, String sourceCode, String fileName);

  /** Returns the timezone offset in seconds given system time millis. */
  @SuppressWarnings("unused") // Called from native code.
  private static int getLocalTimeZoneOffset(double t) {
    int offsetMillis = TimeZone.getDefault().getOffset((long) t);
    return (int) TimeUnit.MILLISECONDS.toSeconds(offsetMillis);
  }
}
