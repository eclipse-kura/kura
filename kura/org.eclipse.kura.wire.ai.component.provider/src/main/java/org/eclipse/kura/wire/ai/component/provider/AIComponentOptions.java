package org.eclipse.kura.wire.ai.component.provider;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.Optional;

public class AIComponentOptions {

    private static final String PREPROCESSOR_MODEL_NAME = "preprocessor.model.name";
    private static final String INFERENCE_MODEL_NAME = "inference.model.name";
    private static final String POSTPROCESSOR_MODEL_NAME = "postprocessor.model.name";

    private final Map<String, Object> properties;

    public AIComponentOptions(Map<String, Object> properties) {
        requireNonNull(properties, "Properties cannot be null");
        this.properties = properties;
    }

    public Optional<String> getPreprocessorModelName() {
        String value = (String) this.properties.get(PREPROCESSOR_MODEL_NAME);
        return (value != null && !value.trim().isEmpty()) ? Optional.of(value.trim()) : Optional.empty();
    }

    public String getInferenceModelName() {
        String value = (String) this.properties.get(INFERENCE_MODEL_NAME);
        return (value != null && !value.trim().isEmpty()) ? value.trim() : null;
    }

    public Optional<String> getPostprocessorModelName() {
        String value = (String) this.properties.get(POSTPROCESSOR_MODEL_NAME);
        return (value != null && !value.trim().isEmpty()) ? Optional.of(value.trim()) : Optional.empty();
    }

}
