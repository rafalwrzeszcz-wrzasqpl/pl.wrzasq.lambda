/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2018 - 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.lambda.edgedeploy.zip;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pl.wrzasq.lambda.edgedeploy.zip.ZipBuilder;

public class ZipBuilderTest
{
    @Test
    public void writeEntryFromBytes() throws IOException
    {
        ZipBuilder zip = new ZipBuilder();
        zip.writeEntry("test.txt", new byte[]{'t', 'e', 's', 't'});
        ByteBuffer buffer = zip.dump();

        ZipInputStream stream = new ZipInputStream(new ByteArrayInputStream(buffer.array()));
        ZipEntry entry = stream.getNextEntry();

        Scanner scanner = new Scanner(stream);

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
    public void writeEntryFromStream() throws IOException
    {
        ZipBuilder zip = new ZipBuilder();
        zip.writeEntry("test.txt", new ByteArrayInputStream(new byte[]{'t', 'e', 's', 't'}));
        ByteBuffer buffer = zip.dump();

        ZipInputStream stream = new ZipInputStream(new ByteArrayInputStream(buffer.array()));
        ZipEntry entry = stream.getNextEntry();

        Scanner scanner = new Scanner(stream);

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
}
