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

package org.osgi.service.indexer.impl;

import static org.osgi.framework.FrameworkUtil.createFilter;

import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.indexer.Capability;
import org.osgi.service.indexer.Requirement;
import org.osgi.service.indexer.ResourceAnalyzer;
import org.osgi.service.indexer.ResourceIndexer;
import org.osgi.service.indexer.impl.types.TypedAttribute;
import org.osgi.service.indexer.impl.util.AddOnlyList;
import org.osgi.service.indexer.impl.util.Indent;
import org.osgi.service.indexer.impl.util.Pair;
import org.osgi.service.indexer.impl.util.Tag;
import org.osgi.service.log.LogService;

/**
 * The repository indexer. See OSGi Enterprise Specification 5.0.0, chapter 132.
 */
public class RepoIndex implements ResourceIndexer {
	/**
	 * Name of the configuration variable for the increment (if not set then
	 * System.currentTimeMillis() is used)
	 */
	public static final String REPOSITORY_INCREMENT_OVERRIDE = "-repository.increment.override";

	/** the generic bundle analyzer */
	private final BundleAnalyzer bundleAnalyzer;

	/** the OSGi Framework analyzer */
	private final OSGiFrameworkAnalyzer frameworkAnalyzer;

	/** the Declarative Services analyzer */
	private final SCRAnalyzer scrAnalyzer;

	/** the BluePrint analyzer */
	private final BlueprintAnalyzer blueprintAnalyzer;

	/** the logger */
	private final LogService log;

	/**
	 * the list of analyzer/filter pairs. The filter determines which resources
	 * can be analyzed
	 */
	private final List<Pair<ResourceAnalyzer, Filter>> analyzers = new LinkedList<Pair<ResourceAnalyzer, Filter>>();

	/**
	 * Construct a default instance that uses a console logger.
	 */
	public RepoIndex() {
		this(new ConsoleLogSvc());
	}

	/**
	 * Constructor
	 *
	 * @param log
	 *            the log service to use
	 */
	public RepoIndex(LogService log) {
		this.log = log;

		this.bundleAnalyzer = new BundleAnalyzer(log);
		this.frameworkAnalyzer = new OSGiFrameworkAnalyzer(log);
		this.scrAnalyzer = new SCRAnalyzer(log);
		this.blueprintAnalyzer = new BlueprintAnalyzer(log);

		try {
			Filter allFilter = createFilter("(name=*.jar)");

			addAnalyzer(bundleAnalyzer, allFilter);
			addAnalyzer(frameworkAnalyzer, allFilter);
			addAnalyzer(scrAnalyzer, allFilter);
			addAnalyzer(blueprintAnalyzer, allFilter);
		} catch (InvalidSyntaxException e) {
			throw new ExceptionInInitializerError("Unexpected internal error compiling filter");
		}
	}

	/**
	 * @param analyzer
	 *            the analyzer to add
	 * @param filter
	 *            the filter that determines which resources can be analyzed
	 */
	public final void addAnalyzer(ResourceAnalyzer analyzer, Filter filter) {
		synchronized (analyzers) {
			analyzers.add(Pair.create(analyzer, filter));
		}
	}

	/**
	 * @param analyzer
	 *            the analyzer to add
	 * @param filter
	 *            the filter that determines which resources can be analyzed
	 */
	public final void removeAnalyzer(ResourceAnalyzer analyzer, Filter filter) {
		synchronized (analyzers) {
			analyzers.remove(Pair.create(analyzer, filter));
		}
	}

