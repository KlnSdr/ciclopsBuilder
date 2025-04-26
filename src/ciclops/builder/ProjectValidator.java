package ciclops.builder;

import common.logger.Logger;
import dobby.exceptions.MalformedJsonException;
import dobby.util.json.NewJson;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static ciclops.builder.FileUtil.hasFile;

public class ProjectValidator {
    private static final Logger LOGGER = new Logger(ProjectValidator.class);

    public static PipelineConfig validateAndLoad(String projectDir) {
        final String configFilePath = projectDir + "/pipeline.json";

        if (!hasFile(configFilePath)) {
            throw new IllegalArgumentException("Pipeline configuration file not found: " + configFilePath);
        }

        final String content = readFile(projectDir, "pipeline.json");

        final NewJson json;
        try {
            json = NewJson.parse(content);
        } catch (MalformedJsonException e) {
            LOGGER.error("Failed to parse pipeline.json");
            LOGGER.trace(e);
            return null;
        }

        if (!PipelineConfig.isValid(json)) {
            LOGGER.error("Invalid pipeline.json");
            return null;
        }

        return PipelineConfig.load(json);
    }

    private static String readFile(String path, String fileName) {
        final String content;
        try {
            final File file = new File(path, fileName);
            FileInputStream fileInputStream = new FileInputStream(file);

            // Read all bytes from the file into a byte array
            content = new String(fileInputStream.readAllBytes());

            // Close the resource
            fileInputStream.close();
            return content;
        } catch (IOException e) {
            LOGGER.error("Failed to read file " + fileName + " in " + path);
            LOGGER.trace(e);
            return null;
        }
    }
}
