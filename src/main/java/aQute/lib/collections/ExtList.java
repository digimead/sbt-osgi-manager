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

package aQute.lib.collections;

import java.util.*;

public class ExtList<T> extends ArrayList<T> {
	private static final long	serialVersionUID	= 1L;

	public ExtList(T... ts) {
		super(ts.length);
		for (T t : ts) {
			add(t);
		}
	}

	public ExtList(int size) {
		super(size);
	}

	public ExtList(Collection<T> arg) {
		super(arg);
	}

	public ExtList(Iterable<T> arg) {
		for ( T t : arg)
			add(t);
	}

	public static ExtList<String> from(String s) {
		// TODO make sure no \ before comma
		return from(s, "\\s*,\\s*");
	}
	public static ExtList<String> from(String s, String delimeter) {
		ExtList<String> result = new ExtList<String>();
		String[] parts = s.split(delimeter);
		for (String p : parts)
			result.add(p);
		return result;
	}

	public String join() {
		return join(",");
	}

	public String join(String del) {
		StringBuilder sb = new StringBuilder();
		String d = "";
		for (T t : this) {
			sb.append(d);
			d = del;
			if (t != null)
				sb.append(t.toString());
		}
		return sb.toString();
	}

}
