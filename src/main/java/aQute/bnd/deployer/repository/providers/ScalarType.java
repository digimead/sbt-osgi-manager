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

public enum ScalarType {
	String, Version, Long, Double;

	public Object parseString(String input) {
		Object result;

		switch (this) {
			case String :
				result = input;
				break;
			case Long :
				result = java.lang.Long.parseLong(input.trim());
				break;
			case Double :
				result = java.lang.Double.parseDouble(input.trim());
				break;
			case Version :
				result = org.osgi.framework.Version.parseVersion(input.trim());
				break;
			default :
				throw new IllegalArgumentException("Cannot parse input for unknown attribute type '" + name() + "'");
		}

		return result;
	}
}