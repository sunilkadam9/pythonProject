pom.xml:

<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"> <modelVersion>4.0.0</modelVersion> <groupId>com.example</groupId> <artifactId>tar-diff-report</artifactId> <version>1.0-SNAPSHOT</version> <dependencies> <dependency> <groupId>org.apache.commons</groupId> <artifactId>commons-compress</artifactId> <version>1.26.1</version> </dependency> <dependency> <groupId>commons-io</groupId> <artifactId>commons-io</artifactId> <version>2.15.1</version> </dependency> </dependencies> </project>

src/main/java/com/example/TarGzDiffReport.java:

package com.example;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry; import org.apache.commons.compress.archivers.tar.TarArchiveInputStream; import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream; import org.apache.commons.io.FileUtils; import org.apache.commons.io.IOUtils;

import java.io.; import java.nio.file.; import java.util.*;

public class TarGzDiffReport {

public static void main(String[] args) throws IOException {
    File tarGz1 = new File("path/to/first.tar.gz");
    File tarGz2 = new File("path/to/second.tar.gz");

    File tempDir1 = Files.createTempDirectory("tar1").toFile();
    File tempDir2 = Files.createTempDirectory("tar2").toFile();

    extractTarGz(tarGz1, tempDir1);
    extractTarGz(tarGz2, tempDir2);

    DiffGenerator diffGenerator = new DiffGenerator();
    diffGenerator.compareDirectories(tempDir1, tempDir2, new File("diff-report.html"));

    FileUtils.deleteDirectory(tempDir1);
    FileUtils.deleteDirectory(tempDir2);
}

public static void extractTarGz(File tarGzFile, File outputDir) throws IOException {
    try (FileInputStream fis = new FileInputStream(tarGzFile);
         BufferedInputStream bis = new BufferedInputStream(fis);
         GzipCompressorInputStream gis = new GzipCompressorInputStream(bis);
         TarArchiveInputStream tarInput = new TarArchiveInputStream(gis)) {

        TarArchiveEntry entry;
        while ((entry = tarInput.getNextTarEntry()) != null) {
            File outFile = new File(outputDir, entry.getName());
            if (entry.isDirectory()) {
                outFile.mkdirs();
            } else {
                outFile.getParentFile().mkdirs();
                try (OutputStream out = new FileOutputStream(outFile)) {
                    IOUtils.copy(tarInput, out);
                }
            }
        }
    }
}

}

// You would include your previous DiffGenerator class here as well // which compares the extracted directories and creates HTML diff.
