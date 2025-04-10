import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class ZipDiffToHTML {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java ZipDiffToHTML <zip1> <zip2>");
            return;
        }

        Path tempDir1 = Files.createTempDirectory("zip1_");
        Path tempDir2 = Files.createTempDirectory("zip2_");

        unzip(Paths.get(args[0]), tempDir1);
        unzip(Paths.get(args[1]), tempDir2);

        List<String> html = new ArrayList<>();
        html.add("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>ZIP Diff Report</title>");
        html.add("<style>");
        html.add("body { font-family: monospace; background: #f8f9fa; padding: 20px; }");
        html.add(".added { background-color: #d4edda; }");
        html.add(".removed { background-color: #f8d7da; }");
        html.add(".changed { background-color: #fff3cd; }");
        html.add("pre { padding: 10px; border: 1px solid #ccc; border-radius: 6px; background: #fff; }");
        html.add("</style></head><body>");
        html.add("<h1>ZIP Diff Report</h1>");

        compareDirs(tempDir1, tempDir2, tempDir1.toString(), html);

        html.add("</body></html>");
        Files.write(Paths.get("zip-diff-report.html"), html);
        System.out.println("Diff report generated: zip-diff-report.html");
    }

    private static void unzip(Path zipPath, Path destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path filePath = destDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    Files.createDirectories(filePath.getParent());
                    Files.copy(zis, filePath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void compareDirs(Path dir1, Path dir2, String baseDir, List<String> html) throws IOException {
        Files.walk(dir1).forEach(path1 -> {
            try {
                Path relative = dir1.relativize(path1);
                Path path2 = dir2.resolve(relative);
                if (Files.isDirectory(path1)) return;

                if (Files.exists(path2)) {
                    if (isTextFile(path1)) {
                        List<String> lines1 = Files.readAllLines(path1);
                        List<String> lines2 = Files.readAllLines(path2);
                        if (!lines1.equals(lines2)) {
                            html.add("<h2>Changed: " + relative + "</h2><pre>");
                            html.addAll(generateDiffHTML(lines1, lines2));
                            html.add("</pre>");
                        }
                    }
                } else {
                    html.add("<h2>Removed: " + relative + "</h2><pre class='removed'>");
                    Files.readAllLines(path1).forEach(line -> html.add("- " + escapeHtml(line)));
                    html.add("</pre>");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        Files.walk(dir2).forEach(path2 -> {
            try {
                Path relative = dir2.relativize(path2);
                Path path1 = dir1.resolve(relative);
                if (Files.isDirectory(path2)) return;

                if (!Files.exists(path1)) {
                    html.add("<h2>Added: " + relative + "</h2><pre class='added'>");
                    Files.readAllLines(path2).forEach(line -> html.add("+ " + escapeHtml(line)));
                    html.add("</pre>");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static boolean isTextFile(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        return name.endsWith(".txt") || name.endsWith(".java") || name.endsWith(".xml") || name.endsWith(".json") || name.endsWith(".yaml");
    }

    private static List<String> generateDiffHTML(List<String> oldLines, List<String> newLines) {
        List<String> html = new ArrayList<>();
        int i = 0, j = 0;

        while (i < oldLines.size() || j < newLines.size()) {
            String line1 = i < oldLines.size() ? oldLines.get(i) : null;
            String line2 = j < newLines.size() ? newLines.get(j) : null;

            if (line1 != null && line2 != null && line1.equals(line2)) {
                html.add("  " + String.format("%4d", i + 1) + "  " + escapeHtml(line1));
                i++;
                j++;
            } else if (line1 != null && (line2 == null || !newLines.subList(j, Math.min(j + 3, newLines.size())).contains(line1))) {
                html.add("<span class='removed'>- " + String.format("%4d", i + 1) + "  " + escapeHtml(line1) + "</span>");
                i++;
            } else if (line2 != null && (line1 == null || !oldLines.subList(i, Math.min(i + 3, oldLines.size())).contains(line2))) {
                html.add("<span class='added'>+ " + String.format("%4d", j + 1) + "  " + escapeHtml(line2) + "</span>");
                j++;
            } else {
                html.add("<span class='removed'>- " + String.format("%4d", i + 1) + "  " + escapeHtml(line1) + "</span>");
                html.add("<span class='added'>+ " + String.format("%4d", j + 1) + "  " + escapeHtml(line2) + "</span>");
                i++;
                j++;
            }
        }

        return html;
    }

    private static String escapeHtml(String line) {
        return line.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
