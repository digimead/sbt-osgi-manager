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

package aQute.bnd.deployer.repository;

import java.io.*;

/**
 * This is used to prevent content providers with interfering with the state of
 * the underlying stream by calling ant of {@link InputStream#close()},
 * {@link InputStream#mark(int)} or {@link InputStream#reset()}.
 *
 * @author Neil Bartlett
 */
class ProtectedStream extends InputStream {

	private InputStream	delegate;

	ProtectedStream(InputStream delegate) {
		this.delegate = delegate;
	}

	public int available() throws IOException {
		return delegate.available();
	}

	public void close() throws IOException {
		// ignore!
	}

	public void mark(int limit) {
		throw new UnsupportedOperationException("mark is not supported");
	}

	public boolean markSupported() {
		return false;
	}

	public int read() throws IOException {
		return delegate.read();
	}

	public int read(byte[] buf) throws IOException {
		return delegate.read(buf);
	}

	public int read(byte[] buf, int start, int len) throws IOException {
		return delegate.read(buf, start, len);
	}

	public void reset() throws IOException {
		throw new IOException("Reset not allowed");
	}

	public long skip(long bytes) throws IOException {
		return delegate.skip(bytes);
	}

}
