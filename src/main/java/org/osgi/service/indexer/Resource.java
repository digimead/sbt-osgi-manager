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

import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.List;
import java.util.jar.Manifest;

public interface Resource {

	static String NAME = "name";
	static String LOCATION = "location";
	static String SIZE = "size";
	static String LAST_MODIFIED = "lastmodified";

	String getLocation();

	Dictionary<String, Object> getProperties();

	long getSize();

	InputStream getStream() throws IOException;

	Manifest getManifest() throws IOException;

	List<String> listChildren(String prefix) throws IOException;

	Resource getChild(String path) throws IOException;

	void close();
}
