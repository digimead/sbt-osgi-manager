/**
 * Copy of bnd/biz.aQute.resolve. Reason: there are no Maven artifacts for bootstrap.
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

package biz.aQute.resolve.internal;

import java.util.*;

public class MapToDictionaryAdapter extends Dictionary<String, Object> {

	private final Map<String, Object> map;

	public MapToDictionaryAdapter(Map<String, Object> map) {
		this.map = map;
	}

	@Override
	public Enumeration<Object> elements() {
		final Iterator<Object> iter = map.values().iterator();
		return new Enumeration<Object>() {
			public boolean hasMoreElements() {
				return iter.hasNext();
			}

			public Object nextElement() {
				return iter.next();
			}
		};
	}

	@Override
	public Object get(Object key) {
		return map.get(key);
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public Enumeration<String> keys() {
		final Iterator<String> iter = map.keySet().iterator();
		return new Enumeration<String>() {
			public boolean hasMoreElements() {
				return iter.hasNext();
			}

			public String nextElement() {
				return iter.next();
			}
		};
	}

	@Override
	public Object put(String key, Object value) {
		return map.put(key, value);
	}

	@Override
	public Object remove(Object key) {
		return map.remove(key);
	}

	@Override
	public int size() {
		return map.size();
	}

}
