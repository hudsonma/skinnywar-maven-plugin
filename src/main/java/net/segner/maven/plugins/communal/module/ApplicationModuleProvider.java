package net.segner.maven.plugins.communal.module;

import net.java.truevfs.access.TFile;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.maven.model.Build;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;

@Named
@Singleton
public class ApplicationModuleProvider {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationModuleProvider.class);
    public static final String MAVEN_EAR_PLUGIN = "org.apache.maven.plugins:maven-ear-plugin";
    public static final String MODULE_EXTENSION_EAR = ".ear";

    private Build mavenBuild;
    @Inject
    private Provider<EarModule> earModuleProvider;
    @Inject
    private Provider<WebModule> webModuleProvider;

    /**
     * Builds a ApplicationModule for the provided path
     *
     * @param path File path of the module root folder / file
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public <T extends GenericApplicationModule> T get(TFile path) throws IOException, IllegalModuleException {

        // Basic sanity check
        Validate.notNull(path);
        String name = path.getName();
        Validate.isTrue(path.exists(), "File path does not exist: " + name);

        GenericApplicationModule module = findByDefaultPathInspection(path);
        if (module != null) {
            Validate.isTrue(module.canRead(), "Unable to read module " + name);
            Validate.isTrue(module.canWrite(), "Unable to write module " + name);
            return (T) module;
        }

        // Unable to determine type
        throw new IllegalModuleException("Path does not represent a known application module");
    }

    @Nonnull
    public <T extends GenericApplicationModule> T get(TFile path, GenericApplicationModule container) throws IOException, IllegalModuleException {
        T module = get(path);
        module.setUnpacked(new TFile(container.getUnpackFolder(), container.getModuleRoot().toPath().relativize(module.getModuleRoot().toPath()).toString()));
        return module;
    }

    @Nonnull
    public EarModule getEar() {
        EarModule module = earModuleProvider.get();
        String expectedEarPath = mavenBuild.getDirectory() + File.separator + mavenBuild.getFinalName() + MODULE_EXTENSION_EAR;
        logger.debug("EAR file expected at: " + expectedEarPath);
        module.init(expectedEarPath);

        // find shared library path, otherwise default is used
        Xpp3Dom earPluginConfig = (Xpp3Dom) mavenBuild.getPluginsAsMap().get(MAVEN_EAR_PLUGIN).getConfiguration();
        Xpp3Dom child = earPluginConfig.getChild("defaultLibBundleDir");
        if (child != null && StringUtils.isNotBlank(child.getValue())) {
            String libraryFolder = child.getValue();
            logger.debug("Using ear library folder from maven ear plugin definition: " + libraryFolder);
            module.setLibraryPath(libraryFolder);
        }

        return module;
    }

    /**
     * returns a module based on inspecting expected default paths that would exist in known modules
     */
    @Nullable
    private GenericApplicationModule findByDefaultPathInspection(TFile path) {
        // check if its a WAR by looking for WEB-INF
        TFile webmd = new TFile(path, WebModule.DEFAULT_WEBMODULE_METADATAPATH);
        if (webmd.exists()) {
            WebModule wm = webModuleProvider.get();
            wm.init(path);
            return wm;
        }

        // TODO account for EJB
        //
        return null;
    }

    public void setMavenBuild(Build mavenBuild) {
        this.mavenBuild = mavenBuild;
    }
}
