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

package aQute.libg.gzip;

import java.io.*;
import java.util.zip.*;

public class GZipUtils {

	/**
	 * Determines whether the specified stream contains gzipped data, by
	 * checking for the GZIP magic number, and returns a stream capable of
	 * reading those data.
	 *
	 * @throws IOException
	 */
	public static InputStream detectCompression(InputStream stream) throws IOException {
		InputStream buffered;
		if (stream.markSupported())
			buffered = stream;
		else
			buffered = new BufferedInputStream(stream);

		buffered.mark(2);
		int magic = readUShort(buffered);
		buffered.reset();

		InputStream result;
		if (magic == GZIPInputStream.GZIP_MAGIC)
			result = new GZIPInputStream(buffered);
		else
			result = buffered;
		return result;
	}

	/*
	 * Reads unsigned short in Intel byte order.
	 */
	private static int readUShort(InputStream in) throws IOException {
		int b = readUByte(in);
		return (readUByte(in) << 8) | b;
	}

	/*
	 * Reads unsigned byte.
	 */
	private static int readUByte(InputStream in) throws IOException {
		int b = in.read();
		if (b == -1) {
			throw new EOFException();
		}
		if (b < -1 || b > 255) {
			// Report on this.in, not argument in; see read{Header, Trailer}.
			throw new IOException(in.getClass().getName() + ".read() returned value out of range -1..255: " + b);
		}
		return b;
	}

}
