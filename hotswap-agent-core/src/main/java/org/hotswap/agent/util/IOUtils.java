
package org.hotswap.agent.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.logging.AgentLogger;
import org.xml.sax.InputSource;
import sun.nio.ch.ChannelInputStream;


public class IOUtils {
    private static AgentLogger LOGGER = AgentLogger.getLogger(IOUtils.class);



    private static int WAIT_FOR_FILE_MAX_SECONDS = 5;


    public static final String URL_PROTOCOL_FILE = "file";


    public static final String URL_PROTOCOL_VFS = "vfs";


    public static byte[] toByteArray(URI uri) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        InputStream inputStream = null;
        int tryCount = 0;
        while (inputStream == null) {
            try {
                inputStream = uri.toURL().openStream();
            } catch (FileNotFoundException e) {


                if (tryCount > WAIT_FOR_FILE_MAX_SECONDS * 10) {
                    LOGGER.trace("File not found, exiting with exception...", e);
                    throw new IllegalArgumentException(e);
                } else {
                    tryCount++;
                    LOGGER.trace("File not found, waiting...", e);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e1) {
                    }
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        LOGGER.error("Can't close file.", e);
                    }
                }
            }
        }

        try (InputStream stream = uri.toURL().openStream()) {
            byte[] chunk = new byte[4096];
            int bytesRead;

            while ((bytesRead = stream.read(chunk)) > 0) {
                outputStream.write(chunk, 0, bytesRead);
            }

        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        return outputStream.toByteArray();
    }


    public static String streamToString(InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }



    public static boolean isFileURL(URL url) {
        String protocol = url.getProtocol();
        return (URL_PROTOCOL_FILE.equals(protocol) || protocol.startsWith(URL_PROTOCOL_VFS));
    }


    public static boolean isDirectoryURL(URL url) {
        try {
            File f = new File(url.toURI());
            if(f.exists() && f.isDirectory()) {
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }


    public static String urlToClassName(URI uri) throws IOException {
        return ClassPool.getDefault().makeClass(uri.toURL().openStream()).getName();
    }


    public static String extractFileNameFromInputStream(InputStream is) {
        try {
            if (is instanceof ChannelInputStream) {
                ReadableByteChannel ch = (ReadableByteChannel) ReflectionHelper.get(is, "ch");
                return ch instanceof FileChannel ? (String) ReflectionHelper.get(ch, "path") : null;
            }
            while (true) {
                if (is instanceof FileInputStream) {
                    return (String) ReflectionHelper.get(is, "path");
                }
                if (!(is instanceof FilterInputStream)) {
                    break;
                }
                is = (InputStream) ReflectionHelper.get(is, "in");
            }
        } catch (IllegalArgumentException e) {
            LOGGER.error("extractFileNameFromInputStream() failed.", e);
        }
        return null;
    }


    public static String extractFileNameFromReader(Reader reader) {
        try {
            if (reader instanceof InputStreamReader) {
                InputStream is = (InputStream) ReflectionHelper.get(reader, "lock");
                return extractFileNameFromInputStream(is);
            }
        } catch (IllegalArgumentException e) {
            LOGGER.error("extractFileNameFromReader() failed.", e);
        }
        return null;
    }


    public static String extractFileNameFromInputSource(InputSource inputSource) {
        if (inputSource.getByteStream() != null) {
            return extractFileNameFromInputStream(inputSource.getByteStream());
        }
        if (inputSource.getCharacterStream() != null) {
            return extractFileNameFromReader(inputSource.getCharacterStream());
        }
        return null;
    }
}
