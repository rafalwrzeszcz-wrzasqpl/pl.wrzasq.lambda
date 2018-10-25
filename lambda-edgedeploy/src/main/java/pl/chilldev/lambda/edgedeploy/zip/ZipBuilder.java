/*
 * This file is part of the ChillDev-Lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2018 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.chilldev.lambda.edgedeploy.zip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utility class for building ZIP archive stream.
 */
public class ZipBuilder
{
    /**
     * Internal interface for writing operations.
     */
    @FunctionalInterface
    private interface ContentWriter
    {
        /**
         * Performs write operation.
         *
         * @throws IOException When write to stream fails.
         */
        void write() throws IOException;
    }

    /**
     * Size of bytes buffer for resource processing.
     */
    private static final int BUFFER_SIZE = 1024;

    /**
     * Output stream.
     */
    private ByteArrayOutputStream output = new ByteArrayOutputStream();

    /**
     * ZIP compression stream.
     */
    private ZipOutputStream zip = new ZipOutputStream(this.output);

    /**
     * Creates new ZIP archive entry.
     *
     * @param name Entry name.
     * @param content Binary content of the file.
     * @throws IOException When writing content to archive fails.
     */
    public void writeEntry(String name, byte[] content) throws IOException
    {
        this.writeEntry(name, () -> this.zip.write(content));
    }

    /**
     * Creates new ZIP archive entry.
     *
     * @param name Entry name.
     * @param stream Content source.
     * @throws IOException When writing content to archive fails.
     */
    public void writeEntry(String name, InputStream stream) throws IOException
    {
        this.writeEntry(
            name,
            () -> {
                byte[] buffer = new byte[ZipBuilder.BUFFER_SIZE];
                int count;
                while ((count = stream.read(buffer)) > 0) {
                    this.zip.write(buffer, 0, count);
                }
            }
        );
    }

    /**
     * Creates ZIP entry.
     *
     * @param name Entry name.
     * @param handler Custom writing logic.
     * @throws IOException When writing to stream fails.
     */
    private void writeEntry(String name, ContentWriter handler) throws IOException
    {
        this.zip.putNextEntry(
            new ZipEntry(name)
        );
        handler.write();
        this.zip.closeEntry();
    }

    /**
     * Closes active stream.
     *
     * @return Archive binary content.
     * @throws IOException When dumping ZIP stream fails.
     */
    public ByteBuffer dump() throws IOException
    {
        this.zip.close();
        this.output.close();

        return ByteBuffer.wrap(this.output.toByteArray());
    }
}
