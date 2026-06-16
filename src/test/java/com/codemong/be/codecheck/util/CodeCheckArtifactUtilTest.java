package com.codemong.be.codecheck.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class CodeCheckArtifactUtilTest {

    private final CodeCheckArtifactUtil codeCheckArtifactUtil = new CodeCheckArtifactUtil(new ObjectMapper());

    @Test
    void parseFailedTestsFromJUnitXmlArchive() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <testsuite name="com.codemong.mission.CalculatorTest" tests="2" failures="1" errors="1">
                    <testcase classname="com.codemong.mission.CalculatorTest" name="add">
                        <failure message="expected 3 but was 4"/>
                    </testcase>
                    <testcase classname="com.codemong.mission.CalculatorTest" name="subtract">
                        <error message="boom"/>
                    </testcase>
                    <testcase classname="com.codemong.mission.CalculatorTest" name="multiply"/>
                </testsuite>
                """;

        assertThat(codeCheckArtifactUtil.parseFailedTestsFromJUnitXmlArchive(zip("TEST-CalculatorTest.xml", xml)))
                .containsExactly(
                        "com.codemong.mission.CalculatorTest.add",
                        "com.codemong.mission.CalculatorTest.subtract"
                );
    }

    private byte[] zip(String entryName, String content) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            zipOutputStream.putNextEntry(new ZipEntry(entryName));
            zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
        }
        return outputStream.toByteArray();
    }
}
