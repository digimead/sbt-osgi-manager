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

public class Logic {

	public static <T> Collection<T> retain(Collection<T> first, Collection<T>... sets) {
		Set<T> result = new HashSet<T>(first);
		for (Collection<T> set : sets) {
			result.retainAll(set);
		}
		return result;
	}

	public static <T> Collection<T> remove(Collection<T> first, Collection<T>... sets) {
		Set<T> result = new HashSet<T>(first);
		for (Collection<T> set : sets) {
			result.removeAll(set);
		}
		return result;
	}
}
