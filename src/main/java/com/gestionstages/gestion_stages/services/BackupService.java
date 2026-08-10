package com.gestionstages.gestion_stages.services;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
public class BackupService {

    public record RestoreResult(Path restoredBackup, Path safetyBackup) {
    }

    private final JdbcTemplate jdbcTemplate;
    private final Path backupDirectory = Paths.get("data", "backups").toAbsolutePath().normalize();

    public BackupService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Path create(String requestedName) throws Exception {
        Files.createDirectories(backupDirectory);
        String base = requestedName == null ? "" : requestedName.trim()
                .replaceAll("[^a-zA-Z0-9._-]", "-");
        if (base.isBlank()) {
            base = "gestion-stages";
        }
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path target = backupDirectory.resolve(base + "-" + timestamp + ".zip").normalize();
        requireInsideDirectory(target);
        String sqlPath = target.toString().replace("\\", "/").replace("'", "''");
        jdbcTemplate.execute("SCRIPT TO '" + sqlPath + "' COMPRESSION ZIP");
        return target;
    }

    public List<Path> list() throws Exception {
        Files.createDirectories(backupDirectory);
        try (var files = Files.list(backupDirectory)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".zip"))
                    .sorted(Comparator.comparing(this::lastModified).reversed())
                    .toList();
        }
    }

    public Path get(String fileName) {
        Path path = backupDirectory.resolve(fileName).normalize();
        requireInsideDirectory(path);
        return path;
    }

    public void delete(String fileName) throws Exception {
        Files.deleteIfExists(get(fileName));
    }

    public synchronized RestoreResult restore(String fileName) throws Exception {
        Path source = get(fileName);
        validate(source);
        Path safety = create("avant-restauration");
        try {
            runScript(source);
            return new RestoreResult(source, safety);
        } catch (Exception restoreFailure) {
            try {
                runScript(safety);
            } catch (Exception rollbackFailure) {
                restoreFailure.addSuppressed(rollbackFailure);
            }
            throw restoreFailure;
        }
    }

    private void runScript(Path source) {
        String sqlPath = source.toString().replace("\\", "/").replace("'", "''");
        jdbcTemplate.execute("DROP ALL OBJECTS");
        jdbcTemplate.execute("RUNSCRIPT FROM '" + sqlPath + "' COMPRESSION ZIP");
    }

    private void validate(Path source) throws Exception {
        if (!Files.isRegularFile(source) || Files.size(source) == 0) {
            throw new IllegalArgumentException("Archive de sauvegarde absente ou vide");
        }
        try (ZipFile zip = new ZipFile(source.toFile())) {
            ZipEntry script = zip.getEntry("script.sql");
            if (script == null || script.isDirectory()) {
                throw new IllegalArgumentException("Archive H2 invalide : script.sql absent");
            }
            long declaredSize = script.getSize();
            if (declaredSize > 250L * 1024 * 1024) {
                throw new IllegalArgumentException("Archive trop volumineuse");
            }
            try (var input = zip.getInputStream(script)) {
                byte[] header = input.readNBytes(4096);
                String sqlHeader = new String(header, java.nio.charset.StandardCharsets.UTF_8)
                        .toUpperCase();
                if (!sqlHeader.contains("CREATE USER")
                        && !sqlHeader.contains("CREATE TABLE")
                        && !sqlHeader.contains("SET DB_CLOSE_DELAY")) {
                    throw new IllegalArgumentException("Le contenu SQL de l'archive est invalide");
                }
            }
        }
    }

    private void requireInsideDirectory(Path path) {
        if (!path.startsWith(backupDirectory)) {
            throw new IllegalArgumentException("Nom de sauvegarde invalide");
        }
    }

    private long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (Exception exception) {
            return 0;
        }
    }
}
