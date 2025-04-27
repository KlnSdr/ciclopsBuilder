package ciclops.builder;

import common.logger.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import static ciclops.builder.FileUtil.hasFile;
import static ciclops.builder.PipelineConfig.*;

public class Pipeline {
    private static final Logger LOGGER = new Logger(Pipeline.class);

    public void run() {
        getSeparator();
        LOGGER.debug("Starting builder");
        final String scm = System.getenv("SCM_URL");

        if (scm == null || scm.isEmpty()) {
            LOGGER.error("SCM_URL environment variable is not set.");
            LOGGER.error("|CICLOPS_EXIT_CODE:1");
            return;
        }

        LOGGER.debug("SCM URL: " + scm);
        final String checkoutCommand = new StringBuilder()
                .append("echo")
                .append(SPACE)
                .append(QUOTE)
                .append("scm checkout")
                .append(SEPARATOR)
                .append(QUOTE)
                .append(AND)
                .append("git")
                .append(SPACE)
                .append("clone")
                .append(SPACE)
                .append(scm)
                .append(AND)
                .append("echo")
                .toString();

        LOGGER.debug("Executing command: " + checkoutCommand);
        if (!exec(checkoutCommand)) {
            LOGGER.error("Command failed: " + checkoutCommand);
            return;
        }

        final String projectDir = getProjectDir(scm);
        final PipelineConfig pipeline = ProjectValidator.validateAndLoad(projectDir);

        if (pipeline == null) {
            LOGGER.error("Pipeline configuration is invalid or not found.");
            return;
        }
        LOGGER.debug("Pipeline configuration loaded successfully.");

        final String pipelineCommand = pipeline.getCombinedCommand();
        LOGGER.debug(isRelease() ? "|Running release pipeline" : "|Running build pipeline");

        if (isRelease() && hasFile(projectDir + "/prerelease.sh")) {
            LOGGER.debug("|running pre release script");

            getSeparator();
            final String preReleaseCommand = new StringBuilder()
                    .append("echo")
                    .append(SPACE)
                    .append(QUOTE)
                    .append("pre-release")
                    .append(SEPARATOR)
                    .append(QUOTE)
                    .append(AND)
                    .append("sh")
                    .append(SPACE)
                    .append(projectDir)
                    .append("/prerelease.sh")
                    .append(AND)
                    .append("echo")
                    .toString();

            if (!exec(preReleaseCommand)) {
                LOGGER.error("Pre-release script failed.");
                return;
            }
        }

        LOGGER.debug("Executing pipeline command: " + pipelineCommand);

        final String pullSeparatorCommand = new StringBuilder()
                .append("echo")
                .append(SPACE)
                .append(QUOTE)
                .append("pod init")
                .append(SEPARATOR)
                .append(QUOTE)
                .append(AND)
                .append("echo")
                .toString();

        LOGGER.debug("Executing command: " + pullSeparatorCommand);
        exec(pullSeparatorCommand);

        runPipelineImage(pipeline.image(), pipelineCommand, projectDir);

        if (isRelease() && hasFile(projectDir + "/postrelease.sh")) {
            LOGGER.debug("|running post release script");

            getSeparator();
            final String postReleaseCommand = new StringBuilder()
                    .append("echo")
                    .append(SPACE)
                    .append(QUOTE)
                    .append("post-release")
                    .append(SEPARATOR)
                    .append(QUOTE)
                    .append(AND)
                    .append("sh")
                    .append(SPACE)
                    .append(projectDir)
                    .append("/postrelease.sh")
                    .append(AND)
                    .append("echo")
                    .toString();

            if (!exec(postReleaseCommand)) {
                LOGGER.error("Pre-release script failed.");
            }
        }
    }

    private void runPipelineImage(String image, String command, String projectDir) {
        String runCommand = "podman run -v " + projectDir + ":/app/src --rm -it " + image + " sh -c \"" + command + "\"";
        LOGGER.debug("Running pipeline image with command: " + runCommand);
        if (!exec(runCommand)) {
            LOGGER.error("Failed to run pipeline image with command: " + runCommand);
        }
    }

    private String getProjectDir(String scm) {
        String[] parts = scm.split("/");
        String projectName = parts[parts.length - 1].replace(".git", "");
        return "/app/" + projectName;
    }

    private boolean exec(String command) {
        try {
            final ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", command);
            processBuilder.redirectErrorStream(true);
            final Process process = processBuilder.start();

            final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                LOGGER.info("|" + line);
            }

            int exitCode = process.waitFor();
            LOGGER.info("|CICLOPS_EXIT_CODE:" + exitCode);
            if (exitCode != 0) {
                LOGGER.error("Failed to execute command \"" + command + "\". Exit code: " + exitCode);
                return false;
            }
        } catch (Exception e) {
            LOGGER.error("Error executing command: " + command);
            LOGGER.trace(e);
            return false;
        }
        return true;
    }

    private boolean isRelease() {
        LOGGER.debug(System.getProperty("RELEASE"));
        return System.getenv("RELEASE") != null && System.getenv("RELEASE").equalsIgnoreCase("true");
    }
}
