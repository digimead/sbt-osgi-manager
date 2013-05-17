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

import org.osgi.service.indexer.impl.Schema;
import org.osgi.service.indexer.impl.util.Tag;

public class TypedAttribute {

	private final String name;
	private final Type type;
	private final Object value;

	public TypedAttribute(String name, Type type, Object value) {
		this.name = name;
		this.type = type;
		this.value = value;
	}

	public static TypedAttribute create(String name, Object value) {
		return new TypedAttribute(name, Type.typeOf(value), value);
	}

	public Tag toXML() {
		Tag tag = new Tag(Schema.ELEM_ATTRIBUTE);
		tag.addAttribute(Schema.ATTR_NAME, name);

		if (type.isList() || type.getType() != ScalarType.String) {
			tag.addAttribute(Schema.ATTR_TYPE, type.toString());
		}

		tag.addAttribute(Schema.ATTR_VALUE, type.convertToString(value));

		return tag;
	}
}
