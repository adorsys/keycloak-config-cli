package de.adorsys.keycloak.config.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Order(2)
@Component
class DirectoryResourceExtractor implements ResourceExtractor {

    public boolean canHandleResource(Resource resource) throws IOException {
        File file = resource.getFile();
        return file.isDirectory() && file.canRead();
    }

    public Collection<File> extract(Resource resource) throws IOException {
        log.debug("Extracting files from DirectoryResource ...");
        Assert.notNull(resource, "The resource to extract files cannot be null!");

        File file = resource.getFile();
        File[] files = file.listFiles();

        return Optional.ofNullable(files).map(f -> Arrays.stream(f).filter(File::isFile).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }
}
