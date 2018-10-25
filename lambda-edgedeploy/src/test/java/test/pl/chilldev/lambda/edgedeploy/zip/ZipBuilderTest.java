/*
 * This file is part of the ChillDev-Lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2018 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.chilldev.lambda.edgedeploy.zip;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Assert;
import org.junit.Test;
import pl.chilldev.lambda.edgedeploy.zip.ZipBuilder;

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

        Assert.assertEquals("ZipBuilder.writeEntry() should set entry filename.", "test.txt", entry.getName());
        Assert.assertEquals("ZipBuilder.writeEntry() should write file content.", "test", scanner.next());
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

        Assert.assertEquals("ZipBuilder.writeEntry() should set entry filename.", "test.txt", entry.getName());
        Assert.assertEquals("ZipBuilder.writeEntry() should write file content.", "test", scanner.next());
    }
}
