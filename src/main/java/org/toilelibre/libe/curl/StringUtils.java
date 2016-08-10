package org.toilelibre.libe.curl;

class StringUtils {

	static String capitalize(final String str) {
		if (str == null || str.length() == 0) {
			return str;
		}

		final char firstChar = str.charAt(0);
		if (Character.isTitleCase(firstChar)) {
			// already capitalized
			return str;
		}

		return Character.toTitleCase(firstChar) + str.substring(1);
	}

}
