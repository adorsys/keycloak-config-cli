package de.adorsys.keycloak.config.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

@Slf4j
@Order(1)
@Component
class FileResourceExtractor implements ResourceExtractor {

    public boolean canHandleResource(Resource resource) {
        try {
            File file = resource.getFile();
            return file.isFile() && file.canRead();
        } catch (IOException ioex) {
            log.error("Unable to handle resource to import file!", ioex);
            return false;
        }
    }

    public Collection<File> extract(Resource resource) {
        try {
            return Arrays.asList(resource.getFile());
        } catch (IOException ioex) {
            log.error("Unable to handle resource to import file!", ioex);
            return Collections.emptyList();
        }
    }
}
