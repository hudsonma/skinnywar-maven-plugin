package net.segner.maven.plugins.communal.enhancer;

import net.segner.maven.plugins.communal.module.EarModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class StandardlSkinnyWarEarEnhancer extends SkinnyWarEarEnhancer implements ModuleEnhancer<EarModule> {
    private static final Logger logger = LoggerFactory.getLogger(StandardlSkinnyWarEarEnhancer.class);

    public static final String MSGINFO_CREATING_SKINNY_WARS = "Enhancing EAR with Standard Skinny WAR layout";

    @Override
    public void enhance() throws IOException {
        logger.info(MSGINFO_CREATING_SKINNY_WARS);
        makeSkinnyModules();
        logger.info(MSGINFO_SUCCESS);
    }
}
