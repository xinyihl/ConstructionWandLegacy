package com.xinyihl.constructionwandlegacy.network;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.Assert.assertFalse;

public class CommonSideBoundaryTest {
    private static void assertNoClientImports(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> files = Files.walk(path)) {
                files.filter(file -> file.toString().endsWith(".java")).forEach(CommonSideBoundaryTest::assertSourceIsCommonSafe);
            }
        } else {
            assertSourceIsCommonSafe(path);
        }
    }

    private static void assertSourceIsCommonSafe(Path path) {
        try {
            String source = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            assertFalse(path + " imports net.minecraft.client", source.contains("import net.minecraft.client"));
            assertFalse(path + " imports the mod client package", source.contains("import com.xinyihl.constructionwandlegacy.client"));
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }

    private static void assertNoClientClassLinks(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> files = Files.walk(path)) {
                files.filter(file -> file.toString().endsWith(".class")).forEach(CommonSideBoundaryTest::assertClassIsCommonSafe);
            }
        } else {
            assertClassIsCommonSafe(path);
        }
    }

    private static void assertClassIsCommonSafe(Path path) {
        try {
            String constants = new String(Files.readAllBytes(path), StandardCharsets.ISO_8859_1);
            assertFalse(path + " links net/minecraft/client", constants.contains("net/minecraft/client"));
            assertFalse(path + " links the mod client package", constants.contains("com/xinyihl/constructionwandlegacy/client/"));
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }

    @Test
    public void commonBootstrapNetworkAndConfigDoNotImportClientClasses() throws IOException {
        assertNoClientImports(Paths.get("src/main/java/com/xinyihl/constructionwandlegacy/network"));
        assertNoClientImports(Paths.get("src/main/java/com/xinyihl/constructionwandlegacy/config"));
        assertNoClientImports(Paths.get("src/main/java/com/xinyihl/constructionwandlegacy/proxy/CommonProxy.java"));
        assertNoClientImports(Paths.get("src/main/java/com/xinyihl/constructionwandlegacy/ConstructionWandLegacy.java"));
    }

    @Test
    public void commonClassConstantPoolsDoNotLinkClientClasses() throws IOException {
        Path classes = Paths.get("build/classes/java/main/com/xinyihl/constructionwandlegacy");
        assertNoClientClassLinks(classes.resolve("network"));
        assertNoClientClassLinks(classes.resolve("config"));
        assertNoClientClassLinks(classes.resolve("proxy/CommonProxy.class"));
        assertNoClientClassLinks(classes.resolve("ConstructionWandLegacy.class"));
    }
}
