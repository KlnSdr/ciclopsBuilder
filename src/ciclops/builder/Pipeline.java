package ciclops.builder;

import common.logger.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Pipeline {
    private static final Logger LOGGER = new Logger(Pipeline.class);

    public void run() {
        final String scm = "https://github.com/KlnSdr/test-pipeline.git";
        LOGGER.debug("Starting builder");
        LOGGER.debug("SCM URL: " + scm);
        final String[] prePipelineCommands = {"git clone " + scm, "ls -la " + getProjectDir(scm), "podman image ls"};

        for (String command : prePipelineCommands) {
            LOGGER.debug("Executing command: " + command);
            if (!exec(command)) {
                LOGGER.error("Command failed: " + command);
                return;
            }
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
                LOGGER.debug(line);
            }

            int exitCode = process.waitFor();
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
}
