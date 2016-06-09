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

package org.osgi.service.indexer;

import java.util.Collections;
import java.util.Map;

/**
 * A requirement
 */
public final class Requirement {
	/** the namespace */
	private final String namespace;

	/** the attributes */
	private final Map<String, Object> attributes;

	/** the directives */
	private final Map<String, String> directives;

	/**
	 * Constructor
	 *
	 * @param namespace
	 *            the namespace
	 * @param attributes
	 *            the attributes
	 * @param directives
	 *            the directives
	 */
	Requirement(String namespace, Map<String, Object> attributes, Map<String, String> directives) {
		this.namespace = namespace;
		this.attributes = attributes;
		this.directives = directives;
	}

	/**
	 * @return the namespace
	 */
	public String getNamespace() {
		return namespace;
	}

	/**
	 * @return the attributes
	 */
	public Map<String, Object> getAttributes() {
		return Collections.unmodifiableMap(attributes);
	}

	/**
	 * @return the directives
	 */
	public Map<String, String> getDirectives() {
		return Collections.unmodifiableMap(directives);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Requirement [namespace=").append(namespace).append(", attributes=").append(attributes).append(", directives=").append(directives).append("]");
		return builder.toString();
	}
}
