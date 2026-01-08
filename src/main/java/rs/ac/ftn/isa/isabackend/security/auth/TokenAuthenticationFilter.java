package rs.ac.ftn.isa.isabackend.security.auth;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.ExpiredJwtException;
import rs.ac.ftn.isa.isabackend.security.TokenUtils;

public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private TokenUtils tokenUtils;
    private UserDetailsService userDetailsService;

    protected final Log LOGGER = LogFactory.getLog(getClass());

    public TokenAuthenticationFilter(TokenUtils tokenHelper, UserDetailsService userDetailsService) {
        this.tokenUtils = tokenHelper;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        LOGGER.info("=== FILTER START === " + request.getMethod() + " " + request.getRequestURI());

        String username;
        String authToken = tokenUtils.getToken(request);

        try {
            if (authToken != null) {
                LOGGER.info("Token found: " + authToken.substring(0, 20) + "...");
                username = tokenUtils.getUsernameFromToken(authToken);
                LOGGER.info("Username from token: " + username);

                if (username != null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    LOGGER.info("User loaded: " + userDetails.getUsername());

                    if (tokenUtils.validateToken(authToken, userDetails)) {
                        LOGGER.info("Token is valid!");
                        TokenBasedAuthentication authentication = new TokenBasedAuthentication(userDetails);
                        authentication.setToken(authToken);
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } else {
                        LOGGER.warn("Token validation failed!");
                    }
                }
            }
        } catch (ExpiredJwtException ex) {
            LOGGER.error("Token expired!", ex);
        } catch (Exception ex) {
            LOGGER.error("Token validation failed: " + ex.getMessage(), ex);
        }

        chain.doFilter(request, response);
    }
}
