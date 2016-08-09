package org.toilelibre.libe.curl;

import java.util.Collections;
import java.util.List;

/**
 * A generic PEM object - type, header properties, and byte content.
 */
class PemObject
{

    private String           type;
    private List<PemHeader>  headers;
    private byte[]           content;

    /**
     * Generic constructor for object with headers.
     *
     * @param type pem object type.
     * @param headers a list of PemHeader objects.
     * @param content the binary content of the object.
     */
    PemObject(String type, List<PemHeader> headers, byte[] content)
    {
        this.type = type;
        this.headers = Collections.unmodifiableList(headers);
        this.content = content;
    }

    String getType()
    {
        return type;
    }

    byte[] getContent()
    {
        return content;
    }
}
