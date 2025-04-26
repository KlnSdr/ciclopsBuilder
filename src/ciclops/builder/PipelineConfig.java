package ciclops.builder;

import dobby.util.json.NewJson;

import java.util.List;

import static ciclops.builder.FileUtil.hasFile;

public record PipelineConfig(String image, String[] defaultSteps, String[] releaseSteps) {
    private static final String AND = " && ";
    private static final String SPACE = " ";
    private static final String QUOTE = "'";
    private static String SEPARATOR;

    public static PipelineConfig load(NewJson json) {
        final String image = json.getString("image");
        final NewJson pipeline = json.getJson("pipeline");
        final List<Object> defaultSteps = pipeline.getList("default");
        final List<Object> releaseSteps = pipeline.getList("release");

        return new PipelineConfig(image, defaultSteps.stream().map(Object::toString).toArray(String[]::new), releaseSteps.stream().map(Object::toString).toArray(String[]::new));
    }

    public static boolean isValid(NewJson json) {
        if (!json.hasKey("pipeline") || !(json.hasKey("image"))) {
            return false;
        }

        if (json.getString("image") == null) {
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

    private static void getSeparator() {
        final String sep = System.getenv("SEPARATOR");
        if (sep == null) {
            SEPARATOR = "";
            return;
        }
        SEPARATOR = sep;
    }

    public String getCombinedCommand() {
        getSeparator();
        final StringBuilder combinedCommand = new StringBuilder();

        combinedCommand.append("cd src").append(SPACE).append(AND).append("echo").append(SPACE).append(QUOTE).append("start").append(SEPARATOR).append(QUOTE);

        for (String step : defaultSteps) {
            final String[] pipeSplit = step.split("\\|");
            combinedCommand.append(AND).append("echo").append(SPACE).append(QUOTE).append(pipeSplit[pipeSplit.length - 1].trim()).append(SEPARATOR).append(QUOTE).append(AND).append(step).append(AND).append("echo");
        }
        combinedCommand.append(AND).append("echo").append(SPACE).append(QUOTE).append("end").append(SEPARATOR).append(QUOTE);


        return combinedCommand.toString();
    }

    public String getCombinedCommandRelease(String projectDir) {
        getSeparator();
        final StringBuilder combinedCommand = new StringBuilder();

        combinedCommand.append("cd src").append(SPACE).append(AND).append("echo").append(SPACE).append(QUOTE).append("start").append(SEPARATOR).append(QUOTE);

        if (hasFile(projectDir + "/prerelease.sh")) {
            combinedCommand.append(AND).append("echo").append(SPACE).append(QUOTE).append("pre-release").append(SEPARATOR).append(QUOTE).append(AND).append("sh prerelease.sh").append(AND).append("echo");
        }

        for (String step : releaseSteps) {
            final String[] pipeSplit = step.split("\\|");
            combinedCommand.append(AND).append("echo").append(SPACE).append(QUOTE).append(pipeSplit[pipeSplit.length - 1].trim()).append(SEPARATOR).append(QUOTE).append(AND).append(step).append(AND).append("echo");
        }

        if (hasFile(projectDir + "/postrelease.sh")) {
            combinedCommand.append(AND).append("echo").append(SPACE).append(QUOTE).append("post-release").append(SEPARATOR).append(QUOTE).append(AND).append("sh postrelease.sh").append(AND).append("echo");
        }

        combinedCommand.append(AND).append("echo").append(SPACE).append(QUOTE).append("end").append(SEPARATOR).append(QUOTE);


        return combinedCommand.toString();
    }
}
