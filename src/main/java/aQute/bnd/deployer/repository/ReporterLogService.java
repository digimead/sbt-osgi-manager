/**
 * Copy of the code from github.com/bndtools/bnd, reason: not available at Maven central or other repository
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

package aQute.bnd.deployer.repository;

import org.osgi.framework.*;
import org.osgi.service.log.*;

import aQute.service.reporter.*;

public class ReporterLogService implements LogService {

	private final Reporter	reporter;

	public ReporterLogService(Reporter reporter) {
		this.reporter = reporter;
	}

	public void log(int level, String message) {
		log(null, level, message, null);
	}

	public void log(int level, String message, Throwable t) {
		log(null, level, message, t);
	}

	public void log(ServiceReference sr, int level, String message) {
		log(sr, level, message, null);
	}

	public void log(ServiceReference sr, int level, String message, Throwable t) {
		if (t != null)
			message += " [" + t + "]";

		if (reporter != null) {
			if (level <= LOG_ERROR)
				reporter.error(message);
			else if (level == LOG_WARNING)
				reporter.warning(message);
			else if (level == LOG_INFO || level == LOG_DEBUG)
				reporter.trace(message);
		}
	}

}
