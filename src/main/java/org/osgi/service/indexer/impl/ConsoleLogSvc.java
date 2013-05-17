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

package org.osgi.service.indexer.impl;

import java.io.PrintStream;

import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

public class ConsoleLogSvc implements LogService {

	public void log(int level, String message) {
		log(null, level, message, null);
	}

	public void log(int level, String message, Throwable exception) {
		log(null, level, message, exception);
	}

	public void log(ServiceReference sr, int level, String message) {
		log(sr, level, message, null);
	}

	public void log(ServiceReference sr, int level, String message, Throwable exception) {
		PrintStream out = level <= LOG_WARNING ? System.err : System.out;

		StringBuilder builder = new StringBuilder();
		switch (level) {
		case LOG_DEBUG:
			builder.append("DEBUG");
			break;
		case LOG_INFO:
			builder.append("INFO");
			break;
		case LOG_WARNING:
			builder.append("WARNING");
			break;
		case LOG_ERROR:
			builder.append("ERROR");
			break;
		default:
			builder.append("<<unknown>>");
		}
		builder.append(": ");
		builder.append(message);

		if (exception != null) {
			builder.append(" [");
			builder.append(exception.getLocalizedMessage());
			builder.append("]");
		}

		out.println(builder.toString());

		if (exception != null)
			exception.printStackTrace(out);
	}

}
