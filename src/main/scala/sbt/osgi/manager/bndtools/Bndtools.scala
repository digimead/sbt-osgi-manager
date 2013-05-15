/**
 * sbt-osgi-manager - OSGi development bridge based on Bnd and Tycho.
 *
 * Copyright (c) 2013 Alexey Aksenov ezh@ezh.msk.ru
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

package sbt.osgi.manager.bndtools

import scala.ref.WeakReference

import aQute.bnd.build.Workspace
import sbt._
import sbt.Keys._
import sbt.osgi.manager.Keys._

class Bndtools(cnf: File) {
  /** A cnf project containing workspace-wide configuration */
  lazy val workspace: Workspace = { // getWorkspace
    val workspace = Workspace.getWorkspace(cnf.getParentFile())
    // add plugins
    //workspace.addBasicPlugin(new WorkspaceListener(workspace))
    //workspace.addBasicPlugin(Activator.instance.repoListenerTracker)
    //workspace.addBasicPlugin(getWorkspaceR5Repository())

    // Initialize projects in synchronized block
    workspace.getBuildOrder()
    workspace
  }

  /*private static final ILogger logger = Logger.getLogger();

    static WorkspaceR5Repository r5Repository = null;
    static RepositoryPlugin workspaceRepo = null;

    static final AtomicBoolean indexValid = new AtomicBoolean(false);
    static final ConcurrentMap<String,Map<String,SortedSet<Version>>> exportedPackageMap = new ConcurrentHashMap<String,Map<String,SortedSet<Version>>>();
    static final ConcurrentMap<String,Collection<String>> containedPackageMap = new ConcurrentHashMap<String,Collection<String>>();

    final Map<IJavaProject,Project> javaProjectToModel = new HashMap<IJavaProject,Project>();
    final List<ModelListener> listeners = new CopyOnWriteArrayList<ModelListener>();

    Central() {}

    public Project getModel(IJavaProject project) {
        try {
            Project model = javaProjectToModel.get(project);
            if (model == null) {
                File projectDir = project.getProject().getLocation().makeAbsolute().toFile();
                try {
                    model = Workspace.getProject(projectDir);
                } catch (IllegalArgumentException e) {
                    // initialiseWorkspace();
                    // model = Workspace.getProject(projectDir);
                    return null;
                }
                if (workspace == null) {
                    model.getWorkspace();
                }
                if (model != null) {
                    javaProjectToModel.put(project, model);
                }
            }
            return model;
        } catch (Exception e) {
            // TODO do something more useful here
            throw new RuntimeException(e);
        }
    }

    /**
* Implementation of the resource changed interface. We are checking in the POST_CHANGE phase if one of our tracked
* models needs to be updated.
*/
    public synchronized void resourceChanged(IResourceChangeEvent event) {
        if (event.getType() != IResourceChangeEvent.POST_CHANGE)
            return;

        IResourceDelta rootDelta = event.getDelta();
        try {
            final Set<Project> changed = new HashSet<Project>();
            rootDelta.accept(new IResourceDeltaVisitor() {
                public boolean visit(IResourceDelta delta) throws CoreException {
                    try {

                        IPath location = delta.getResource().getLocation();
                        if (location == null) {
                            System.out.println("Cannot convert resource to file: " + delta.getResource());
                        } else {
                            File file = location.toFile();
                            File parent = file.getParentFile();
                            boolean parentIsWorkspace = parent.equals(getWorkspace().getBase());

                            // file
                            // /development/osgi/svn/build/org.osgi.test.cases.distribution/bnd.bnd
                            // parent
                            // /development/osgi/svn/build/org.osgi.test.cases.distribution
                            // workspace /development/amf/workspaces/osgi
                            // false

                            if (parentIsWorkspace) {
                                // We now are on project level, we do not go
                                // deeper
                                // because projects/workspaces should check for
                                // any
                                // changes.
                                // We are careful not to create unnecessary
                                // projects
                                // here.
                                if (file.getName().equals(Workspace.CNFDIR)) {
                                    if (workspace.refresh()) {
                                        changed.addAll(workspace.getCurrentProjects());
                                    }
                                    return false;
                                }
                                if (workspace.isPresent(file.getName())) {
                                    Project project = workspace.getProject(file.getName());
                                    changed.add(project);
                                } else {
                                    // Project not created yet, so we
                                    // have
                                    // no cached results

                                }
                                return false;
                            }
                        }
                        return true;
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new CoreException(new Status(Status.ERROR, Activator.PLUGIN_ID, "During checking project changes", e));
                    }
                }

            });

            for (Project p : changed) {
                p.refresh();
                changed(p);

            }
        } catch (CoreException e) {
            Activator.getDefault().error("While handling changes", e);
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static IFile getWorkspaceBuildFile() throws Exception {
        File file = Central.getWorkspace().getPropertiesFile();
        IFile[] matches = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(file.toURI());

        if (matches == null || matches.length != 1) {
            logger.logError("Cannot find workspace location for bnd configuration file " + file, null);
            return null;
        }

        return matches[0];
    }

    public synchronized static WorkspaceR5Repository getWorkspaceR5Repository() throws Exception {
        if (r5Repository != null)
            return r5Repository;

        r5Repository = new WorkspaceR5Repository();
        r5Repository.init();

        return r5Repository;
    }

    public synchronized static RepositoryPlugin getWorkspaceRepository() throws Exception {
        if (workspaceRepo != null)
            return workspaceRepo;

        workspaceRepo = new WorkspaceRepository(getWorkspace());
        return workspaceRepo;
    }
*/
  /*
    public void changed(Project model) {
        model.setChanged();
        for (ModelListener m : listeners)
            try {
                m.modelChanged(model);
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    public void addModelListener(ModelListener m) {
        if (!listeners.contains(m)) {
            listeners.add(m);
        }
    }

    public void removeModelListener(ModelListener m) {
        listeners.remove(m);
    }

    public static IJavaProject getJavaProject(Project model) {
        for (IProject iproj : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
            if (iproj.getName().equals(model.getName())) {
                IJavaProject ij = JavaCore.create(iproj);
                if (ij != null && ij.exists()) {
                    return ij;
                }
                // current project is not a Java project
            }
        }
        return null;
    }

    public static IPath toPath(File file) throws Exception {
        IPath result = null;

        File absolute = file.getCanonicalFile();

        IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace().getRoot();
        IFile[] candidates = wsroot.findFilesForLocationURI(absolute.toURI());
        if (candidates != null && candidates.length > 0) {
            result = candidates[0].getFullPath();
        } else {
            String workspacePath = getWorkspace().getBase().getAbsolutePath();
            String absolutePath = absolute.getPath();
            if (absolutePath.startsWith(workspacePath))
                result = new Path(absolutePath.substring(workspacePath.length()));
        }

        return result;
    }

    public static void refresh(IPath path) {
        try {
            IResource r = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
            if (r != null)
                return;

            IPath p = (IPath) path.clone();
            while (p.segmentCount() > 0) {
                p = p.removeLastSegments(1);
                IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(p);
                if (resource != null) {
                    resource.refreshLocal(2, null);
                    return;
                }
            }
        } catch (Exception e) {
            Activator.getDefault().error("While refreshing path " + path, e);
        }
    }

    public static void refreshPlugins() throws Exception {
        List<Refreshable> rps = getWorkspace().getPlugins(Refreshable.class);
        for (Refreshable rp : rps) {
            if (rp.refresh()) {
                File dir = rp.getRoot();
                refreshFile(dir);
            }
        }
    }

    public static void refreshFile(File f) throws Exception {
        String path = toLocal(f);
        IResource r = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
        if (r != null) {
            r.refreshLocal(IResource.DEPTH_INFINITE, null);
        }
    }

    public static void refresh(Project p) throws Exception {
        IJavaProject jp = getJavaProject(p);
        if (jp != null)
            jp.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
    }

    private static String toLocal(File f) throws Exception {
        String root = getWorkspace().getBase().getAbsolutePath();
        String path = f.getAbsolutePath().substring(root.length());
        return path;
    }

    public void close() {}

    public static void invalidateIndex() {
        indexValid.set(false);
    }

    public static boolean needsIndexing() {
        return indexValid.compareAndSet(false, true);
    }

    public static Map<String,SortedSet<Version>> getExportedPackageModel(IProject project) {
        String key = project.getFullPath().toPortableString();
        return exportedPackageMap.get(key);
    }

    public static Collection<String> getContainedPackageModel(IProject project) {
        String key = project.getFullPath().toPortableString();
        return containedPackageMap.get(key);
    }

    public static void setProjectPackageModel(IProject project, Map<String,SortedSet<Version>> exports, Collection<String> contained) {
        String key = project.getFullPath().toPortableString();
        exportedPackageMap.put(key, exports);
        containedPackageMap.put(key, contained);
    }
*/
}

object Bndtools {
  @volatile private var cnfProjects = Seq[WeakReference[Project]]()
  lazy val settings = inConfig(OSGiConf)(Seq[sbt.Project.Setting[_]](
    osgiCnfPath <<= (osgiDirectory) { file =>
      val cnf = file / "cnf"
      cnf.mkdirs()
      assert(cnf.isDirectory(), cnf + " is not directory")
      assert(cnf.canWrite(), cnf + " is not writable")
      cnf
    },
    osgiBndtoolsDirectory <<= (osgiDirectory) { _ / "bnd" },
    osgiBndBuildPath := List[String](),
    osgiBndBundleActivator := "",
    osgiBndBundleCategory := List[String](), // http://www.osgi.org/Specifications/Reference#categories
    osgiBndBundleContactAddress := "",
    osgiBndBundleCopyright := "",
    osgiBndBundleDescription <<= description in This,
    osgiBndBundleUpdateLocation := "",
    osgiBndBundleSymbolicName <<= organization in This,
    osgiBndBundleName <<= name in This,
    osgiBndBundleLicense := "",
    osgiBndBundleDocURL :== "",
    osgiBndBundleVendor <<= organizationName in This,
    osgiBndBundleVersion <<= version in This,
    osgiBndClassPath := List[String](),
    osgiBndExportPackage := List[String](),
    osgiBndImportPackage := List[String](),
    osgiBndPlugin := List[String](),
    osgiBndPluginPath := List[String](),
    osgiBndPrivatePackage := List[String](),
    osgiBndRunBundles := List[String](),
    osgiBndRunEE := "OSGi/Minimum-1.0",
    osgiBndRunFramework := "",
    osgiBndRunFW := "org.apache.felix.framework",
    osgiBndRunProperties := "",
    osgiBndRunRepos := List[String](),
    osgiBndRunRequires := "",
    osgiBndRunVM := "",
    osgiBndSub := List[String](),
    osgiBndServiceComponent := "",
    osgiBndSources := false,
    osgiBndTestCases := List[String]()))

  /** Get an exists or create a new instance for the cnf project */
  def get(cnf: File): Bndtools =
    cnfProjects find (_.get.exists(_.cnfLocation == cnf)) flatMap (_.get.map(_.bndtool)) getOrElse {
      val bndtoolInstance = Project(cnf, new Bndtools(cnf))
      cnfProjects = cnfProjects :+ new WeakReference(bndtoolInstance)
      bndtoolInstance.bndtool
    }

  case class Project(cnfLocation: File, bndtool: Bndtools)
}
