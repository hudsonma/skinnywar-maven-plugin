package net.segner.maven.plugins.communal.weblogic;


import net.segner.maven.plugins.communal.module.ApplicationModule;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;

public class WeblogicClassloaderStructure {
    public static final String TAG_CLASSLOADER_STRUCTURE = "classloader-structure";
    public static final String TAG_MODULE_URI = "module-uri";
    public static final String TAG_MODULE_REF = "module-ref";

    private final WeblogicApplicationXml applicationXml;

    public WeblogicClassloaderStructure(WeblogicApplicationXml applicationXml) {
        Validate.notNull(applicationXml);
        this.applicationXml = applicationXml;
    }

    public void removeModule(String sharedModuleName) throws SAXException, IOException, ParserConfigurationException {
        Node shared = findModuleRef(sharedModuleName);
        if (shared == null) return;
        Node parentNode = shared.getParentNode();
        parentNode.removeChild(shared);

        // remove parent if it is now an empty node
        if (!hasElementChildNode(parentNode)) {
            parentNode.getParentNode().removeChild(parentNode);
        }
    }

    private boolean hasElementChildNode(Node parentNode) {
        NodeList nodeList = parentNode.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (Node.ELEMENT_NODE == node.getNodeType()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets the provided module as the overall parent of the current classloader-structure section.
     *
     * @return Element referencing the newly created parent classloader-structure
     */
    public Element makeParentClassloaderStructure(String sharedModuleName) throws SAXException, IOException, ParserConfigurationException {
        Element newParentClStructure = generateModuleClassloaderStructure(sharedModuleName);
        Node clssStructureRoot = findClassloaderStructureRoot();
        if (clssStructureRoot != null) {
            clssStructureRoot.getParentNode().appendChild(newParentClStructure);
            if (hasElementChildNode(clssStructureRoot)) {
                newParentClStructure.appendChild(clssStructureRoot);
            }else{
                clssStructureRoot.getParentNode().removeChild(clssStructureRoot);
            }
        } else {
            applicationXml.getXmlRoot().appendChild(newParentClStructure);
        }
        return newParentClStructure;
    }

    @Nullable
    private Node findClassloaderStructureRoot() throws SAXException, IOException, ParserConfigurationException {
        Document document = applicationXml.getDocument();
        Element weblogicRoot = applicationXml.getXmlRoot();
        NodeList nodes = document.getElementsByTagName(TAG_CLASSLOADER_STRUCTURE);

        // find an existing classloader
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getParentNode().equals(weblogicRoot)) {
                return node;
            }
        }
        return null;
    }

    @Nullable
    private Node findModuleRef(String sharedModuleName) throws SAXException, IOException, ParserConfigurationException {
        NodeList nodes = applicationXml.getDocument().getElementsByTagName(TAG_MODULE_URI);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (StringUtils.equalsIgnoreCase(node.getTextContent(), sharedModuleName)) {
                return node.getParentNode();
            }
        }
        return null;
    }

    @Nonnull
    private Element generateModuleClassloaderStructure(String moduleName) throws SAXException, IOException, ParserConfigurationException {
        Document document = applicationXml.getDocument();
        Element moduleUri = document.createElement(TAG_MODULE_URI);
        Element moduleRef = document.createElement(TAG_MODULE_REF);
        Element classloader = document.createElement(TAG_CLASSLOADER_STRUCTURE);

        moduleUri.setTextContent(moduleName);
        moduleRef.appendChild(moduleUri);
        classloader.appendChild(moduleRef);

        return classloader;
    }

    public void verifyModulesExist(Element classloaderStructureParent, List<ApplicationModule> standardModules) throws SAXException, IOException, ParserConfigurationException {
        for (ApplicationModule applicationModule : standardModules) {
            String moduleName = applicationModule.getName();
            Node ref = findModuleRef(moduleName);
            if (ref == null) {
                Element moduleStructure = generateModuleClassloaderStructure(moduleName);
                classloaderStructureParent.appendChild(moduleStructure);
            }
        }
    }
}
