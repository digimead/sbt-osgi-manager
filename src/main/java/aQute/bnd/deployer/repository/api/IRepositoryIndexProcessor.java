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

package aQute.bnd.deployer.repository.api;

import java.net.*;

import org.osgi.resource.*;

public interface IRepositoryIndexProcessor {

	/**
	 * Process an OBR resource descriptor from the index document, and possibly
	 * request early termination of the parser.
	 *
	 * @param resource
	 *            The resource to be processed. The content URI of the resource
	 *            must be a resolved, absolute URI.
	 */
	void processResource(Resource resource);

	/**
	 * Process an OBR referral
	 *
	 * @param parentUri
	 *            The URI of the Repository that referred to this Referral
	 * @param referral
	 *            The referral to be processed
	 * @param maxDepth
	 *            The depth of referrals this repository acknowledges.
	 * @param currentDepth
	 *            The current depth
	 */
	void processReferral(URI parentUri, Referral referral, int maxDepth, int currentDepth);

}
