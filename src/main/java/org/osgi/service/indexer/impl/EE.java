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

package org.osgi.service.indexer.impl;

import org.osgi.framework.Version;

public class EE {

	private String name;
	private String version;

	private EE() {}

	public static final EE parseBREE(String bree) {
		EE ee = new EE();

		int splitIndex = bree.indexOf("/");
		if (splitIndex >= 0) {
			Segment segment1 = versionSplit(bree.substring(0, splitIndex));
			if (segment1.name.equals("J2SE"))
				segment1.name = "JavaSE";

			Segment segment2 = versionSplit(bree.substring(splitIndex + 1));

			if (segment1.version != null) {
				if (segment1.version.equals(segment2.version)) {
					ee.name = segment1.name + "/" + segment2.name;
					ee.version = segment1.version;
				} else {
					StringBuilder builder = new StringBuilder().append(segment1.name).append('-').append(segment1.version).append('/').append(segment2.name);
					if (segment2.version != null)
						builder.append('-').append(segment2.version);
					ee.name = builder.toString();
					ee.version = null;
				}
			} else {
				ee.name = segment1.name + "/" + segment2.name;
				ee.version = segment2.version;
			}
		} else {
			Segment segment = versionSplit(bree);
			if (segment.name.equals("J2SE"))
				segment.name = "JavaSE";

			ee.name = segment.name;
			ee.version = segment.version;
		}

		return ee;
	}

	private static class Segment {
		private String name;
		private String version;
	}

	private static Segment versionSplit(String input) {
		Segment result = new Segment();
		int index = input.indexOf('-');
		if (index >= 0) {
			String name = input.substring(0, index);
			String versionStr = input.substring(index + 1);

			try {
				new Version(versionStr);
				result.name = name;
				result.version = versionStr;
			} catch (IllegalArgumentException e) {
				result.name = input;
				result.version = null;
			}
		} else {
			result.name = input;
			result.version = null;
		}
		return result;
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	public String toFilter() {
		StringBuilder builder = new StringBuilder();
		builder.append("(osgi.ee=").append(name).append(")");

		if (version != null) {
			builder.insert(0, "(&");
			builder.append("(version=").append(version).append(")");
			builder.append(")");
		}

		return builder.toString();
	}

}
