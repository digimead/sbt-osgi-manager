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

package aQute.bnd.osgi.resource;

import java.util.*;

import org.osgi.framework.namespace.*;
import org.osgi.resource.*;

class ResourceImpl implements Resource {

	private List<Capability>				allCapabilities;
	private Map<String,List<Capability>>	capabilityMap;

	private List<Requirement>				allRequirements;
	private Map<String,List<Requirement>>	requirementMap;

	void setCapabilities(List<Capability> capabilities) {
		allCapabilities = capabilities;

		capabilityMap = new HashMap<String,List<Capability>>();
		for (Capability capability : capabilities) {
			List<Capability> list = capabilityMap.get(capability.getNamespace());
			if (list == null) {
				list = new LinkedList<Capability>();
				capabilityMap.put(capability.getNamespace(), list);
			}
			list.add(capability);
		}
	}

	public List<Capability> getCapabilities(String namespace) {
		return namespace == null ? allCapabilities : capabilityMap.get(namespace);
	}

	void setRequirements(List<Requirement> requirements) {
		allRequirements = requirements;

		requirementMap = new HashMap<String,List<Requirement>>();
		for (Requirement requirement : requirements) {
			List<Requirement> list = requirementMap.get(requirement.getNamespace());
			if (list == null) {
				list = new LinkedList<Requirement>();
				requirementMap.put(requirement.getNamespace(), list);
			}
			list.add(requirement);
		}
	}

	public List<Requirement> getRequirements(String namespace) {
		return namespace == null ? allRequirements : requirementMap.get(namespace);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		List<Capability> identities = getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
		if (identities != null && identities.size() == 1) {
			Capability idCap = identities.get(0);
			Object id = idCap.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE);
			Object version = idCap.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);

			builder.append(id).append(" ver=").append(version);
		} else {
			// Generic toString
			builder.append("ResourceImpl [caps=");
			builder.append(allCapabilities);
			builder.append(", reqs=");
			builder.append(allRequirements);
			builder.append("]");
		}
		return builder.toString();
	}

}
