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

import java.util.List;

import aQute.bnd.annotation.ConsumerType;

/**
 * <p>
 * A resource analyzer is responsible for analyzing a resource for some specific
 * purpose, and discovering requirements and capabilities that may not be
 * discovered by the basic Bundle Analyzer.
 * </p>
 *
 * <p>
 * Clients may implement this interface and register instances as services.
 * </p>
 *
 * @author Neil Bartlett <njbartlett@gmail.com>
 */
@ConsumerType
public interface ResourceAnalyzer {
	/**
	 * The service property used to declare a resource filter, so that the
	 * analyzer is only invoked on a subset of resources. Example:
	 * <code>(&(|(name=foo.jar)(name=*.ear))(lastmodified>=1262217600753))</code>
	 */
	static final String FILTER = "filter";

	/**
	 * <p>
	 * This method is invoked for each resource that the analyzer is requested
	 * to analyze. Implementations add zero or more capabilities and/or
	 * requirements to the supplied lists.
	 * </p>
	 *
	 * <p>
	 * Analyzers <b>may</b> examine the lists of already-discovered requirements
	 * and capabilities; for example they may wish to add a certain capability
	 * if (and only if) it has not already been added.
	 * </p>
	 *
	 * <p>
	 * However, analyzers <b>should not</b> rely on being invoked in any
	 * particular order, i.e. either before or after any other analyzer.
	 * </p>
	 *
	 * <p>
	 * Analyzers <b>MUST NOT</b> attempt to remove or replace any capability or
	 * requirement from the supplied list. Clients of this method may enforce
	 * this by passing List implementations that throw
	 * {@link UnsupportedOperationException} upon any attempt to call
	 * {@link List#remove(int)}, etc.
	 * </p>
	 *
	 *
	 * @param resource
	 *            The current resource.
	 * @param capabilities
	 *            The list of capabilities.
	 * @param requirements
	 *            The list of requirements.
	 * @throws Exception
	 *             If something goes wrong. The error will be logged to the OSGi
	 *             Log Service (if available) and the next ResourceAnalyzer (if
	 *             any) will be asked to analyze the resource.
	 */
	void analyzeResource(Resource resource, List<Capability> capabilities, List<Requirement> requirements) throws Exception;
}
