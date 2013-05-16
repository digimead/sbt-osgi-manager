/**
 * Copy of bnd/biz.aQute.resolve. Reason: there are no Maven artifacts for bootstrap.
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

package biz.aQute.resolve.internal;

import java.util.*;

import org.osgi.framework.*;
import org.osgi.resource.*;


public class CapabilityIndex {

	private final Map<String,List<Capability>>	capabilityMap	= new HashMap<String,List<Capability>>();

	public void clear() {
		capabilityMap.clear();
	}

	public void addResource(Resource resource) {
		List<Capability> capabilities = resource.getCapabilities(null);
		if (capabilities == null)
			return;

		for (Capability cap : capabilities) {
			addCapability(cap);
		}
	}

	public void addCapability(Capability cap) {
		List<Capability> list = capabilityMap.get(cap.getNamespace());
		if (list == null) {
			list = new LinkedList<Capability>();
			capabilityMap.put(cap.getNamespace(), list);
		}
		list.add(cap);
	}

	public void appendMatchingCapabilities(Requirement requirement, Collection< ? super Capability> capabilities) {
		List<Capability> caps = capabilityMap.get(requirement.getNamespace());
		if (caps == null || caps.isEmpty())
			return;

		try {
			String filterStr = requirement.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
			Filter filter = filterStr != null ? FrameworkUtil.createFilter(filterStr) : null;

			for (Capability cap : caps) {
				boolean match;
				if (filter == null)
					match = true;
				else
					match = filter.match(new MapToDictionaryAdapter(cap.getAttributes()));

				if (match)
					capabilities.add(cap);
			}
		}
		catch (InvalidSyntaxException e) {
			// Assume no matches
		}
	}

}
