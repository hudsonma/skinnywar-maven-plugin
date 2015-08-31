package net.segner.maven.plugins.communal.enhancer;

import net.segner.maven.plugins.communal.module.EarModule;
import net.segner.maven.plugins.communal.weblogic.WeblogicApplicationXml;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;

public class WeblogicLtwMetadataEnhancer extends AbstractEnhancer<EarModule> {
    private static final Logger logger = LoggerFactory.getLogger(WeblogicLtwMetadataEnhancer.class);

    private SkinnyWarEarEnhancer skinnyEnhancer;

    public void setSkinnyEnhancer(SkinnyWarEarEnhancer skinnyEnhancer) {
        this.skinnyEnhancer = skinnyEnhancer;
    }

    @Override
    public void enhance() throws IOException {
        Validate.notNull(skinnyEnhancer);
        skinnyEnhancer.enhance();
        if (skinnyEnhancer instanceof CommunalSkinnyWarEarEnhancer) {
            logger.info("Enhancing EAR with modified Weblogic classloader-structure...");
            try {
                WeblogicApplicationXml weblogicApplicationXml = new WeblogicApplicationXml(getTargetModule());
                weblogicApplicationXml.setupCommunalWeblogicApplicationXml(skinnyEnhancer.getSharedModuleName());
                weblogicApplicationXml.persistToEarModule();

                logger.info("Finished classloader-structure generation.");
            } catch (ParserConfigurationException | SAXException | TransformerException e) {
                throw new IOException(e.getMessage(), e);
            }
        } else {
            logger.info("Ignored classloader-structure generation -- not communal WAR layout");
        }
    }

    @Override
    public void setTargetModule(EarModule targetModule) {
        super.setTargetModule(targetModule);
        skinnyEnhancer.setTargetModule(targetModule);
    }
}
