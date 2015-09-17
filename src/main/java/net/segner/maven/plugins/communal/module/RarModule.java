package net.segner.maven.plugins.communal.module;

import net.java.truevfs.access.TFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.Map;

public class RarModule extends GenericApplicationModule implements ApplicationModule {
    private static final Logger logger = LoggerFactory.getLogger(RarModule.class);

    public static final String DEFAULT_WEBMODULE_LIBPATH = "." + File.separator;
    public static final String EXTENSION = "rar";

    RarModule() {
    }

    public RarModule(TFile archivePath) {
        super(archivePath);
    }

    @Override
    public String getDefaultLibraryPath() {
        return DEFAULT_WEBMODULE_LIBPATH;
    }

    void init(TFile path) {
        super.init(path);
    }

}
