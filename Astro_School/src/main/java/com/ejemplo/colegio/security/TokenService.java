package com.ejemplo.colegio.security;

import com.ejemplo.colegio.domain.TipoUsuario;
import com.ejemplo.colegio.support.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class TokenService {
    private final JwtEncoder encoder;
    private final JwtDecoder qrDecoder;
    private final long accesoMinutos;
    private final long qrMinutos;

    public record TokenEmitido(String token, Instant expiraEn) { }

    public TokenService(JwtEncoder encoder, SecretKey key,
            @Value("${app.jwt.expiration-minutes:480}") long accesoMinutos,
            @Value("${app.jwt.qr-expiration-minutes:1440}") long qrMinutos) {
        this.encoder = encoder;
        this.accesoMinutos = accesoMinutos;
        this.qrMinutos = qrMinutos;
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("colegio-local"));
        this.qrDecoder = decoder;
    }

    public TokenEmitido emitirAcceso(TipoUsuario rol, Long usuarioId, long authVersion) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plus(accesoMinutos, ChronoUnit.MINUTES);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("colegio-local").issuedAt(ahora).expiresAt(expira)
                .subject(rol.name() + ":" + usuarioId)
                .claim("roles", List.of(rol.name()))
                .claim("authVersion", authVersion)
                .claim("purpose", "ACCESS")
                .build();
        return new TokenEmitido(codificar(claims), expira);
    }

    public TokenEmitido emitirQr(Long estudianteId, String nombre, String grupo) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plus(qrMinutos, ChronoUnit.MINUTES);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("colegio-local").issuedAt(ahora).expiresAt(expira)
                .subject("ESTUDIANTE:" + estudianteId)
                .claim("purpose", "QR_ESTUDIANTE")
                .claim("nombre", nombre).claim("grupo", grupo)
                .build();
        return new TokenEmitido(codificar(claims), expira);
    }

    public Jwt validarQr(String token) {
        try {
            Jwt jwt = qrDecoder.decode(token);
            if (!"QR_ESTUDIANTE".equals(jwt.getClaimAsString("purpose"))
                    || jwt.getSubject() == null || !jwt.getSubject().startsWith("ESTUDIANTE:"))
                throw new ApiException(HttpStatus.BAD_REQUEST, "El codigo QR no es valido");
            return jwt;
        } catch (JwtException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El QR es invalido o ha expirado");
        }
    }

    private String codificar(JwtClaimsSet claims) {
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
