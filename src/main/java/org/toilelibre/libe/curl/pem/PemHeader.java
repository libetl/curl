package org.toilelibre.libe.curl.pem;

/**
 * Class representing a PEM header (name, value) pair.
 */
class PemHeader
{
    private final String name;
    private final String value;

    /**
     * Base constructor.
     *
     * @param name name of the header property.
     * @param value value of the header property.
     */
    PemHeader(String name, String value)
    {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
