package com.dipcoin.api.request;

import com.dipcoin.api.filter.HttpServletContext;
import io.micrometer.core.annotation.Timed;
import javax.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

// @EnableAsync
@Timed
public abstract class RequestHandler {

	private static final Logger LOG = LogManager.getLogger(RequestHandler.class);

	@Autowired
	@Lazy
	private HttpServletContext httpServletContext;

	protected final void dumpHttpServletRequest() {
		HttpServletRequest servletRequest = httpServletContext.getServletRequest();
		StringBuilder sb = new StringBuilder();
		sb.append(" [");
		sb.append("\n\t request.getContextPath(): ").append(servletRequest.getContextPath());
		sb.append("\n\t request.getLocalAddr(): ").append(servletRequest.getLocalAddr());
		sb.append("\n\t request.getLocalName(): ").append(servletRequest.getLocalName());
		sb.append("\n\t request.getLocalPort(): ").append(servletRequest.getLocalPort());
		sb.append("\n\t request.getPathInfo(): ").append(servletRequest.getPathInfo());
		sb.append("\n\t request.getPathTranslated(): ").append(servletRequest.getPathTranslated());
		sb.append("\n\t request.getProtocol(): ").append(servletRequest.getProtocol());
		sb.append("\n\t request.getRemoteAddr(): ").append(servletRequest.getRemoteAddr());
		sb.append("\n\t request.getRemoteHost(): ").append(servletRequest.getRemoteHost());
		sb.append("\n\t request.getRemotePort(): ").append(servletRequest.getRemotePort());
		sb.append("\n\t request.getRequestURI(): ").append(servletRequest.getRequestURI());
		sb.append("\n\t request.getRequestURL(): ").append(servletRequest.getRequestURL());
		sb.append("\n\t request.getScheme(): ").append(servletRequest.getScheme());
		sb.append("\n\t request.getServerName(): ").append(servletRequest.getServerName());
		sb.append("\n\t request.getServerPort(): ").append(servletRequest.getServerPort());
		sb.append("\n\t request.getServletPath(): ").append(servletRequest.getServletPath());
		sb.append("\n]");

		LOG.debug(sb.toString());
	}
}
