package ciclops.builder;

import java.io.File;

public class FileUtil {
    public static boolean hasFile(String path) {
        return new File(path).exists() && new File(path).isFile();
    }
}
