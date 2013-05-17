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

package aQute.libg.glob;

import java.util.*;
import java.util.regex.*;

public class Glob {

	private final String	glob;
	private final Pattern	pattern;

	public Glob(String globString) {
		this.glob = globString;
		this.pattern = Pattern.compile(convertGlobToRegEx(globString));
	}

	public Matcher matcher(CharSequence input) {
		return pattern.matcher(input);
	}

	@Override
	public String toString() {
		return glob;
	}

	private static String convertGlobToRegEx(String line) {
		line = line.trim();
		int strLen = line.length();
		StringBuilder sb = new StringBuilder(strLen);
		boolean escaping = false;
		int inCurlies = 0;
		for (char currentChar : line.toCharArray()) {
			switch (currentChar) {
				case '*' :
					if (escaping)
						sb.append("\\*");
					else
						sb.append(".*");
					escaping = false;
					break;
				case '?' :
					if (escaping)
						sb.append("\\?");
					else
						sb.append('.');
					escaping = false;
					break;
				case '.' :
				case '(' :
				case ')' :
				case '+' :
				case '|' :
				case '^' :
				case '$' :
				case '@' :
				case '%' :
					sb.append('\\');
					sb.append(currentChar);
					escaping = false;
					break;
				case '\\' :
					if (escaping) {
						sb.append("\\\\");
						escaping = false;
					} else
						escaping = true;
					break;
				case '{' :
					if (escaping) {
						sb.append("\\{");
					} else {
						sb.append('(');
						inCurlies++;
					}
					escaping = false;
					break;
				case '}' :
					if (inCurlies > 0 && !escaping) {
						sb.append(')');
						inCurlies--;
					} else if (escaping)
						sb.append("\\}");
					else
						sb.append("}");
					escaping = false;
					break;
				case ',' :
					if (inCurlies > 0 && !escaping) {
						sb.append('|');
					} else if (escaping)
						sb.append("\\,");
					else
						sb.append(",");
					break;
				default :
					escaping = false;
					sb.append(currentChar);
			}
		}
		return sb.toString();
	}

	public void select(List<?> objects) {
		for ( Iterator<?> i =objects.iterator(); i.hasNext(); ) {
			String s = i.next().toString();
			if ( !matcher(s).matches())
				i.remove();
		}
	}

	public static Pattern toPattern(String s) {
		try {
			return Pattern.compile( convertGlobToRegEx(s));
		} catch( Exception e) {
			// ignore
		}
		return null;
	}
}
