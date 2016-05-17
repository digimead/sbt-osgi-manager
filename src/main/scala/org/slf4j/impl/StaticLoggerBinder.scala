/**
 * sbt-osgi-manager - OSGi development bridge based on Bnd and Tycho.
 *
 * Copyright (c) 2016 Alexey Aksenov ezh@ezh.msk.ru
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

package org.slf4j.impl

import org.slf4j.{ ILoggerFactory, Logger, Marker }
import org.slf4j.spi.LoggerFactoryBinder
import sbt.osgi.manager.Plugin
import java.io.StringWriter
import java.io.PrintWriter

class StaticLoggerBinder private () extends LoggerFactoryBinder {
  private val factory = new StaticLoggerBinder.Factory
  private val classString = classOf[StaticLoggerBinder.Factory].getName

  override def getLoggerFactory: ILoggerFactory = factory

  override def getLoggerFactoryClassStr: String = classString
}

object StaticLoggerBinder extends StaticLoggerBinder {
  val REQUESTED_API_VERSION = "1.7.15"

  def getSingleton: StaticLoggerBinder = this

  class Factory extends ILoggerFactory {
    def getLogger(name: String): Logger = new PluginLogger(name)
  }
  class PluginLogger(name: String) extends Logger {
    def debug(marker: Marker, message: String, throwable: Throwable): Unit =
      doLog(sbt.Level.Debug, message, throwable)
    def debug(marker: Marker, message: String, args: Object*): Unit =
      doLog(sbt.Level.Debug, message.format(args), null)
    def debug(marker: Marker, message: String, arg1: Any, arg2: Any): Unit =
      doLog(sbt.Level.Debug, message.format(arg1, arg2), null)
    def debug(marker: Marker, message: String, arg1: Any): Unit =
      doLog(sbt.Level.Debug, message.format(arg1), null)
    def debug(marker: Marker, message: String): Unit =
      doLog(sbt.Level.Debug, message, null)
    def debug(message: String, throwable: Throwable): Unit =
      doLog(sbt.Level.Debug, message, throwable)
    def debug(message: String, args: Object*): Unit =
      doLog(sbt.Level.Debug, message.format(args), null)
    def debug(message: String, arg1: Any, arg2: Any): Unit =
      doLog(sbt.Level.Debug, message.format(arg1, arg2), null)
    def debug(message: String, arg1: Any): Unit =
      doLog(sbt.Level.Debug, message.format(arg1), null)
    def debug(message: String): Unit =
      doLog(sbt.Level.Debug, message, null)

    def error(marker: Marker, message: String, throwable: Throwable): Unit =
      doLog(sbt.Level.Error, message, throwable)
    def error(marker: Marker, message: String, args: Object*): Unit =
      doLog(sbt.Level.Error, message, null)
    def error(marker: Marker, message: String, arg1: Any, arg2: Any): Unit =
      doLog(sbt.Level.Error, message.format(arg1, arg2), null)
    def error(marker: Marker, message: String, arg1: Any): Unit =
      doLog(sbt.Level.Error, message.format(arg1), null)
    def error(marker: Marker, message: String): Unit =
      doLog(sbt.Level.Error, message, null)
    def error(message: String, throwable: Throwable): Unit =
      doLog(sbt.Level.Error, message, throwable)
    def error(message: String, args: Object*): Unit =
      doLog(sbt.Level.Error, message.format(args), null)
    def error(message: String, arg1: Any, arg2: Any): Unit =
      doLog(sbt.Level.Error, message.format(arg1, arg2), null)
    def error(message: String, arg1: Any): Unit =
      doLog(sbt.Level.Error, message.format(arg1), null)
    def error(message: String): Unit =
      doLog(sbt.Level.Error, message, null)

    def getName(): String = name

    def info(marker: Marker, message: String, throwable: Throwable): Unit =
      doLog(sbt.Level.Error, message, throwable)
    def info(marker: Marker, message: String, args: Object*): Unit =
      doLog(sbt.Level.Error, message.format(args), null)
    def info(marker: Marker, message: String, arg1: Any, arg2: Any): Unit =
      doLog(sbt.Level.Error, message.format(arg1, arg2), null)
    def info(marker: Marker, message: String, arg1: Any): Unit =
      doLog(sbt.Level.Error, message.format(arg1), null)
    def info(marker: Marker, message: String): Unit =
      doLog(sbt.Level.Error, message, null)
    def info(message: String, throwable: Throwable): Unit =
      doLog(sbt.Level.Error, message, throwable)
    def info(message: String, args: Object*): Unit =
      doLog(sbt.Level.Error, message.format(args), null)
    def info(message: String, arg1: Any, arg2: Any): Unit =
      doLog(sbt.Level.Error, message.format(arg1, arg2), null)
    def info(message: String, arg1: Any): Unit =
      doLog(sbt.Level.Error, message.format(arg1), null)
    def info(message: String): Unit =
      doLog(sbt.Level.Error, message, null)

    def isDebugEnabled(marker: Marker): Boolean = true
    def isDebugEnabled(): Boolean = true
    def isErrorEnabled(marker: Marker): Boolean = true
    def isErrorEnabled(): Boolean = true
    def isInfoEnabled(marker: Marker): Boolean = true
    def isInfoEnabled(): Boolean = true
    def isTraceEnabled(marker: Marker): Boolean = false
    def isTraceEnabled(): Boolean = false
    def isWarnEnabled(marker: Marker): Boolean = true
    def isWarnEnabled(): Boolean = true

    def trace(marker: Marker, message: String, throwable: Throwable): Unit = {}
    def trace(marker: Marker, message: String, args: Object*): Unit = {}
    def trace(marker: Marker, message: String, arg2: Any, x$4: Any): Unit = {}
    def trace(marker: Marker, message: String, arg2: Any): Unit = {}
    def trace(marker: Marker, message: String): Unit = {}
    def trace(message: String, throwable: Throwable): Unit = {}
    def trace(message: String, args: Object*): Unit = {}
    def trace(message: String, arg1: Any, arg2: Any): Unit = {}
    def trace(message: String, arg1: Any): Unit = {}
    def trace(message: String): Unit = {}

    def warn(marker: Marker, message: String, throwable: Throwable): Unit =
      doLog(sbt.Level.Error, message, throwable)
    def warn(marker: Marker, message: String, args: Object*): Unit =
      doLog(sbt.Level.Error, message.format(args), null)
    def warn(marker: Marker, message: String, arg1: Any, arg2: Any): Unit =
      doLog(sbt.Level.Error, message.format(arg1, arg2), null)
    def warn(marker: Marker, message: String, arg1: Any): Unit =
      doLog(sbt.Level.Error, message.format(arg1), null)
    def warn(marker: Marker, message: String): Unit =
      doLog(sbt.Level.Error, message, null)
    def warn(message: String, throwable: Throwable): Unit =
      doLog(sbt.Level.Error, message, throwable)
    def warn(message: String, arg1: Any, arg2: Any): Unit =
      doLog(sbt.Level.Error, message.format(arg1, arg2), null)
    def warn(message: String, args: Object*): Unit =
      doLog(sbt.Level.Error, message.format(args), null)
    def warn(message: String, arg1: Any): Unit =
      doLog(sbt.Level.Error, message.format(arg1), null)
    def warn(message: String): Unit =
      doLog(sbt.Level.Error, message, null)

    protected def doLog(level: sbt.Level.Value, message: String, throwable: Throwable) {
      val writer = new StringWriter()
      val printWriter = new PrintWriter(writer)
      printWriter.println(message)
      if (throwable != null) throwable.printStackTrace(printWriter)
      level match {
        case sbt.Level.Debug ⇒ withLog { logger ⇒ logger.debug(writer.toString()) }
        case sbt.Level.Error ⇒ withLog { logger ⇒ logger.error(writer.toString()) }
        case sbt.Level.Info ⇒ withLog { logger ⇒ logger.info(writer.toString()) }
        case sbt.Level.Warn ⇒ withLog { logger ⇒ logger.warn(writer.toString()) }
      }
    }
    protected def withLog[T](f: sbt.Logger ⇒ T): Option[T] = Plugin.getLastKnownState().map(state ⇒ f(state.log))
  }
}
