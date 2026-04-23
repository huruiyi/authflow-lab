package com.demo.authserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuthServerDemoApplication {

  /**
   * http://127.0.0.1:30000/.well-known/openid-configuration
   */
  public static void main(String[] args) {
    SpringApplication.run(AuthServerDemoApplication.class, args);
  }

}
