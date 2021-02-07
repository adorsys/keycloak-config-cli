package de.adorsys.keycloak.config.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

@Slf4j
@Order(1)
@Component
class FileResourceExtractor implements ResourceExtractor {

    public boolean canHandleResource(Resource resource) throws IOException {
        File file = resource.getFile();
        return file.isFile() && file.canRead();
    }

    public Collection<File> extract(Resource resource) throws IOException {
        log.debug("Extracting files from FileResource ...");
        Assert.notNull(resource, "The resource to extract files cannot be null!");

        File file = resource.getFile();
        return FileUtils.extractFile(file);
    }
}
