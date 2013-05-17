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

package aQute.lib.io;

import java.io.*;

public class LimitedInputStream extends InputStream {

	final InputStream	in;
	final int			size;
	int					left;

	public LimitedInputStream(InputStream in, int size) {
		this.in = in;
		this.left = size;
		this.size = size;
	}

	@Override
	public int read() throws IOException {
		if (left <= 0) {
			eof();
			return -1;
		}

		left--;
		return in.read();
	}

	@Override
	public int available() throws IOException {
		return Math.min(left, in.available());
	}

	@Override
	public void close() throws IOException {
		eof();
		in.close();
	}

	protected void eof() {}

	@Override
	public synchronized void mark(int readlimit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean markSupported() {
		return false;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int min = Math.min(len, left);
		if (min == 0)
			return 0;

		int read = in.read(b, off, min);
		if (read > 0)
			left -= read;
		return read;
	}

	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	@Override
	public synchronized void reset() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public long skip(long n) throws IOException {
		long count = 0;
		byte buffer[] = new byte[1024];
		while (n > 0 && read() >= 0) {
			int size = read(buffer);
			if (size <= 0)
				return count;
			count += size;
			n -= size;
		}
		return count;
	}
}
