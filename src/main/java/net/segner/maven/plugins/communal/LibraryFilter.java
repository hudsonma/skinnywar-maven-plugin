package net.segner.maven.plugins.communal;

public interface LibraryFilter {

    boolean isMatch(String libraryName);
}
