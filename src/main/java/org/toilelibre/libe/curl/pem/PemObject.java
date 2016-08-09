package org.toilelibre.libe.curl.pem;

import java.util.Collections;
import java.util.List;

/**
 * A generic PEM object - type, header properties, and byte content.
 */
public class PemObject
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
    public PemObject(String type, List headers, byte[] content)
    {
        this.type = type;
        this.headers = Collections.<PemHeader>unmodifiableList(headers);
        this.content = content;
    }

    public String getType()
    {
        return type;
    }

    public byte[] getContent()
    {
        return content;
    }
}
