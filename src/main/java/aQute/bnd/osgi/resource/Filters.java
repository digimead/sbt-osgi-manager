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

import org.osgi.framework.namespace.*;

import aQute.bnd.version.*;
import aQute.libg.filters.*;

public class Filters {

	public static final String DEFAULT_VERSION_ATTR = IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE;

	/**
	 * Generate an LDAP-style version filter from a version range, e.g.
	 * {@code [1.0,2.0)} generates {@code (&(version>=1.0)(!(version>=2.0))}
	 *
	 * @param range
	 * @return The generated filter.
	 * @throws IllegalArgumentException
	 *             If the supplied range is invalid.
	 */
	public static String fromVersionRange(String range) throws IllegalArgumentException {
		return fromVersionRange(range, DEFAULT_VERSION_ATTR);
	}

	/**
	 * Generate an LDAP-style version filter from a version range, using a
	 * specific attribute name for the version; for example can be used to
	 * generate a range using the {@code bundle-version} attribute such as
	 * {@code (&(bundle-version>=1.0)(!(bundle-version>=2.0))}.
	 *
	 * @param range
	 * @param versionAttr
	 * @return The generated filter
	 * @throws IllegalArgumentException
	 *             If the supplied range is invalid.
	 */
	public static String fromVersionRange(String range, String versionAttr) throws IllegalArgumentException {
		if (range == null)
			return null;
		VersionRange parsedRange = new VersionRange(range);

		Filter left;
		if (parsedRange.includeLow())
			left = new SimpleFilter(versionAttr, Operator.GreaterThanOrEqual, parsedRange.getLow().toString());
		else
			left = new NotFilter(new SimpleFilter(versionAttr, Operator.LessThanOrEqual, parsedRange.getLow().toString()));

		Filter right;
		if (!parsedRange.isRange())
			right = null;
		else if (parsedRange.includeHigh())
			right = new SimpleFilter(versionAttr, Operator.LessThanOrEqual, parsedRange.getHigh().toString());
		else
			right = new NotFilter(new SimpleFilter(versionAttr, Operator.GreaterThanOrEqual, parsedRange.getHigh().toString()));

		Filter result;
		if (right != null)
			result = new AndFilter().addChild(left).addChild(right);
		else
			result = left;

		return result.toString();
	}
}
