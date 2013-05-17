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

package aQute.libg.filters;

import java.util.LinkedList;
import java.util.List;

public final class AndFilter extends Filter {

	private final List<Filter> children = new LinkedList<Filter>();

	public AndFilter addChild(Filter child) {
		if (child instanceof AndFilter)
			children.addAll(((AndFilter) child).children);
		else
			children.add(child);
		return this;
	}

	@Override
	public void append(StringBuilder builder) {
		if (children.isEmpty())
			return;

		builder.append("(&");
		for (Filter child : children) {
			child.append(builder);
		}
		builder.append(")");
	}

}
