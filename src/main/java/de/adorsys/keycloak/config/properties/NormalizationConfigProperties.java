/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2021 adorsys GmbH & Co. KG @ https://adorsys.com
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

package de.adorsys.keycloak.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@ConfigurationProperties(prefix = "normalization", ignoreUnknownFields = false)
@Validated
public class NormalizationConfigProperties {

    @Valid
    private final NormalizationFilesProperties files;

    private final OutputFormat outputFormat;

    private final String fallbackVersion;

    public NormalizationConfigProperties(@DefaultValue NormalizationFilesProperties files,
                                         @DefaultValue("yaml") OutputFormat outputFormat,
                                         String fallbackVersion) {
        this.files = files;
        this.outputFormat = outputFormat;
        this.fallbackVersion = fallbackVersion;
    }

    public NormalizationFilesProperties getFiles() {
        return files;
    }

    public OutputFormat getOutputFormat() {
        return outputFormat;
    }

    public String getFallbackVersion() {
        return fallbackVersion;
    }

    public static class NormalizationFilesProperties {

        @NotNull
        private final Collection<String> inputLocations;

        @NotNull
        private final Collection<String> excludes;

        @NotNull
        private final boolean includeHiddenFiles;

        @NotNull
        private final String outputDirectory;

        public NormalizationFilesProperties(Collection<String> inputLocations,
                                            @DefaultValue Collection<String> excludes,
                                            @DefaultValue("false") boolean includeHiddenFiles,
                                            String outputDirectory) {
            this.inputLocations = inputLocations;
            this.excludes = excludes;
            this.includeHiddenFiles = includeHiddenFiles;
            this.outputDirectory = outputDirectory;
        }

        public Collection<String> getInputLocations() {
            return inputLocations;
        }

        public Collection<String> getExcludes() {
            return excludes;
        }

        public boolean isIncludeHiddenFiles() {
            return includeHiddenFiles;
        }

        public String getOutputDirectory() {
            return outputDirectory;
        }
    }

    public enum OutputFormat {
        JSON, YAML
    }
}
