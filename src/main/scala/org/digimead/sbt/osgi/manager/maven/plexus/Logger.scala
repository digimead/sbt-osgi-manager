/**
 * sbt-osgi-manager - OSGi development bridge based on Bnd and Tycho.
 *
 * Copyright (c) 2013 Alexey Aksenov ezh@ezh.msk.ru
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.digimead.sbt.osgi.manager.maven.plexus

import java.io.PrintWriter
import java.io.StringWriter

import org.codehaus.plexus.logging.AbstractLogger

import org.digimead.sbt.osgi.manager.Plugin
import org.digimead.sbt.osgi.manager.Support._

class Logger(threshold: Int, name: String) extends AbstractLogger(threshold, name) {
  def getChildLogger(name: String) = this
  def debug(message: String, throwable: Throwable) = Logger.debug(message, throwable)
  def error(message: String, throwable: Throwable) = Logger.error(message, throwable)
  def fatalError(message: String, throwable: Throwable) = Logger.fatalError(message, throwable)
  def info(message: String, throwable: Throwable) = Logger.info(message, throwable)
  def warn(message: String, throwable: Throwable) = Logger.warn(message, throwable)
}

object Logger {
  def debug(message: String, throwable: Throwable) = logger match {
    case Some(logger) =>
      Option(throwable) match {
        case Some(t) => logger.debug(logPrefix("*") + message + "\n" + getThrowableDump(t))
        case None => logger.debug(logPrefix("*") + message)
      }
    case None =>
      Option(throwable) match {
        case Some(t) => System.err.println(logPrefix("*") + "DEBUG: " + message + "\n" + getThrowableDump(t))
        case None => System.err.println(logPrefix("*") + "DEBUG: " + message)
      }
  }
  def error(message: String, throwable: Throwable) = logger match {
    case Some(logger) =>
      Option(throwable) match {
        case Some(t) => logger.error(logPrefix("*") + message + "\n" + getThrowableDump(t))
        case None => logger.error(logPrefix("*") + message)
      }
      logger.error(logPrefix("*") + message)
    case None =>
      Option(throwable) match {
        case Some(t) => System.err.println(logPrefix("*") + "ERROR: " + message + "\n" + getThrowableDump(t))
        case None => System.err.println(logPrefix("*") + "ERROR: " + message)
      }
  }
  def fatalError(message: String, throwable: Throwable) = logger match {
    case Some(logger) =>
      Option(throwable) match {
        case Some(t) => logger.error(logPrefix("*") + "FATAL: " + message + "\n" + getThrowableDump(t))
        case None => logger.error(logPrefix("*") + "FATAL: " + message)
      }
    case None =>
      Option(throwable) match {
        case Some(t) => System.err.println(logPrefix("*") + "FATAL: " + message + "\n" + getThrowableDump(t))
        case None => System.err.println(logPrefix("*") + "FATAL: " + message)
      }
  }
  def info(message: String, throwable: Throwable) = logger match {
    case Some(logger) =>
      Option(throwable) match {
        case Some(t) => logger.info(logPrefix("*") + message + "\n" + t)
        case None => logger.info(logPrefix("*") + message)
      }
    case None =>
      Option(throwable) match {
        case Some(t) => System.err.println(logPrefix("*") + "INFO: " + message + "\n" + getThrowableDump(t))
        case None => System.err.println(logPrefix("*") + "INFO: " + message)
      }
  }
  def warn(message: String, throwable: Throwable) = logger match {
    case Some(logger) =>
      Option(throwable) match {
        case Some(t) => logger.warn(logPrefix("*") + message + "\n" + getThrowableDump(t))
        case None => logger.warn(logPrefix("*") + message)
      }
    case None =>
      Option(throwable) match {
        case Some(t) => System.err.println(logPrefix("*") + "WARN: " + message + "\n" + getThrowableDump(t))
        case None => System.err.println(logPrefix("*") + "WARN: " + message)
      }
  }
  /** Get the default logger */
  def logger: Option[sbt.Logger] =
    Plugin.getLastKnownState.map(_.log)
  /** Get throwable as string */
  protected def getThrowableDump(throwable: Throwable): String = {
    val writer = new StringWriter()
    val printWriter = new PrintWriter(writer)
    if (throwable != null) throwable.printStackTrace(printWriter)
    writer.toString()
  }
}
