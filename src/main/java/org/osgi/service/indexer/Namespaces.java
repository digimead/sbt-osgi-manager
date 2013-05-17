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

public final class Namespaces {

	// Basic namespaces
	public static final String NS_IDENTITY = "osgi.identity";
	public static final String NS_CONTENT = "osgi.content";

	// Wiring namespaces
	public static final String NS_WIRING_PACKAGE = "osgi.wiring.package";
	public static final String NS_WIRING_BUNDLE = "osgi.wiring.bundle";
	public static final String NS_WIRING_HOST = "osgi.wiring.host";
	public static final String NS_EE = "osgi.ee";

	// Non-core namespaces
	public static final String NS_EXTENDER = "osgi.extender";
	public static final String NS_SERVICE = "osgi.service";
	public static final String NS_CONTRACT = "osgi.contract";
	public static final String NS_NATIVE = "osgi.native";

	// Generic attributes
	public static final String ATTR_VERSION = "version";

	// Identity attributes
	public static final String ATTR_IDENTITY_TYPE = "type";

	// Resource types
	public static final String RESOURCE_TYPE_BUNDLE = "osgi.bundle";
	public static final String RESOURCE_TYPE_FRAGMENT = "osgi.fragment";
	public static final String RESOURCE_TYPE_PLAIN_JAR = "jarfile";

	// Content attributes
	public static final String ATTR_CONTENT_URL = "url";
	public static final String ATTR_CONTENT_SIZE = "size";
	public static final String ATTR_CONTENT_MIME = "mime";

	// Package export attributes
	public static final String ATTR_BUNDLE_SYMBOLIC_NAME = "bundle-symbolic-name";
	public static final String ATTR_BUNDLE_VERSION = "bundle-version";

	// Native Attributes
	public static final String ATTR_NATIVE_OSNAME = NS_NATIVE + ".osname";
	public static final String ATTR_NATIVE_OSVERSION = NS_NATIVE + ".osversion";
	public static final String ATTR_NATIVE_PROCESSOR = NS_NATIVE + ".processor";
	public static final String ATTR_NATIVE_LANGUAGE = NS_NATIVE + ".language";

	// Common directives
	public static final String DIRECTIVE_FILTER = "filter";
	public static final String DIRECTIVE_SINGLETON = "singleton";
	public static final String DIRECTIVE_EFFECTIVE = "effective";
	public static final String DIRECTIVE_MANDATORY = "mandatory";
	public static final String DIRECTIVE_USES = "uses";
	public static final String DIRECTIVE_RESOLUTION = "resolution";

	public static final String RESOLUTION_OPTIONAL = "optional";

	public static final String EFFECTIVE_RESOLVE = "resolve";
	public static final String EFFECTIVE_ACTIVE = "active";

	// Known contracts and extenders
	public static final String CONTRACT_OSGI_FRAMEWORK = "OSGiFramework";
	public static final String EXTENDER_SCR = "osgi.ds";
	public static final String EXTENDER_BLUEPRINT = "osgi.blueprint";

}
