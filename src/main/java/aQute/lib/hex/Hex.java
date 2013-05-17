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

package aQute.lib.hex;

import java.io.*;

/*
 * Hex converter.
 *
 * TODO Implement string to byte[]
 */
public class Hex {
	final static char[]	HEX	= {
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
							};

	public final static byte[] toByteArray(String string) {
		string = string.trim();
		if ((string.length() & 1) != 0)
			throw new IllegalArgumentException("a hex string must have an even length");

		byte[] out = new byte[string.length() / 2];
		for (int i = 0; i < out.length; i++) {
			int high = nibble(string.charAt(i * 2)) << 4;
			int low = nibble(string.charAt(i * 2 + 1));
			out[i] = (byte) (high + low);
		}
		return out;
	}

	public final static int nibble(char c) {
		if (c >= '0' && c <= '9')
			return c - '0';

		if (c >= 'A' && c <= 'F')
			return c - 'A' + 10;
		if (c >= 'a' && c <= 'f')
			return c - 'a' + 10;

		throw new IllegalArgumentException("Not a hex digit: " + c);
	}

	public final static String toHexString(byte data[]) {
		StringBuilder sb = new StringBuilder();
		try {
			append(sb, data);
		}
		catch (IOException e) {
			// cannot happen with sb
		}
		return sb.toString();
	}

	public final static void append(Appendable sb, byte[] data) throws IOException {
		for (int i = 0; i < data.length; i++) {
			sb.append(nibble(data[i] >> 4));
			sb.append(nibble(data[i]));
		}
	}

	private final static char nibble(int i) {
		return HEX[i & 0xF];
	}
}
