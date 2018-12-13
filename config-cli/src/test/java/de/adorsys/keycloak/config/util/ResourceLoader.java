package de.adorsys.keycloak.config.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;

public class ResourceLoader {

    public static File loadResource(String path) {
        ClassLoader classLoader = ResourceLoader.class.getClassLoader();
        URL resource = classLoader.getResource(path);

        if(resource == null) {
            throw new IllegalArgumentException("Cannot find file at '" + path + "'");
        }

        String filename = resource.getFile();

        return new File(filename);
    }

    public static File loadProjectFile(String path) {
        ClassLoader classLoader = ResourceLoader.class.getClassLoader();
        URL resource = classLoader.getResource("");

        if (resource == null) {
            throw new IllegalArgumentException("Cannot find file at '" + path + "'");
        }

        String projectPath = resource.toString() + "../../";
        String filePath = projectPath + path;

        URL fileUrl;
        try {
            fileUrl = new URL(filePath);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        String filename = fileUrl.getFile();
        return new File(filename);
    }

    public static File loadTargetFile(String relativeFilePath) {
        ProtectionDomain protectionDomain = ResourceLoader.class.getProtectionDomain();
        CodeSource codeSource = protectionDomain.getCodeSource();
        URL location = codeSource.getLocation();

        URI uri = getUri(location);
        File file = new File(uri);

        return new File(file, relativeFilePath);
    }

    private static URI getUri(URL location) {
        URI uri;

        try {
            uri = location.toURI();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }

        return uri;
    }
}
