package org.adnan.travner.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;

/** Maps access-token claims to the application's {@link UserPrincipal}. */
public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String userId = jwt.getSubject();
        String username = jwt.getClaimAsString("username");
        String email = jwt.getClaimAsString("email");
        List<String> roles = jwt.getClaimAsStringList("roles");

        UserPrincipal principal = new UserPrincipal(userId, username, null, email, roles, true);
        Collection<? extends GrantedAuthority> authorities = principal.getAuthorities();
        return new UsernamePasswordAuthenticationToken(principal, jwt.getTokenValue(), authorities);
    }
}
