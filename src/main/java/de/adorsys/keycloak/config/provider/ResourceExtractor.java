package de.adorsys.keycloak.config.provider;

import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

interface ResourceExtractor {

    boolean canHandleResource(Resource resource) throws IOException;

    Collection<File> extract(Resource resource) throws IOException;
}