	/*
	 * See ResourceIndexer interface
	 */
	public void index(Set<File> files, OutputStream out, Map<String, String> config) throws Exception {
		if (config == null)
			config = new HashMap<String, String>(0);

		Set<File> filesToIndex = new TreeSet<File>();
		if (files != null && !files.isEmpty()) {
			resolveDirectories(files, filesToIndex);
		}

		Indent indent;
		PrintWriter pw = null;
		try {
			String prettySetting = config.get(ResourceIndexer.PRETTY);
			String compressedSetting = config.get(ResourceIndexer.COMPRESSED);
			/**
			 * <pre>
			 * pretty   compressed         out-pretty     out-compressed
			 *   null         null        Indent.NONE               true*
			 *   null        false        Indent.NONE              false
			 *   null         true        Indent.NONE               true
			 *  false         null      Indent.PRETTY              false*
			 *  false        false        Indent.NONE              false
			 *  false         true        Indent.NONE               true
			 *   true         null      Indent.PRETTY              false*
			 *   true        false      Indent.PRETTY              false
			 *   true         true      Indent.PRETTY               true
			 *
			 *   * = original behaviour, before compressed was introduced
			 * </pre>
			 */
			indent = (prettySetting == null || (!Boolean.parseBoolean(prettySetting) && compressedSetting != null)) ? Indent.NONE : Indent.PRETTY;
			boolean compressed = (prettySetting == null && compressedSetting == null) || Boolean.parseBoolean(compressedSetting);
			if (!compressed) {
				pw = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
			} else {
				pw = new PrintWriter(new GZIPOutputStream(out, Deflater.BEST_COMPRESSION));
			}

			pw.print(Schema.XML_PROCESSING_INSTRUCTION);
			Tag repoTag = new Tag(Schema.ELEM_REPOSITORY);

			String repoName = config.get(REPOSITORY_NAME);
			if (repoName == null)
				repoName = REPOSITORYNAME_DEFAULT;
			repoTag.addAttribute(Schema.ATTR_NAME, repoName);

			String increment = config.get(REPOSITORY_INCREMENT_OVERRIDE);
			if (increment == null)
				increment = Long.toString(System.currentTimeMillis());
			repoTag.addAttribute(Schema.ATTR_INCREMENT, increment);

			repoTag.addAttribute(Schema.ATTR_XML_NAMESPACE, Schema.NAMESPACE);

			repoTag.printOpen(indent, pw, false);
			for (File file : filesToIndex) {
				try {
					Tag resourceTag = generateResource(file, config);
					resourceTag.print(indent.next(), pw);
				} catch (Exception e) {
					log(LogService.LOG_WARNING, MessageFormat.format("Could not index {0}, skipped ({1}).", file, e.getMessage()), null);
				}
			}
			repoTag.printClose(indent, pw);
		} finally {
			if (pw != null) {
				pw.flush();
				pw.close();
			}
		}
	}

	private void resolveDirectories(Set<File> files, Set<File> filesToIndex) {
		for (File file : files) {
			if (!file.isDirectory()) {
				filesToIndex.add(file);
			} else {
				File[] dirFiles = file.listFiles();
				if (dirFiles.length > 0) {
					Set<File> dirFilesSet = new LinkedHashSet<File>(Arrays.asList(dirFiles));
					resolveDirectories(dirFilesSet, filesToIndex);
				}
			}
		}
	}

	public void indexFragment(Set<File> files, Writer out, Map<String, String> config) throws Exception {
		PrintWriter pw;
		if (out instanceof PrintWriter)
			pw = (PrintWriter) out;
		else
			pw = new PrintWriter(out);

		for (File file : files) {
			try {
				Tag resourceTag = generateResource(file, config);
				resourceTag.print(Indent.PRETTY, pw);
			} catch (Exception e) {
				log(LogService.LOG_WARNING, MessageFormat.format("Could not index {0}, skipped ({1}).", file, e.getMessage()), null);
			}
		}
	}

