package org.toilelibre.libe.curl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/**
 * A generic PEM reader, based on the format outlined in RFC 1421
 */
class PemReader extends BufferedReader {
	private static final String BEGIN = "-----BEGIN ";
	private static final String END = "-----END ";

	PemReader(Reader reader) {
		super(reader);
	}

	PemObject readPemObject() throws IOException {
		String line = readLine();

		while (line != null && !line.startsWith(BEGIN)) {
			line = readLine();
		}

		if (line != null) {
			line = line.substring(BEGIN.length());
			int index = line.indexOf('-');
			String type = line.substring(0, index);

			if (index > 0) {
				return loadObject(type);
			}
		}

		return null;
	}

	private PemObject loadObject(String type) throws IOException {
		String line;
		String endMarker = END + type;
		StringBuilder stringBuffer = new StringBuilder();
		List<PemHeader> headers = new ArrayList<>();

		while ((line = readLine()) != null) {
			if (line.contains(":")) {
				int index = line.indexOf(':');
				String hdr = line.substring(0, index);
				String value = line.substring(index + 1).trim();

				headers.add(new PemHeader(hdr, value));

				continue;
			}

			if (line.contains(endMarker)) {
				break;
			}

			stringBuffer.append(line.trim());
		}

		if (line == null) {
			throw new IOException(endMarker + " not found");
		}

		return new PemObject(type, headers, Base64.getDecoder().decode(stringBuffer.toString()));
	}

	static class PemHeader {
		private final String name;
		private final String value;

		/**
		 * Base constructor.
		 *
		 * @param name
		 *            name of the header property.
		 * @param value
		 *            value of the header property.
		 */
		PemHeader(String name, String value) {
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

	static class PemObject {

		private String type;
		private List<PemHeader> headers;
		private byte[] content;

		/**
		 * Generic constructor for object with headers.
		 *
		 * @param type
		 *            pem object type.
		 * @param headers
		 *            a list of PemHeader objects.
		 * @param content
		 *            the binary content of the object.
		 */
		PemObject(String type, List<PemHeader> headers, byte[] content) {
			this.type = type;
			this.headers = Collections.unmodifiableList(headers);
			this.content = content;
		}

		String getType() {
			return type;
		}

		byte[] getContent() {
			return content;
		}

		List<PemHeader> getHeaders() {
			return headers;
		}

	}
}
