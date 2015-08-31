package net.segner.maven.plugins.communal;

import net.java.truevfs.access.TVFS;
import net.java.truevfs.kernel.spec.FsSyncException;
import net.segner.maven.plugins.communal.enhancer.CommunalSkinnyWarEarEnhancer;
import net.segner.maven.plugins.communal.enhancer.ModuleEnhancer;
import net.segner.maven.plugins.communal.enhancer.SkinnyWarEarEnhancer;
import net.segner.maven.plugins.communal.enhancer.StandardlSkinnyWarEarEnhancer;
import net.segner.maven.plugins.communal.module.ApplicationModuleProvider;
import net.segner.maven.plugins.communal.module.EarModule;
import net.segner.maven.plugins.communal.enhancer.WeblogicLtwMetadataEnhancer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.sisu.Description;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mojo(name = "ear", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST)
@Description("Enhances the EAR with an alternate layout")
public class EarLayoutEnhancerMojo extends AbstractMojo {

    public static final List<String> ASPECTJLIBRARIES = Arrays.asList(StringUtils.split("aopalliance aspectjweaver aspectjrt"));

    /**
     * The bundle name of the war to be used as the communal war. This expects an ear to have already been packaged.
     * A skinny WAR is a WAR that does not have all of its dependencies in WEB-INF/lib. Instead those dependencies are shared between the WARs.
     * Usually the dependencies are shared via the EAR. When this is not empty, the shared dependencies will be moved to the communal WAR.
     */
    @Parameter(alias = "communalWar")
    protected String communalModuleName;

    @Parameter(defaultValue = "true")
    protected Boolean warningBreaksBuild;

    @Parameter(defaultValue = "true")
    protected Boolean forceAspectJLibToEar;

    @Parameter(defaultValue = "true")
    private Boolean generateWeblogicLtwMetadata;

    @Parameter(alias = "earLibraries")
    protected List<LibraryFilter> earLibraryList = new ArrayList<>();

    @Parameter(alias = "pinnedLibraries")
    protected List<LibraryFilter> pinnedLibraryList = new ArrayList<>();

    @Parameter(defaultValue = "${project.build}")
    private Build mavenBuild;

    @Inject
    private ApplicationModuleProvider applicationModuleProvider;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            // get the ear module
            EarModule earModule = applicationModuleProvider.getEar();
            Validate.isTrue(earModule.canRead() && earModule.canWrite(), "Missing read / write permissions to ear target folder");

            // enhance the ear with the skinny war pattern
            ModuleEnhancer<EarModule> earModuleEnhancer = fetchEarEnhancer();
            earModuleEnhancer.setTargetModule(earModule);
            earModuleEnhancer.enhance();

        } catch (IllegalArgumentException ex) {
            getLog().warn(ex.getMessage());
            if (warningBreaksBuild) {
                throw new MojoFailureException(ex.getMessage(), ex);
            }
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } finally {
            try {
                TVFS.umount();
            } catch (FsSyncException e) {
                //noinspection ThrowFromFinallyBlock
                throw new MojoExecutionException("TVFS failed to unmount cleanly", e);
            }
        }
    }

    @Nonnull
    private ModuleEnhancer<EarModule> fetchEarEnhancer() {

        // merge ear library list with aspectj list for the complete list
        List<LibraryFilter> fullEarLibraryList = new ArrayList<>(earLibraryList);
        if (forceAspectJLibToEar) {
            ASPECTJLIBRARIES.forEach(earLib -> fullEarLibraryList.add(new LibraryPrefixFilter(earLib)));
        }

        // create skinny enhancer
        SkinnyWarEarEnhancer skinnyEnhancer = StringUtils.isNotBlank(communalModuleName) ?
                new CommunalSkinnyWarEarEnhancer(communalModuleName) :
                new StandardlSkinnyWarEarEnhancer();
        skinnyEnhancer.setPinnedLibraries(pinnedLibraryList);
        skinnyEnhancer.setEarLibraries(fullEarLibraryList);

        // create weblogic ltw metadata generation
        if (generateWeblogicLtwMetadata) {
            WeblogicLtwMetadataEnhancer ltwEnhancer = new WeblogicLtwMetadataEnhancer();
            ltwEnhancer.setSkinnyEnhancer(skinnyEnhancer);
            return ltwEnhancer;
        } else {
            return skinnyEnhancer;
        }
    }

    public void setMavenBuild(Build mavenBuild) {
        this.mavenBuild = mavenBuild;
        applicationModuleProvider.setMavenBuild(mavenBuild);
    }
}
