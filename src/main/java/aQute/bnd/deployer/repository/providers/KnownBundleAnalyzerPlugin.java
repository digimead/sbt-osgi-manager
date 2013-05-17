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

import java.io.*;
import java.util.*;

import org.osgi.service.indexer.*;
import org.osgi.service.indexer.impl.*;

import aQute.bnd.service.*;
import aQute.service.reporter.*;

public class KnownBundleAnalyzerPlugin extends KnownBundleAnalyzer implements ResourceAnalyzer, Plugin {

	private static final String	PROP_DATA	= "data";

	private Reporter	reporter;

	public KnownBundleAnalyzerPlugin() {
		super(new Properties());
	}

	public void setProperties(Map<String,String> config) {
		String fileName = config.get(PROP_DATA);
		if (fileName == null)
			throw new IllegalArgumentException(String.format("Property name '%s' must be set on KnownBundleAnalyzerPlugin", PROP_DATA));
		File file = new File(fileName);
		if (!file.isFile())
			throw new IllegalArgumentException(String.format("Data file does not exist, or is not a plain file: %s", file));

		FileInputStream stream = null;
		try {
			stream = new FileInputStream(file);
			Properties props = new Properties();
			props.load(stream);
			setKnownBundlesExtra(props);
		} catch (IOException e) {
			throw new IllegalArgumentException(String.format("Unable to read data file: %s", file), e);
		} finally {
			try { if (stream != null) stream.close(); } catch (IOException e) {}
		}
	}

	public void setReporter(Reporter reporter) {
		this.reporter = reporter;
	}

}
