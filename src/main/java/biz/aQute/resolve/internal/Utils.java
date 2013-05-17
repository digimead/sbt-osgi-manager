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

package biz.aQute.resolve.internal;

import java.util.List;

import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

public class Utils {
    static Version findIdentityVersion(Resource resource) {
        List<Capability> idCaps = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
        if (idCaps == null || idCaps.isEmpty())
            throw new IllegalArgumentException("Resource has no identity capability.");
        if (idCaps.size() > 1)
            throw new IllegalArgumentException("Resource has more than one identity capability.");

        Object versionObj = idCaps.get(0).getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
        if (versionObj == null)
            return Version.emptyVersion;

        if (versionObj instanceof Version)
            return (Version) versionObj;

        if (versionObj instanceof String)
            return Version.parseVersion((String) versionObj);

        throw new IllegalArgumentException("Unable to convert type for version attribute: " + versionObj.getClass());
    }
}
