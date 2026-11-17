package com.mindspace.security;

import com.mindspace.entity.SysUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    // 定义密钥 和 有效期
    private final SecretKey key;
    private final long expiration;

    // 初始化密钥 和 有效期
    // 从application.properties中获取密钥 和 有效期 在spring启动之前
    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    // 签发生成token
    public String generateToken(SysUser user) {
        //date为当前时间
        Date now = new Date();
        //构建token
        return Jwts.builder()
                //用userId这个字段作为标识
                .subject(String.valueOf(user.getId()))
                //在预载payload中存储一些信息 例如用户名 邮箱
                .claim("username", user.getUsername())
                .claim("email", user.getEmail())
                //签发时间
                .issuedAt(now)
                //过期时间 = 签发时间 + 有效期（单位毫秒）
                .expiration(new Date(now.getTime() + expiration))
                //签名
                .signWith(key)
                //转换成token
                .compact();
    }

    // 解析或者验证token
    //拿到token 然后用签名解析出来 然后验证 然后取出来payload中的userId
    public Long getUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)//验证是否过期
                .getPayload();//去拿userId（为的就是防止你有秘钥 但是改id 变成管理员）
        return Long.valueOf(claims.getSubject());
    }

    public long getExpiration() {
        return expiration;
    }
}