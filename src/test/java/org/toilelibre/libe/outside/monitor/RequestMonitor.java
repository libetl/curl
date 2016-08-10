package org.toilelibre.libe.outside.monitor;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.JobExecutionExitCodeGenerator;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootApplication
@EnableWebMvc
@EnableWebSecurity
public class RequestMonitor {
	private static final Logger LOGGER = LoggerFactory.getLogger(RequestMonitor.class);
	private static ConfigurableApplicationContext context;
	private static int managementPort;
	private static int port;

	public static void main(final String[] args) {
		RequestMonitor.start(args);
	}

	public static int[] start() {
		return RequestMonitor.start(new String[0]);
	}

	public static int[] start(final String[] args) {
		return RequestMonitor.start(true, args);
	}

	public static int[] start(final boolean withSsl, final String[] args) {
		final Random random = new Random();
		RequestMonitor.port = random.nextInt(32767) + 32768;
		RequestMonitor.managementPort = random.nextInt(32767) + 32768;
		RequestMonitor.start(RequestMonitor.port, RequestMonitor.managementPort, withSsl, args);
		return new int[] { RequestMonitor.port, RequestMonitor.managementPort };
	}

	public static void start(final int port, final int managementPort, final boolean withSsl, final String[] args) {
		System.setProperty("server.port", String.valueOf(port));
		System.setProperty("managementPort.port", String.valueOf(managementPort));
		if (withSsl) {
			System.setProperty("server.ssl.key-store", "classpath:server/libe/libe.jks");
			System.setProperty("server.ssl.key-store-password", "myserverpass");
			System.setProperty("server.ssl.key-password", "myserverpass");
			System.setProperty("server.ssl.client-auth", "need");
		}

		RequestMonitor.context = SpringApplication.run(RequestMonitor.class, args);
	}

	public static void stop() {
		SpringApplication.exit(RequestMonitor.context, new JobExecutionExitCodeGenerator());
	}

	public static int port() {
		return RequestMonitor.port;
	}

	@Configuration
	@EnableWebSecurity
	static class WebSecurityConfig extends WebSecurityConfigurerAdapter {
		@Override
		public void configure(final WebSecurity http) throws Exception {
			http.ignoring().antMatchers("/public/**");
		}

		@Override
		protected void configure(final HttpSecurity http) throws Exception {
			http.authorizeRequests().antMatchers("/private").permitAll().anyRequest().authenticated().and().httpBasic()
					.realmName("basic").and().logout().permitAll();
		}

		@Autowired
		public void configureGlobal(final AuthenticationManagerBuilder auth) throws Exception {
			auth.inMemoryAuthentication().withUser("user").password("password").roles("USER");
		}
	}

	@Controller
	@RequestMapping("/**")
	static class MonitorController {

		@RequestMapping(value = "/public/redirection", produces = MediaType.TEXT_PLAIN_VALUE)
		@ResponseStatus(code = HttpStatus.FOUND)
		@ResponseBody
		public String redirection(final HttpServletRequest request, final HttpServletResponse response,
				@RequestBody(required = false) final String body) {
			response.setHeader("Location", this.serverLocation(request) + "/public/redirectedThere");
			this.logRequest(request, body);
			return "";
		}

		@RequestMapping(value = "/public/unauthorized", produces = MediaType.TEXT_PLAIN_VALUE)
		@ResponseStatus(code = HttpStatus.UNAUTHORIZED)
		@ResponseBody
		public String unauthorized(final HttpServletRequest request, final HttpServletResponse response,
				@RequestBody(required = false) final String body) {
			response.setHeader("Location", this.serverLocation(request) + "/public/tryagain");
			this.logRequest(request, body);
			return "";
		}

		@RequestMapping(value = "/private/login", produces = MediaType.TEXT_PLAIN_VALUE)
		@ResponseStatus(code = HttpStatus.FOUND)
		@ResponseBody
		public String login(final HttpServletRequest request, final HttpServletResponse response,
				@RequestBody(required = false) final String body, final Authentication auth) {
			response.setHeader("Location", this.serverLocation(request) + "/private/logged");
			this.logRequest(request, body);
			return "";
		}

		@RequestMapping(produces = MediaType.TEXT_PLAIN_VALUE)
		@ResponseStatus(code = HttpStatus.OK)
		@ResponseBody
		public String receiveRequest(final HttpServletRequest request,
				@RequestBody(required = false) final String body) {
			return this.logRequest(request, body);
		}

		@RequestMapping(value = "/public/json", produces = MediaType.TEXT_PLAIN_VALUE)
		@ResponseStatus(code = HttpStatus.OK)
		@ResponseBody
		public String json(final HttpServletRequest request, @RequestBody(required = true) final String body)
				throws JsonParseException, JsonMappingException, IOException {
			@SuppressWarnings("unchecked")
			final Map<String, Object> map = new ObjectMapper().readValue(body, Map.class);
			RequestMonitor.LOGGER.info(map.toString());
			return this.logRequest(request, body);
		}

		private String logRequest(final HttpServletRequest request, final String body) {
			final StringBuffer curlLog = new StringBuffer("curl");

			curlLog.append(" -k ");
			curlLog.append("--cert-type P12 --cert src/test/resources/clients/libe/libe.p12:mylibepass");
			curlLog.append(" -X ");
			curlLog.append(request.getMethod());

			for (final Enumeration<String> headerNameEnumeration = request.getHeaderNames(); headerNameEnumeration
					.hasMoreElements();) {
				final String headerName = headerNameEnumeration.nextElement();
				final String headerValue = request.getHeader(headerName);
				curlLog.append(" -H '");
				curlLog.append(headerName);
				curlLog.append(": ");
				curlLog.append(headerValue);
				curlLog.append("'");

			}

			if (body != null) {
				curlLog.append(" -d '");
				curlLog.append(body.replace("'", "''"));
				curlLog.append("'");
			}

			curlLog.append(" ");
			curlLog.append(" '");
			curlLog.append(this.serverLocation(request) + request.getServletPath()
					+ (request.getQueryString() == null ? "" : "?" + request.getQueryString()));
			curlLog.append("'");
			RequestMonitor.LOGGER.info(curlLog.toString());
			return curlLog.toString();
		}

		private String serverLocation(final HttpServletRequest request) {
			return request.getScheme() + "://" + request.getServerName() + ":" + RequestMonitor.port();
		}
	}
}