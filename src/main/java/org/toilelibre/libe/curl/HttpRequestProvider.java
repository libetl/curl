package org.toilelibre.libe.curl;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.cli.CommandLine;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.message.BasicHeader;
import org.toilelibre.libe.curl.Curl.CurlException;

class HttpRequestProvider {

	static HttpUriRequest prepareRequest(final CommandLine commandLine) throws CurlException {

		final HttpUriRequest request = HttpRequestProvider.getBuilder(commandLine);

		setData(commandLine, request);

		setForm(commandLine, request);

		setHeaders(commandLine, request);

		return request;

	}

	private static void setForm(CommandLine commandLine, HttpUriRequest request) {
		final String[] forms = Optional.ofNullable(commandLine.getOptionValues(Arguments.FORM.getOpt()))
				.orElse(new String[0]);

		if (forms.length == 0) {
			return;
		}

		final MultipartEntityBuilder multiPartBuilder = MultipartEntityBuilder.create();

		Arrays.stream(forms)
				.forEach(arg -> multiPartBuilder.addTextBody(
						arg.indexOf('=') == -1 ? "data" : arg.substring(0, arg.indexOf('=')),
						arg.indexOf('=') == -1 ? arg : arg.substring(arg.indexOf('=') + 1)));
		((HttpEntityEnclosingRequest) request).setEntity(multiPartBuilder.build());

	}

	private static void setHeaders(final CommandLine commandLine, final HttpUriRequest request) {
		final String[] headers = Optional.ofNullable(commandLine.getOptionValues(Arguments.HEADER.getOpt()))
				.orElse(new String[0]);
		Arrays.stream(headers).map(optionAsString -> optionAsString.split(":"))
				.map(optionAsArray -> new BasicHeader(optionAsArray[0].trim().replaceAll("^\"", "")
						.replaceAll("\\\"$", "").replaceAll("^\\'", "").replaceAll("\\'$", ""),
						optionAsArray[1].trim()))
				.forEach(request::addHeader);

		if (commandLine.hasOption(Arguments.USER_AGENT.getOpt())) {
			request.addHeader("User-Agent", commandLine.getOptionValue(Arguments.USER_AGENT.getOpt()));
		}
	}

	private static void setData(final CommandLine commandLine, final HttpUriRequest request) {
		if (commandLine.hasOption(Arguments.DATA.getOpt()) && request instanceof HttpEntityEnclosingRequest) {
			try {
				((HttpEntityEnclosingRequest) request)
						.setEntity(new StringEntity(commandLine.getOptionValue(Arguments.DATA.getOpt()).toString()));
			} catch (final UnsupportedEncodingException e) {
				throw new CurlException(e);
			}
		}
	}

	private static HttpUriRequest getBuilder(final CommandLine cl) throws CurlException {
		try {
			final String method = cl.getOptionValue(Arguments.HTTP_METHOD.getOpt()) == null ? "GET"
					: cl.getOptionValue(Arguments.HTTP_METHOD.getOpt());
			return (HttpUriRequest) Class
					.forName(HttpRequestBase.class.getPackage().getName() + ".Http"
							+ StringUtils.capitalize(method.toLowerCase().replaceAll("[^a-z]", "")))
					.getConstructor(URI.class).newInstance(new URI(cl.getArgs()[0]));
		} catch (IllegalAccessException | IllegalArgumentException | SecurityException | IllegalStateException
				| InstantiationException | ClassNotFoundException | InvocationTargetException | NoSuchMethodException
				| URISyntaxException e) {
			throw new CurlException(e);
		}
	}
}
