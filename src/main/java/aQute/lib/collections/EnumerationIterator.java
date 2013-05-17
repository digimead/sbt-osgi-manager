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

/**
 * Simple facade for enumerators so they can be used in for loops.
 *
 * @param <T>
 */
public class EnumerationIterator<T> implements Iterable<T>, Iterator<T> {

	public static <T> EnumerationIterator<T> iterator(Enumeration<T> e) {
		return new EnumerationIterator<T>(e);
	}

	final Enumeration<T>	enumerator;
	volatile boolean		done	= false;

	public EnumerationIterator(Enumeration<T> e) {
		enumerator = e;
	}

	public synchronized Iterator<T> iterator() {
		if (done)
			throw new IllegalStateException("Can only be used once");
		done = true;
		return this;

	}

	public boolean hasNext() {
		return enumerator.hasMoreElements();
	}

	public T next() {
		return enumerator.nextElement();
	}

	public void remove() {
		throw new UnsupportedOperationException("Does not support removes");
	}
}
