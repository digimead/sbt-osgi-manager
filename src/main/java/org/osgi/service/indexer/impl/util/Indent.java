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

package org.osgi.service.indexer.impl.util;

import java.io.PrintWriter;

public class Indent {

	private final boolean newLine;
	private final int level;
	private final int increment;

	public static final Indent NONE = new Indent(false, 0, 0);
	public static final Indent PRETTY = new Indent(true, 0, 2);

	private Indent(boolean newLine, int level, int increment) {
		this.newLine = newLine;
		this.level = level;
		this.increment = increment;
	}

	public void print(PrintWriter pw) {
		if (newLine)
			pw.print('\n');
		int n = level;
		while (n-- > 0)
			pw.print(' ');
	}

	public Indent next() {
		return (increment <= 0) ? this : new Indent(newLine, level + increment, increment);
	}
}
