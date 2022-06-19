package com.example.item.config;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    // http.authorizeRequests().antMatchers(HttpMethod.GET, "/**").authenticated().anyRequest()
    // .authenticated();
    // http.oauth2ResourceServer().jwt();
    http.cors().configurationSource(getCorsConfigurationSource());
  }

  private CorsConfigurationSource getCorsConfigurationSource() {
    CorsConfiguration corsConfiguration = new CorsConfiguration();
    // TODO: 修正が必須
    // 全てのメソッドを許可
    corsConfiguration.addAllowedMethod(CorsConfiguration.ALL);
    // 全てのヘッダを許可
    corsConfiguration.addAllowedHeader(CorsConfiguration.ALL);
    // 全てのオリジンを許可
    corsConfiguration.addAllowedOrigin(CorsConfiguration.ALL);

    UrlBasedCorsConfigurationSource corsSource = new UrlBasedCorsConfigurationSource();
    // パスごとに設定が可能。ここでは全てのパスに対して設定
    corsSource.registerCorsConfiguration("/**", corsConfiguration);

    return corsSource;
  }
}
