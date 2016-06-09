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

import java.util.List;

public class QuotedTokenizer {
	String string;
	int index = 0;
	String separators;
	boolean returnTokens;
	boolean ignoreWhiteSpace = true;
	String peek;
	char separator;

	public QuotedTokenizer(String string, String separators, boolean returnTokens) {
		if (string == null)
			throw new IllegalArgumentException("string argument must be not null");
		this.string = string;
		this.separators = separators;
		this.returnTokens = returnTokens;
	}

	public QuotedTokenizer(String string, String separators) {
		this(string, separators, false);
	}

	public String nextToken(String separators) {
		separator = 0;
		if (peek != null) {
			String tmp = peek;
			peek = null;
			return tmp;
		}

		if (index == string.length())
			return null;

		StringBuilder sb = new StringBuilder();

		while (index < string.length()) {
			char c = string.charAt(index++);

			if (Character.isWhitespace(c)) {
				if (index == string.length())
					break;
				else {
					sb.append(c);
					continue;
				}
			}

			if (separators.indexOf(c) >= 0) {
				if (returnTokens)
					peek = Character.toString(c);
				else
					separator = c;
				break;
			}

			switch (c) {
			case '"':
			case '\'':
				quotedString(sb, c);
				break;

			default:
				sb.append(c);
			}
		}
		String result = sb.toString().trim();
		if (result.length() == 0 && index == string.length())
			return null;
		return result;
	}

	public String nextToken() {
		return nextToken(separators);
	}

	private void quotedString(StringBuilder sb, char c) {
		char quote = c;
		while (index < string.length()) {
			c = string.charAt(index++);
			if (c == quote)
				break;
			if (c == '\\' && index < string.length() && string.charAt(index + 1) == quote)
				c = string.charAt(index++);
			sb.append(c);
		}
	}

	public String[] getTokens() {
		return getTokens(0);
	}

	private String[] getTokens(int cnt) {
		String token = nextToken();
		if (token == null)
			return new String[cnt];

		String result[] = getTokens(cnt + 1);
		result[cnt] = token;
		return result;
	}

	public char getSeparator() {
		return separator;
	}

	public List<String> getTokenSet() {
		List<String> list = Create.list();
		String token = nextToken();
		while (token != null) {
			list.add(token);
			token = nextToken();
		}
		return list;
	}
}
