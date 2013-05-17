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

public class Requirement {

	private final String namespace;
	private final Map<String, Object> attributes;
	private final Map<String, String> directives;

	Requirement(String namespace, Map<String, Object> attributes, Map<String, String> directives) {
		this.namespace = namespace;
		this.attributes = attributes;
		this.directives = directives;
	}

	public String getNamespace() {
		return namespace;
	}

	public Map<String, Object> getAttributes() {
		return Collections.unmodifiableMap(attributes);
	}

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
