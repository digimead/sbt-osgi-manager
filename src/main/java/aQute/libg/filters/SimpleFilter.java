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

public final class SimpleFilter extends Filter {

	private final String	name;
	private final Operator	operator;
	private final String	value;

	/**
	 * Construct a simple filter with the default "equals" operator, i.e.
	 * {@code (name=value)}.
	 */
	public SimpleFilter(String name, String value) {
		this(name, Operator.Equals, value);
	}

	/**
	 * Construct a simple filter with any of the comparison operators.
	 */
	public SimpleFilter(String name, Operator operator, String value) {
		this.name = name;
		this.operator = operator;
		this.value = value;
	}

	@Override
	public void append(StringBuilder builder) {
		builder.append('(');
		builder.append(name).append(operator.getSymbol()).append(value);
		builder.append(')');
	}

}
