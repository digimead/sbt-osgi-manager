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

package aQute.bnd.deployer.repository.providers;

import java.util.*;
import java.util.regex.*;

public class AttributeType {

	public static final AttributeType STRING      = new AttributeType(false, ScalarType.String);
	public static final AttributeType STRINGLIST  = new AttributeType(true, ScalarType.String);
	public static final AttributeType LONG        = new AttributeType(false, ScalarType.Long);
	public static final AttributeType LONGLIST    = new AttributeType(true, ScalarType.Long);
	public static final AttributeType DOUBLE      = new AttributeType(false, ScalarType.Double);
	public static final AttributeType DOUBLELIST  = new AttributeType(true, ScalarType.Double);
	public static final AttributeType VERSION      = new AttributeType(false, ScalarType.Version);
	public static final AttributeType VERSIONLIST = new AttributeType(true, ScalarType.Version);

	public static final AttributeType DEFAULT = STRING;

	private static final Pattern LIST_TYPE_PATTERN = Pattern.compile("List<(\\w*)>");

	private final boolean list;
	private final ScalarType baseType;

	public static AttributeType parseTypeName(String typeName) throws IllegalArgumentException {
		if (typeName == null)
			return DEFAULT;

		Matcher matcher = LIST_TYPE_PATTERN.matcher(typeName);
		if (matcher.matches()) {
			String scalarTypeName = matcher.group(1);
			ScalarType scalarType = ScalarType.valueOf(scalarTypeName);
			return new AttributeType(true, scalarType);
		}

		ScalarType scalarType = ScalarType.valueOf(typeName.trim());
		return new AttributeType(false, scalarType);
	}

	public AttributeType(boolean list, ScalarType baseType) {
		this.list = list;
		this.baseType = baseType;
	}

	public boolean isList() {
		return list;
	}

	public ScalarType getBaseType() {
		return baseType;
	}

	public Object parseString(String input) {
		if (list) {
			LinkedList<Object> list = new LinkedList<Object>();
			StringTokenizer tokenizer = new StringTokenizer(input, ",");
			while (tokenizer.hasMoreTokens())
				list.add(baseType.parseString(tokenizer.nextToken()));
			return list;
		}

		return baseType.parseString(input);
	}

	@Override
	public String toString() {
		String output;
		if (list) {
			output = "List<" + baseType.toString() + ">";
		} else {
			output = baseType.toString();
		}
		return output;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((baseType == null) ? 0 : baseType.hashCode());
		result = prime * result + (list ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AttributeType other = (AttributeType) obj;
		if (baseType != other.baseType)
			return false;
		if (list != other.list)
			return false;
		return true;
	}

}