/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2018 - 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.lambda.edgedeploy.zip;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Scanner;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pl.wrzasq.lambda.edgedeploy.zip.ZipBuilder;

public class ZipBuilderTest {
    @Test
    public void writeEntryFromBytes() throws IOException {
        var zip = new ZipBuilder();
        zip.writeEntry("test.txt", new byte[]{'t', 'e', 's', 't'});
        var buffer = zip.dump();

        var stream = new ZipInputStream(new ByteArrayInputStream(buffer.array()));
        var entry = stream.getNextEntry();

        var scanner = new Scanner(stream);

        Assertions.assertEquals(
            "test.txt",
            entry.getName(),
            "ZipBuilder.writeEntry() should set entry filename."
        );
        Assertions.assertEquals(
            "test",
            scanner.next(),
            "ZipBuilder.writeEntry() should write file content."
        );
    }

    @Test
    public void writeEntryFromStream() throws IOException {
        var zip = new ZipBuilder();
        zip.writeEntry("test.txt", new ByteArrayInputStream(new byte[]{'t', 'e', 's', 't'}));
        var buffer = zip.dump();

        var stream = new ZipInputStream(new ByteArrayInputStream(buffer.array()));
        var entry = stream.getNextEntry();

        var scanner = new Scanner(stream);

        Assertions.assertEquals(
            "test.txt",
            entry.getName(),
            "ZipBuilder.writeEntry() should set entry filename."
        );
        Assertions.assertEquals(
            "test",
            scanner.next(),
            "ZipBuilder.writeEntry() should write file content."
        );
    }

    @Test
    public void copyFrom() throws IOException {
        var zip = new ZipBuilder();
        zip.writeEntry("test.txt", new ByteArrayInputStream(new byte[]{'t', 'e', 's', 't'}));
        var buffer = zip.dump();

        var destination = new ZipBuilder();
        destination.copyFrom(new ZipInputStream(new ByteArrayInputStream(buffer.array())));

        var stream = new ZipInputStream(new ByteArrayInputStream(buffer.array()));
        var entry = stream.getNextEntry();

        var scanner = new Scanner(stream);

        Assertions.assertEquals(
            "test.txt",
            entry.getName(),
            "ZipBuilder.copyFrom() should copy entry filename."
        );
        Assertions.assertEquals(
            "test",
            scanner.next(),
            "ZipBuilder.copyFrom() should copy file content."
        );
    }
}
