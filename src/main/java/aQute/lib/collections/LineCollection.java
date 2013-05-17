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

import java.io.*;
import java.util.*;

public class LineCollection implements Iterator<String>, Closeable {
	final BufferedReader	reader;
	String					next;

	public LineCollection(InputStream in) throws IOException {
		this(new InputStreamReader(in, "UTF8"));
	}

	public LineCollection(File in) throws IOException {
		this(new InputStreamReader(new FileInputStream(in), "UTF-8"));
	}

	public LineCollection(Reader reader) throws IOException {
		this(new BufferedReader(reader));
	}

	public LineCollection(BufferedReader reader) throws IOException {
		this.reader = reader;
		next = reader.readLine();
	}

	public boolean hasNext() {
		return next != null;
	}

	public String next() {
		if (next == null)
			throw new IllegalStateException("Iterator has finished");
		try {
			String result = next;
			next = reader.readLine();
			if (next == null)
				reader.close();
			return result;
		}
		catch (Exception e) {
			// ignore
			return null;
		}
	}

	public void remove() {
		if (next == null)
			throw new UnsupportedOperationException("Cannot remove");
	}

	public void close() throws IOException {
		reader.close();
	}
}
