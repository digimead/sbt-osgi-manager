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

import java.util.LinkedHashMap;
import java.util.Map;

public class Builder {

	private String namespace = null;
	private final Map<String, Object> attributes = new LinkedHashMap<String, Object>();
	private final Map<String, String> directives = new LinkedHashMap<String, String>();

	public Builder setNamespace(String namespace) {
		this.namespace = namespace;
		return this;
	}

	public Builder addAttribute(String name, Object value) {
		attributes.put(name, value);
		return this;
	}

	public Builder addDirective(String name, String value) {
		directives.put(name, value);
		return this;
	}

	public Capability buildCapability() throws IllegalStateException {
		if (namespace == null)
			throw new IllegalStateException("Namespace not set");

		return new Capability(namespace, new LinkedHashMap<String, Object>(attributes), new LinkedHashMap<String, String>(directives));
	}

	public Requirement buildRequirement() throws IllegalStateException {
		if (namespace == null)
			throw new IllegalStateException("Namespace not set");

		return new Requirement(namespace, new LinkedHashMap<String, Object>(attributes), new LinkedHashMap<String, String>(directives));
	}

}
