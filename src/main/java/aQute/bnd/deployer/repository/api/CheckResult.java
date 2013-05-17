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

import static aQute.bnd.deployer.repository.api.Decision.*;

public class CheckResult {

	private Decision	decision;
	private String		message;
	private Throwable	exception;

	public static CheckResult fromBool(boolean match, String matchMsg, String unmatchedMsg, Throwable exception) {
		return new CheckResult(match ? accept : reject, match ? matchMsg : unmatchedMsg, exception);
	}

	public CheckResult(Decision decision, String message, Throwable exception) {
		assert decision != null;
		this.decision = decision;
		this.message = message;
		this.exception = exception;
	}

	public Decision getDecision() {
		return decision;
	}

	public void setDecision(Decision decision) {
		this.decision = decision;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Throwable getException() {
		return exception;
	}

	public void setException(Throwable exception) {
		this.exception = exception;
	}

	@Override
	public String toString() {
		return "CheckResult [decision=" + decision + ", message=" + message + ", exception=" + exception + "]";
	}

}
