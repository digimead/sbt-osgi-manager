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

import java.util.List;

import org.osgi.service.indexer.Builder;
import org.osgi.service.indexer.Capability;
import org.osgi.service.indexer.Namespaces;
import org.osgi.service.indexer.Requirement;
import org.osgi.service.indexer.Resource;
import org.osgi.service.indexer.ResourceAnalyzer;
import org.osgi.service.log.LogService;

public class BlueprintAnalyzer implements ResourceAnalyzer {

	private static final String BUNDLE_BLUEPRINT_HEADER = "Bundle-Blueprint";

	@SuppressWarnings("unused")
	private LogService log;

	public BlueprintAnalyzer(LogService log) {
		this.log = log;
	}

	public void analyzeResource(Resource resource, List<Capability> capabilities, List<Requirement> requirements) throws Exception {
		boolean blueprintEnabled = false;

		String header = resource.getManifest().getMainAttributes().getValue(BUNDLE_BLUEPRINT_HEADER);
		if (header != null) {
			blueprintEnabled = true;
		} else {
			List<String> children = resource.listChildren("OSGI-INF/blueprint/");
			if (children != null) {
				for (String child : children) {
					if (child.toLowerCase().endsWith(".xml")) {
						blueprintEnabled = true;
						break;
					}
				}
			}
		}

		if (blueprintEnabled)
			requirements.add(createRequirement());
	}

	private Requirement createRequirement() {
		Builder builder = new Builder().setNamespace(Namespaces.NS_EXTENDER);
		String filter = String.format("(&(%s=%s)(version>=1.0.0)(!(version>=2.0.0)))", Namespaces.NS_EXTENDER, Namespaces.EXTENDER_BLUEPRINT);
		builder.addDirective(Namespaces.DIRECTIVE_FILTER, filter).addDirective(Namespaces.DIRECTIVE_EFFECTIVE, Namespaces.EFFECTIVE_ACTIVE);
		return builder.buildRequirement();
	}

}
