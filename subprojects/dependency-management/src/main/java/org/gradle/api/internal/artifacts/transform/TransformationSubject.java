/*
 * Copyright 2018 the original author or authors.
 *
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
 */

package org.gradle.api.internal.artifacts.transform;

import com.google.common.collect.ImmutableList;
import org.gradle.api.Describable;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.internal.hash.HashCode;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Optional;

/**
 * Subject which is transformed or the result of a transformation.
 */
public abstract class TransformationSubject implements Describable {

    public static TransformationSubject failure(String displayName, Throwable failure) {
        return new TransformationFailedSubject(displayName, failure);
    }

    public static TransformationSubject initial(File file) {
        return new InitialFileTransformationSubject(file);
    }

    public static TransformationSubject initial(ComponentArtifactIdentifier artifactId, File file) {
        return new InitialArtifactTransformationSubject(artifactId, file);
    }

    /**
     * The files which should be transformed.
     */
    public abstract ImmutableList<File> getFiles();

    public abstract String getInitialFileName();

    public abstract Optional<ProjectComponentIdentifier> getProducer();

    public abstract ImmutableList<HashCode> getPreviousTransformerIdentities();

    /**
     * Records the failure to transform a previous subject.
     */
    @Nullable
    public abstract Throwable getFailure();

    public TransformationSubject transformationFailed(Throwable failure) {
        return failure(getDisplayName(), failure);
    }

    public TransformationSubject transformationSuccessful(ImmutableList<File> result, HashCode transformerHash) {
        return new DefaultTransformationSubject(
            this,
            result,
            ImmutableList.<HashCode>builder()
                .addAll(getPreviousTransformerIdentities())
                .add(transformerHash)
                .build());
    }

    private static class TransformationFailedSubject extends TransformationSubject {
        private final String displayName;
        private final Throwable failure;

        public TransformationFailedSubject(String displayName, Throwable failure) {
            this.displayName = displayName;
            this.failure = failure;
        }

        @Override
        public ImmutableList<File> getFiles() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getInitialFileName() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<ProjectComponentIdentifier> getProducer() {
            return Optional.empty();
        }

        @Override
        public ImmutableList<HashCode> getPreviousTransformerIdentities() {
            return ImmutableList.of();
        }

        @Nullable
        @Override
        public Throwable getFailure() {
            return failure;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return getDisplayName();
        }
    }

    private static abstract class AbstractInitialTransformationSubject extends TransformationSubject {
        private final File file;

        public AbstractInitialTransformationSubject(File file) {
            this.file = file;
        }

        @Override
        public ImmutableList<File> getFiles() {
            return ImmutableList.of(file);
        }

        public File getFile() {
            return file;
        }

        @Nullable
        @Override
        public Throwable getFailure() {
            return null;
        }

        @Override
        public String getInitialFileName() {
            return getFile().getName();
        }

        @Override
        public ImmutableList<HashCode> getPreviousTransformerIdentities() {
            return ImmutableList.of();
        }

        @Override
        public String toString() {
            return getDisplayName();
        }
    }

    private static class InitialFileTransformationSubject extends AbstractInitialTransformationSubject {
        public InitialFileTransformationSubject(File file) {
            super(file);
        }

        @Override
        public String getDisplayName() {
            return "file " + getFile();
        }

        @Override
        public Optional<ProjectComponentIdentifier> getProducer() {
            return Optional.empty();
        }
    }

    private static class InitialArtifactTransformationSubject extends AbstractInitialTransformationSubject {
        private final ComponentArtifactIdentifier artifactId;

        public InitialArtifactTransformationSubject(ComponentArtifactIdentifier artifactId, File file) {
            super(file);
            this.artifactId = artifactId;
        }

        @Override
        public String getDisplayName() {
            return "artifact " + artifactId.getDisplayName();
        }

        @Override
        public Optional<ProjectComponentIdentifier> getProducer() {
            ComponentIdentifier componentIdentifier = artifactId.getComponentIdentifier();
            if (componentIdentifier instanceof ProjectComponentIdentifier) {
                return Optional.of((ProjectComponentIdentifier) componentIdentifier);
            }
            return Optional.empty();
        }
    }

    public static class DefaultTransformationSubject extends TransformationSubject {
        private final TransformationSubject previous;
        private final ImmutableList<File> files;
        private final ImmutableList<HashCode> previousTransformationIdentities;

        public DefaultTransformationSubject(TransformationSubject previous, ImmutableList<File> files, ImmutableList<HashCode> previousTransformationIdentities) {
            this.previous = previous;
            this.files = files;
            this.previousTransformationIdentities = previousTransformationIdentities;
        }

        @Override
        public ImmutableList<File> getFiles() {
            return files;
        }

        @Override
        public String getInitialFileName() {
            return previous.getInitialFileName();
        }

        @Override
        public ImmutableList<HashCode> getPreviousTransformerIdentities() {
            return previousTransformationIdentities;
        }

        @Override
        public Optional<ProjectComponentIdentifier> getProducer() {
            return previous.getProducer();
        }

        @Override
        public Throwable getFailure() {
            return null;
        }

        @Override
        public String getDisplayName() {
            return previous.getDisplayName();
        }

        @Override
        public String toString() {
            return getDisplayName();
        }
    }
}
