package de.adorsys.keycloak.config.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.Base64Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

@Slf4j
@Order(3)
@Component
class UrlResourceExtractor implements ResourceExtractor {

    @Override
    public boolean canHandleResource(Resource resource) {
        return resource instanceof UrlResource;
    }

    @Override
    public Collection<File> extract(Resource resource) throws IOException {
        log.debug("Extracting files from UrlResource ...");
        Assert.notNull(resource, "The resource to extract files must be not null!");

        URL url = resource.getURL();
        URLConnection urlConnection = url.openConnection();
        maybeSetupBasicAuth(url, urlConnection);

        InputStream inputStream = urlConnection.getInputStream();
        File tempFile = FileUtils.createTempFile(resource.getFilename(), inputStream);
        return FileUtils.extractFile(tempFile);
    }

    private void maybeSetupBasicAuth(URL url, URLConnection connection) {
        String userInfo = url.getUserInfo();
        if (userInfo != null) {
            String basicAuthHeader = "Basic " + Base64Utils.encodeToString(userInfo.getBytes(StandardCharsets.UTF_8));
            connection.setRequestProperty("Authorization", basicAuthHeader);
        }
    }
}