	private Tag generateResource(File file, Map<String, String> config) throws Exception {

		JarResource resource = new JarResource(file);
		List<Capability> caps = new AddOnlyList<Capability>(new LinkedList<Capability>());
		List<Requirement> reqs = new AddOnlyList<Requirement>(new LinkedList<Requirement>());

		try {
			// Read config settings and save in thread local state
			if (config != null) {
				URL rootURL;
				String rootURLStr = config.get(ResourceIndexer.ROOT_URL);
				if (rootURLStr != null) {
					File rootDir = new File(rootURLStr);
					if (rootDir.isDirectory())
						rootURL = rootDir.toURI().toURL();
					else
						rootURL = new URL(rootURLStr);
				} else
					rootURL = new File(System.getProperty("user.dir")).toURI().toURL();

				String urlTemplate = config.get(ResourceIndexer.URL_TEMPLATE);
				bundleAnalyzer.setStateLocal(new GeneratorState(rootURL, urlTemplate));
			} else {
				bundleAnalyzer.setStateLocal(null);
			}

			// Iterate over the analyzers
			try {
				synchronized (analyzers) {
					for (Pair<ResourceAnalyzer, Filter> entry : analyzers) {
						ResourceAnalyzer analyzer = entry.getFirst();
						Filter filter = entry.getSecond();

						if (filter == null || filter.match(resource.getProperties())) {
							try {
								analyzer.analyzeResource(resource, caps, reqs);
							} catch (Exception e) {
								log(LogService.LOG_ERROR,
										MessageFormat.format("Error calling analyzer \"{0}\" on resource {1}.", analyzer.getClass().getName(), resource.getLocation()), e);
							}
						}
					}
				}
			} finally {
				bundleAnalyzer.setStateLocal(null);
			}
		} finally {
			resource.close();
		}

		Tag resourceTag = new Tag(Schema.ELEM_RESOURCE);
		for (Capability cap : caps) {
			Tag capTag = new Tag(Schema.ELEM_CAPABILITY);
			capTag.addAttribute(Schema.ATTR_NAMESPACE, cap.getNamespace());

			appendAttributeAndDirectiveTags(capTag, cap.getAttributes(), cap.getDirectives());

			resourceTag.addContent(capTag);
		}

		for (Requirement req : reqs) {
			Tag reqTag = new Tag(Schema.ELEM_REQUIREMENT);
			reqTag.addAttribute(Schema.ATTR_NAMESPACE, req.getNamespace());

			appendAttributeAndDirectiveTags(reqTag, req.getAttributes(), req.getDirectives());

			resourceTag.addContent(reqTag);
		}

		return resourceTag;
	}

	private void log(int level, String message, Throwable t) {
		if (log != null)
			log.log(level, message, t);
	}

	private static void appendAttributeAndDirectiveTags(Tag parentTag, Map<String, Object> attribs, Map<String, String> directives) {
		for (Entry<String, Object> attribEntry : attribs.entrySet()) {
			Tag attribTag = new Tag(Schema.ELEM_ATTRIBUTE);
			attribTag.addAttribute(Schema.ATTR_NAME, attribEntry.getKey());

			TypedAttribute typedAttrib = TypedAttribute.create(attribEntry.getKey(), attribEntry.getValue());
			parentTag.addContent(typedAttrib.toXML());
		}

		for (Entry<String, String> directiveEntry : directives.entrySet()) {
			Tag directiveTag = new Tag(Schema.ELEM_DIRECTIVE);
			directiveTag.addAttribute(Schema.ATTR_NAME, directiveEntry.getKey());
			directiveTag.addAttribute(Schema.ATTR_VALUE, directiveEntry.getValue());
			parentTag.addContent(directiveTag);
		}
	}

	/**
	 * Get the current analyzers
	 */

	public List<ResourceAnalyzer> getAnalyzers() {
		List<ResourceAnalyzer> list = new ArrayList<ResourceAnalyzer>();
		for (Pair<ResourceAnalyzer, Filter> entry : analyzers) {
			list.add(entry.getFirst());
		}
		return list;

	}

	/*
	 * Index a file and return a resource for it.
	 */

	public IndexResult indexFile(File file) throws Exception {
		IndexResult result = new IndexResult();
		result.resource = new JarResource(file);
		result.signature = getSignature();
		synchronized (analyzers) {
			for (Pair<ResourceAnalyzer, Filter> entry : analyzers) {
				ResourceAnalyzer analyzer = entry.getFirst();
				Filter filter = entry.getSecond();

				if (filter == null || filter.match(result.resource.getProperties())) {
					analyzer.analyzeResource(result.resource, result.capabilities, result.requirements);
				}
			}
		}
		return result;
	}

	private long getSignature() {
		long value = 97;
		for (Pair<ResourceAnalyzer, Filter> ra : analyzers) {
			value *= 997 * ra.getFirst().getClass().getName().hashCode() + 13;
		}
		return value;
	}
}
