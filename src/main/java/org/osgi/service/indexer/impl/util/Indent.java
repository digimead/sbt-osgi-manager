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
import java.util.Arrays;

/**
 * Represents an indent in a file
 */
public class Indent {
	/** the platform specific EOL */
	static private String eol = String.format("%n");

	/** true when a newline must be printed before the indent */
	private final boolean newLine;

	/** the level of the indent */
	private final int level;

	/** the increment for the next indent */
	private final int increment;

	/** the indent string */
	private char[] indent;

	/** no indent */
	public static final Indent NONE = new Indent(false, 0, 0);

	/** default indent (2 spaces) */
	public static final Indent PRETTY = new Indent(true, 0, 2);

	/**
	 * Constructor
	 *
	 * @param newLine
	 *            true when a newline must be printed before the indent
	 * @param level
	 *            the level of the indent
	 * @param increment
	 *            the increment for the next indent
	 */
	private Indent(boolean newLine, int level, int increment) {
		this.newLine = newLine;
		this.level = level;
		this.increment = increment;
		this.indent = new char[level];
		Arrays.fill(this.indent, ' ');
	}

	/**
	 * Print the indent
	 *
	 * @param pw
	 *            the writer to print to
	 */
	public void print(PrintWriter pw) {
		if (newLine)
			pw.print(eol);
		pw.print(indent);
	}

	/**
	 * @return the next indent when the increment is larger than zero, this
	 *         indent otherwise
	 */
	public Indent next() {
		return (increment <= 0) ? this : new Indent(newLine, level + increment, increment);
	}
}
