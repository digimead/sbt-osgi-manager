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

package org.osgi.service.indexer.impl.types;

import org.osgi.framework.Constants;
import org.osgi.service.indexer.Namespaces;

public enum VersionKey {

	PackageVersion(Constants.VERSION_ATTRIBUTE), BundleVersion(Constants.BUNDLE_VERSION_ATTRIBUTE), NativeOsVersion(Namespaces.ATTR_NATIVE_OSVERSION);

	private String key;

	VersionKey(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}
}
