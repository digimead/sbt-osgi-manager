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

public final class Schema {

	public static final Object XML_PROCESSING_INSTRUCTION = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
	public static final String NAMESPACE = "http://www.osgi.org/xmlns/repository/v1.0.0";

	public static final String ELEM_REPOSITORY = "repository";
	public static final String ELEM_RESOURCE = "resource";
	public static final String ELEM_CAPABILITY = "capability";
	public static final String ELEM_REQUIREMENT = "requirement";
	public static final String ELEM_ATTRIBUTE = "attribute";
	public static final String ELEM_DIRECTIVE = "directive";

	public static final String ATTR_XML_NAMESPACE = "xmlns";
	public static final String ATTR_NAME = "name";
	public static final String ATTR_INCREMENT = "increment";
	public static final String ATTR_NAMESPACE = "namespace";
	public static final String ATTR_TYPE = "type";
	public static final String ATTR_VALUE = "value";

	public static final String TYPE_VERSION = "Version";

}
