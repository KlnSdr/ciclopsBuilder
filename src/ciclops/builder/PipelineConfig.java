package ciclops.builder;

import dobby.util.json.NewJson;

import java.util.List;

public record PipelineConfig(String image, String dockerfile, String[] defaultSteps, String[] releaseSteps) {

    public static PipelineConfig load(NewJson json) {
        final String image = json.getString("image");
        final String dockerfile = json.getString("dockerfile");

        final NewJson pipeline = json.getJson("pipeline");
        final List<Object> defaultSteps = pipeline.getList("default");
        final List<Object> releaseSteps = pipeline.getList("release");

        return new PipelineConfig(image, dockerfile,
                defaultSteps.stream().map(Object::toString).toArray(String[]::new),
                releaseSteps.stream().map(Object::toString).toArray(String[]::new));
    }

    public static boolean isValid(NewJson json) {
        if (!json.hasKey("pipeline") || !(json.hasKey("image") || json.hasKey("dockerfile"))) {
            return false;
        }

        if (json.getString("image") == null && json.getString("dockerfile") == null) {
            return false;
        }

        final NewJson pipeline = json.getJson("pipeline");

        if (pipeline == null) {
            return false;
        }

        return isValidPipeline(pipeline);
    }

    private static boolean isValidPipeline(NewJson pipeline) {
        if (!pipeline.hasKeys("default", "release")) {
            return false;
        }

        final List<Object> defaultSteps = pipeline.getList("default");
        final List<Object> releaseSteps = pipeline.getList("release");
        if (defaultSteps == null || releaseSteps == null) {
            return false;
        }

        return defaultSteps.stream().allMatch(o -> o instanceof String) && releaseSteps.stream().allMatch(o -> o instanceof String);
    }
}
