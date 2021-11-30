/**
 *    Copyright 2020-present  Shanghai Jinmu Information Technology Co., Ltd.
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the Server Side Public License, version 1,
 *    as published by Shanghai Jinmu Information Technology Co., Ltd.(The name of the development team is Whaleal.)
 *
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    Server Side Public License for more details.
 *
 *    You should have received a copy of the Server Side Public License
 *    along with this program. If not, see
 *    <http://www.whaleal.com/licensing/server-side-public-license>.
 *
 *    As a special exception, the copyright holders give permission to link the
 *    code of portions of this program with the OpenSSL library under certain
 *    conditions as described in each individual source file and distribute
 *    linked combinations including the program with the OpenSSL library. You
 *    must comply with the Server Side Public License in all respects for
 *    all of the code used other than as permitted herein. If you modify file(s)
 *    with this exception, you may extend this exception to your version of the
 *    file(s), but you are not obligated to do so. If you do not wish to do so,
 *    delete this exception statement from your version. If you delete this
 *    exception statement from all source files in the program, then also delete
 *    it in the license file.
 */
package com.whaleal.mars.gridfs;


import com.whaleal.mars.util.ResourceUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

@Slf4j
public abstract class AbstractResource implements Resource {

    /**
     * This implementation checks whether a File can be opened,
     * falling back to whether an InputStream can be opened.
     * This will cover both directories and content resources.
     */
    @Override
    public boolean exists() {
        // Try file existence: can we find the file in the file system?
        if (isFile()) {
            try {
                return getFile().exists();
            } catch (IOException ex) {
                if (log.isDebugEnabled()) {
                    log.debug("Could not retrieve File for existence check of " + getDescription(), ex);
                }
            }
        }
        // Fall back to stream existence: can we open the stream?
        try {
            getInputStream().close();
            return true;
        } catch (Throwable ex) {
            if (log.isDebugEnabled()) {
                log.debug("Could not retrieve InputStream for existence check of " + getDescription(), ex);
            }
            return false;
        }
    }

    /**
     * This implementation always returns {@code true} for a resource
     * that {@link #exists() exists} (revised as of 5.1).
     */
    @Override
    public boolean isReadable() {
        return exists();
    }

    /**
     * This implementation always returns {@code false}.
     */
    @Override
    public boolean isOpen() {
        return false;
    }

    /**
     * This implementation always returns {@code false}.
     */
    @Override
    public boolean isFile() {
        return false;
    }

    /**
     * This implementation throws a FileNotFoundException, assuming
     * that the resource cannot be resolved to a URL.
     */
    @Override
    public URL getURL() throws IOException {
        throw new FileNotFoundException(getDescription() + " cannot be resolved to URL");
    }

    /**
     * This implementation builds a URI based on the URL returned
     * by {@link #getURL()}.
     */
    @Override
    public URI getURI() throws IOException {
        URL url = getURL();
        try {
            return ResourceUtils.toURI(url);
        } catch (URISyntaxException ex) {
            throw new IOException("Invalid URI [" + url + "]", ex);
        }
    }

    /**
     * This implementation throws a FileNotFoundException, assuming
     * that the resource cannot be resolved to an absolute file path.
     */
    @Override
    public File getFile() throws IOException {
        throw new FileNotFoundException(getDescription() + " cannot be resolved to absolute file path");
    }

    /**
     * with the result of {@link #getInputStream()}.
     * <p>This is the same as in {@link Resource}'s corresponding default method
     * but mirrored here for efficient JVM-level dispatching in a class hierarchy.
     */
    @Override
    public ReadableByteChannel readableChannel() throws IOException {
        return Channels.newChannel(getInputStream());
    }

    /**
     * This method reads the entire InputStream to determine the content length.
     * <p>For a custom sub-class of {@code InputStreamResource}, we strongly
     * recommend overriding this method with a more optimal implementation, e.g.
     * checking File length, or possibly simply returning -1 if the stream can
     * only be read once.
     *
     * @see #getInputStream()
     */
    @Override
    public long contentLength() throws IOException {
        InputStream is = getInputStream();
        try {
            long size = 0;
            byte[] buf = new byte[256];
            int read;
            while ((read = is.read(buf)) != -1) {
                size += read;
            }
            return size;
        } finally {
            try {
                is.close();
            } catch (IOException ex) {
                if (log.isDebugEnabled()) {
                    log.debug("Could not close content-length InputStream for " + getDescription(), ex);
                }
            }
        }
    }

    /**
     * This implementation checks the timestamp of the underlying File,
     * if available.
     *
     * @see #getFileForLastModifiedCheck()
     */
    @Override
    public long lastModified() throws IOException {
        File fileToCheck = getFileForLastModifiedCheck();
        long lastModified = fileToCheck.lastModified();
        if (lastModified == 0L && !fileToCheck.exists()) {
            throw new FileNotFoundException(getDescription() +
                    " cannot be resolved in the file system for checking its last-modified timestamp");
        }
        return lastModified;
    }

    /**
     * Determine the File to use for timestamp checking.
     * <p>The default implementation delegates to {@link #getFile()}.
     *
     * @return the File to use for timestamp checking (never {@code null})
     * @throws FileNotFoundException if the resource cannot be resolved as
     *                               an absolute file path, i.e. is not available in a file system
     * @throws IOException           in case of general resolution/reading failures
     */
    protected File getFileForLastModifiedCheck() throws IOException {
        return getFile();
    }

    /**
     * This implementation throws a FileNotFoundException, assuming
     * that relative resources cannot be created for this resource.
     */
    @Override
    public Resource createRelative(String relativePath) throws IOException {
        throw new FileNotFoundException("Cannot create a relative resource for " + getDescription());
    }

    /**
     * This implementation always returns {@code null},
     * assuming that this resource type does not have a filename.
     */
    @Override
    
    public String getFilename() {
        return null;
    }


    /**
     * This implementation compares description strings.
     *
     * @see #getDescription()
     */
    @Override
    public boolean equals( Object other) {
        return (this == other || (other instanceof Resource &&
                ((Resource) other).getDescription().equals(getDescription())));
    }

    /**
     * This implementation returns the description's hash code.
     *
     * @see #getDescription()
     */
    @Override
    public int hashCode() {
        return getDescription().hashCode();
    }

    /**
     * This implementation returns the description of this resource.
     *
     * @see #getDescription()
     */
    @Override
    public String toString() {
        return getDescription();
    }

}
