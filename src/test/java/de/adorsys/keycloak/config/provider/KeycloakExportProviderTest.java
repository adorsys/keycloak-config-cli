/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2025 adorsys GmbH & Co. KG @ https://adorsys.com
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---license-end
 */

package de.adorsys.keycloak.config.provider;

import de.adorsys.keycloak.config.exception.InvalidImportException;
import de.adorsys.keycloak.config.model.ImportResource;
import de.adorsys.keycloak.config.properties.NormalizationConfigProperties;
import de.adorsys.keycloak.config.properties.NormalizationConfigProperties.NormalizationFilesProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.PathMatcher;
import org.keycloak.representations.idm.RealmRepresentation;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeycloakExportProviderTest {

    @Mock
    private PathMatchingResourcePatternResolver patternResolver;

    @Mock
    private NormalizationConfigProperties normalizationConfigProperties;

    @Mock
    private NormalizationFilesProperties filesProperties;

    @Mock
    private PathMatcher pathMatcher;

    private KeycloakExportProvider exportProvider;

    @BeforeEach
    void setUp() {
        // Set up default behaviors for mocks
        // make these lenient to avoid unnecessary stubbing failures in tests that don't use them
        lenient().when(normalizationConfigProperties.getFiles()).thenReturn(filesProperties);
        lenient().when(patternResolver.getPathMatcher()).thenReturn(pathMatcher);
        lenient().when(filesProperties.getExcludes()).thenReturn(Collections.emptyList());
        lenient().when(filesProperties.isIncludeHiddenFiles()).thenReturn(false);
        
        exportProvider = new KeycloakExportProvider(patternResolver, normalizationConfigProperties);
    }

    @Test
    void testReadFromLocations_Success() throws IOException, URISyntaxException {
        // Given
        String location = "classpath:import-files/*.yaml";
        Resource mockResource = mock(Resource.class);
        File mockFile = mock(File.class);
        String yamlContent = "id: test-realm\nrealm: test";

        when(filesProperties.getInputLocations()).thenReturn(Collections.singletonList(location));
        when(patternResolver.getResources(anyString())).thenReturn(new Resource[]{mockResource});
        when(mockResource.isFile()).thenReturn(true);
        when(mockResource.getFile()).thenReturn(mockFile);
        when(mockResource.getInputStream()).thenReturn(new ByteArrayInputStream(yamlContent.getBytes()));
    when(mockResource.getURI()).thenReturn(new URI("file:/test/realm.yaml"));
    when(mockResource.getURL()).thenReturn(new URI("file:/test/realm.yaml").toURL());
        when(mockFile.isDirectory()).thenReturn(false);
        when(mockFile.isHidden()).thenReturn(false);

        // When
        Map<String, Map<String, List<RealmRepresentation>>> result = exportProvider.readFromLocations();

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result.get(location)).isNotNull();
        verify(patternResolver).getResources(location);
    }

    @Test
    void testReadFromLocations_MultipleLocations() throws IOException, URISyntaxException {
        // Given
        List<String> locations = Arrays.asList("classpath:import-files/*.yaml", "file:/other/*.json");
        Resource mockResource1 = mock(Resource.class);
        Resource mockResource2 = mock(Resource.class);
        File mockFile1 = mock(File.class);
        File mockFile2 = mock(File.class);
        String yamlContent = "id: test-realm-1\nrealm: test1";

        when(filesProperties.getInputLocations()).thenReturn(locations);
        when(patternResolver.getResources(locations.get(0))).thenReturn(new Resource[]{mockResource1});
        when(patternResolver.getResources(locations.get(1))).thenReturn(new Resource[]{mockResource2});
        
        when(mockResource1.isFile()).thenReturn(true);
        when(mockResource1.getFile()).thenReturn(mockFile1);
        when(mockResource1.getInputStream()).thenReturn(new ByteArrayInputStream(yamlContent.getBytes()));
    when(mockResource1.getURI()).thenReturn(new URI("file:/test/realm1.yaml"));
    when(mockResource1.getURL()).thenReturn(new URI("file:/test/realm1.yaml").toURL());
        when(mockFile1.isDirectory()).thenReturn(false);
        when(mockFile1.isHidden()).thenReturn(false);

        when(mockResource2.isFile()).thenReturn(true);
        when(mockResource2.getFile()).thenReturn(mockFile2);
        when(mockResource2.getInputStream()).thenReturn(new ByteArrayInputStream(yamlContent.getBytes()));
    when(mockResource2.getURI()).thenReturn(new URI("file:/test/realm2.json"));
    when(mockResource2.getURL()).thenReturn(new URI("file:/test/realm2.json").toURL());
        when(mockFile2.isDirectory()).thenReturn(false);
        when(mockFile2.isHidden()).thenReturn(false);

        // When
        Map<String, Map<String, List<RealmRepresentation>>> result = exportProvider.readFromLocations();

        // Then
        assertThat(result).hasSize(2);
        verify(patternResolver).getResources(locations.get(0));
        verify(patternResolver).getResources(locations.get(1));
    }

    @Test
    void testReadFromLocations_IOException() throws IOException {
        // Given
        String location = "classpath:import-files/*.yaml";
        when(filesProperties.getInputLocations()).thenReturn(Collections.singletonList(location));
        when(patternResolver.getResources(anyString())).thenThrow(new IOException("Test IO error"));

        // Then
        assertThatThrownBy(() -> exportProvider.readFromLocations())
            .isInstanceOf(InvalidImportException.class)
            .hasMessageContaining("Unable to proceed location");
    }

    @Test
    void testReadFromLocations_NoFilesMatching() throws IOException {
        // Given
        String location = "classpath:import-files/*.yaml";
        when(filesProperties.getInputLocations()).thenReturn(Collections.singletonList(location));
        when(patternResolver.getResources(anyString())).thenReturn(new Resource[]{});

        // Then
        assertThatThrownBy(() -> exportProvider.readFromLocations())
            .isInstanceOf(InvalidImportException.class)
            .hasMessageContaining("No files matching");
    }

    @Test
    void testFilterExcludedResources_NotAFile() throws IOException {
        // Given
        Resource resource = mock(Resource.class);
        when(resource.isFile()).thenReturn(false);

        // When
        boolean result = exportProvider.filterExcludedResources(resource);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void testSetupAuthentication_WithValidUserInfo() throws IOException, URISyntaxException {
        // Given
        Resource resource = mock(UrlResource.class);
        URI uri = new URI("http://user:password@example.com/test.yaml");
        when(resource.getURL()).thenReturn(uri.toURL());
        when(resource.getURI()).thenReturn(uri);

        try {
            // When
            Resource result = exportProvider.setupAuthentication(resource);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getURI().toString()).contains("***@");
        } finally {
            Authenticator.setDefault(null);
        }
    }

    @Test
    void testSetupAuthentication_WithoutUserInfo() throws IOException, URISyntaxException {
        // Given
        Resource resource = mock(Resource.class);
        URI uri = new URI("http://example.com/test.yaml");
        when(resource.getURL()).thenReturn(uri.toURL());

        try {
            // When
            Resource result = exportProvider.setupAuthentication(resource);

            // Then
            assertThat(result).isSameAs(resource);
        } finally {
            Authenticator.setDefault(null);
        }
    }

    @Test
    void testFilterEmptyResources_NonEmpty() {
        // Given
        ImportResource resource = new ImportResource("test.yaml", "content");

        // When
        boolean result = exportProvider.filterEmptyResources(resource);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void testFilterEmptyResources_Empty() {
        // Given
        ImportResource resource = new ImportResource("test.yaml", "");

        // When
        boolean result = exportProvider.filterEmptyResources(resource);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void testReadContent_SingleDocument() {
        // Given
        String yamlContent = "id: test-realm\nrealm: test";

        // When
        List<RealmRepresentation> result = exportProvider.readContent(yamlContent);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRealm()).isEqualTo("test");
    }

    @Test
    void testPrepareResourceLocation_WithZipPrefix() {
        // Given
        String location = "zip:file:/path/to/file.zip";

        // When
        String result = exportProvider.prepareResourceLocation(location);

        // Then
        assertThat(result).isEqualTo("jar:file:/path/to/file.zip");
    }

    @Test
    void testPrepareResourceLocation_WithoutPrefix() {
        // Given
        String location = "/path/to/file.yaml";

        // When
        String result = exportProvider.prepareResourceLocation(location);

        // Then
        assertThat(result).isEqualTo("file:/path/to/file.yaml");
    }

    @Test
    void testPrepareResourceLocation_WithHttpPrefix() {
        // Given
        String location = "http://example.com/realm.yaml";

        // When
        String result = exportProvider.prepareResourceLocation(location);

        // Then
        assertThat(result).isEqualTo("http://example.com/realm.yaml");
    }

    @Test
    void testReadResource_InputStreamIOException() throws IOException {
        // Given
        Resource resource = mock(Resource.class);
        when(resource.getInputStream()).thenThrow(new IOException("Test IO error"));
        when(resource.toString()).thenReturn("test-resource");
        // prevent setupAuthentication() from throwing NPE by providing a URL
        URI uri = URI.create("http://example.com/test.yaml");
        when(resource.getURL()).thenReturn(uri.toURL());

        // Then
        assertThatThrownBy(() -> exportProvider.readResource(resource))
            .isInstanceOf(InvalidImportException.class)
            .hasMessageContaining("Unable to proceed resource 'test-resource'");
    }

    @Test
    void testReadResource_URIException() throws IOException {
        // Given
        Resource resource = mock(Resource.class);
        when(resource.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));
        // ensure getURL() is present so setupAuthentication() doesn't NPE
        URI uri = URI.create("http://example.com/test.yaml");
        when(resource.getURL()).thenReturn(uri.toURL());
        when(resource.getURI()).thenThrow(new IOException("URI error"));
        when(resource.toString()).thenReturn("test-resource");

        // Then
        assertThatThrownBy(() -> exportProvider.readResource(resource))
            .isInstanceOf(InvalidImportException.class)
            .hasMessageContaining("Unable to proceed resource 'test-resource'");
    }

    @Test
    void testSetupAuthentication_IncompleteUserInfo() throws IOException, URISyntaxException {
        // Given
        Resource resource = mock(UrlResource.class);
        URI uri = new URI("http://user@example.com/test.yaml");
        when(resource.getURL()).thenReturn(uri.toURL());

        try {
            // When
            Resource result = exportProvider.setupAuthentication(resource);

            // Then
            assertThat(result).isSameAs(resource);
        } finally {
            Authenticator.setDefault(null);
        }
    }

    @Test
    void testSetupAuthentication_URLIOException() throws IOException {
        // Given
        Resource resource = mock(Resource.class);
        when(resource.getURL()).thenThrow(new IOException("URL error"));

        // When
        Resource result = exportProvider.setupAuthentication(resource);

        // Then
        assertThat(result).isSameAs(resource);
    }

    @Test
    void testFilterExcludedResources_IOExceptionOnGetFile() throws IOException {
        // Given
        Resource resource = mock(Resource.class);
        when(resource.isFile()).thenReturn(true);
        when(resource.getFile()).thenThrow(new IOException("File error"));

        // When
        boolean result = exportProvider.filterExcludedResources(resource);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void testFilterExcludedResources_ExcludePatternMatching() throws IOException {
        // Given
        Resource resource = mock(Resource.class);
        File file = mock(File.class);
        when(resource.isFile()).thenReturn(true);
        when(resource.getFile()).thenReturn(file);
        when(file.isDirectory()).thenReturn(false);
        when(file.isHidden()).thenReturn(false);
        when(file.getPath()).thenReturn("/path/to/test.tmp");
        when(filesProperties.getExcludes()).thenReturn(Arrays.asList("**/*.tmp", "*.log", "/hidden/**"));
        when(pathMatcher.match(anyString(), eq("/path/to/test.tmp"))).thenReturn(true);

        // When
        boolean result = exportProvider.filterExcludedResources(resource);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void testReadContent_EmptyInput() {
        // Given
        String yamlContent = "";

        // When
        List<RealmRepresentation> result = exportProvider.readContent(yamlContent);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testReadFromLocations_AllResourcesFiltered() throws IOException {
        // Given
        String location = "classpath:import-files/*.yaml";
        Resource mockResource = mock(Resource.class);
        File mockFile = mock(File.class);

        when(filesProperties.getInputLocations()).thenReturn(Collections.singletonList(location));
        when(patternResolver.getResources(anyString())).thenReturn(new Resource[]{mockResource});
        when(mockResource.isFile()).thenReturn(true);
        when(mockResource.getFile()).thenReturn(mockFile);
        when(mockFile.isDirectory()).thenReturn(true); // This will cause the resource to be filtered out

        // Then
        assertThatThrownBy(() -> exportProvider.readFromLocations())
            .isInstanceOf(InvalidImportException.class)
            .hasMessageContaining("No files matching");
    }
}