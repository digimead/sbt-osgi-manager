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

package sbt.osgi.manager

import java.util.Properties
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfigurationStub
import org.eclipse.tycho.core.resolver.shared.PlatformPropertiesUtils

object Environment {
  /* Keys.osgiEnv for current platform. */
  lazy val current = Seq((OS.current(), WS.current(), ARCH.current()))
  /* Keys.osgiEnv for current platform. */
  lazy val all = {
    def getWS(os: OS) = WS.all.map { ws ⇒ (os, ws) }
    def getARCH(osws: (OS, WS)) = ARCH.all.map { arch ⇒ (osws._1, osws._2, arch) }
    OS.all.map { os ⇒ getWS(os).map(getARCH).flatten }.flatten
  }

  /** Execution environment */
  object Execution {
    lazy val JavaSE6 = new ExecutionEnvironmentConfigurationStub("JavaSE-1.6")
    lazy val JavaSE7 = new ExecutionEnvironmentConfigurationStub("JavaSE-1.7")
    lazy val JavaSE8 = new ExecutionEnvironmentConfigurationStub("JavaSE-1.8")
    lazy val JavaSE9 = new ExecutionEnvironmentConfigurationStub("JavaSE-1.9")
    lazy val cdc10 = new ExecutionEnvironmentConfigurationStub("CDC-1.0/Foundation-1.0")
    lazy val cdc11 = new ExecutionEnvironmentConfigurationStub("CDC-1.1/Foundation-1.1")
    lazy val j2SE12 = new ExecutionEnvironmentConfigurationStub("J2SE-1.2")
    lazy val j2SE13 = new ExecutionEnvironmentConfigurationStub("J2SE-1.3")
    lazy val j2SE14 = new ExecutionEnvironmentConfigurationStub("J2SE-1.4")
    lazy val j2SE5 = new ExecutionEnvironmentConfigurationStub("J2SE-1.5")
    lazy val jre11 = new ExecutionEnvironmentConfigurationStub("JRE-1.1")
    lazy val osgiMin10 = new ExecutionEnvironmentConfigurationStub("OSGi/Minimum-1.0")
    lazy val osgiMin11 = new ExecutionEnvironmentConfigurationStub("OSGi/Minimum-1.1")
    lazy val osgiMin12 = new ExecutionEnvironmentConfigurationStub("OSGi/Minimum-1.2")
  }
  /** osgi.arch */
  case class ARCH(val value: String)
  object ARCH {
    lazy val all = Seq(X86, X86_64)
    lazy val X86 = ARCH(PlatformPropertiesUtils.ARCH_X86)
    lazy val X86_64 = ARCH(PlatformPropertiesUtils.ARCH_X86_64)

    /** Get current ARCH.*/
    def current(): ARCH =
      find(PlatformPropertiesUtils.getArch(new Properties())) getOrElse
        { throw new IllegalStateException("Unable to find current processor architecture") }
    /** Find ARCH by string. */
    def find(arch: String): Option[ARCH] = arch match {
      case (X86.value) ⇒ Some(X86)
      case (X86_64.value) ⇒ Some(X86_64)
      case _ ⇒ None
    }
  }
  /** osgi.os */
  case class OS(val value: String)
  object OS {
    lazy val all = Seq(AIX, HPUX, LINUX, MACOSX, QNX, SOLARIS, UNKNOWN, WIN32)
    lazy val AIX = OS(PlatformPropertiesUtils.OS_AIX)
    lazy val HPUX = OS(PlatformPropertiesUtils.OS_HPUX)
    lazy val LINUX = OS(PlatformPropertiesUtils.OS_LINUX)
    lazy val MACOSX = OS(PlatformPropertiesUtils.OS_MACOSX)
    lazy val QNX = OS(PlatformPropertiesUtils.OS_QNX)
    lazy val SOLARIS = OS(PlatformPropertiesUtils.OS_SOLARIS)
    lazy val UNKNOWN = OS(PlatformPropertiesUtils.OS_UNKNOWN)
    lazy val WIN32 = OS(PlatformPropertiesUtils.OS_WIN32)

    /** Get current OS.*/
    def current(): OS =
      find(PlatformPropertiesUtils.getOS(new Properties())) getOrElse
        { throw new IllegalStateException("Unable to find current operating system") }
    /** Find OS by string. */
    def find(os: String): Option[OS] = os match {
      case (AIX.value) ⇒ Some(AIX)
      case (HPUX.value) ⇒ Some(HPUX)
      case (LINUX.value) ⇒ Some(LINUX)
      case (MACOSX.value) ⇒ Some(MACOSX)
      case (QNX.value) ⇒ Some(QNX)
      case (SOLARIS.value) ⇒ Some(SOLARIS)
      case (UNKNOWN.value) ⇒ Some(UNKNOWN)
      case (WIN32.value) ⇒ Some(WIN32)
      case _ ⇒ None
    }
  }
  /** osgi.ws */
  case class WS(val value: String)
  object WS {
    lazy val all = Seq(CARBON, COCOA, GTK, MOTIF, PHOTON, UNKNOWN, WIN32, WPF)
    lazy val CARBON = WS(PlatformPropertiesUtils.WS_CARBON)
    lazy val COCOA = WS(PlatformPropertiesUtils.WS_COCOA)
    lazy val GTK = WS(PlatformPropertiesUtils.WS_GTK)
    lazy val MOTIF = WS(PlatformPropertiesUtils.WS_MOTIF)
    lazy val PHOTON = WS(PlatformPropertiesUtils.WS_PHOTON)
    lazy val UNKNOWN = WS(PlatformPropertiesUtils.WS_UNKNOWN)
    lazy val WIN32 = WS(PlatformPropertiesUtils.WS_WIN32)
    lazy val WPF = WS(PlatformPropertiesUtils.WS_WPF)

    /** Get current WS.*/
    def current(): WS =
      find(PlatformPropertiesUtils.getWS(new Properties())) getOrElse
        { throw new IllegalStateException("Unable to find current windowing system") }
    /** Find WS by string. */
    def find(ws: String): Option[WS] = ws match {
      case (CARBON.value) ⇒ Some(CARBON)
      case (COCOA.value) ⇒ Some(COCOA)
      case (GTK.value) ⇒ Some(GTK)
      case (MOTIF.value) ⇒ Some(MOTIF)
      case (PHOTON.value) ⇒ Some(PHOTON)
      case (UNKNOWN.value) ⇒ Some(UNKNOWN)
      case (WIN32.value) ⇒ Some(WIN32)
      case (WPF.value) ⇒ Some(WPF)
      case _ ⇒ None
    }
  }
}
