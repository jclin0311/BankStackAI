package com.bankstack.eval;

import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

public class ResourceLoaderUtil {
	
	
	public static boolean exists(String path) {
	    return ResourceLoaderUtil.class.getClassLoader().getResource(path) != null;
	}
	
	
	
  static String loadText(String path) {
    try (var is = new ClassPathResource(path).getInputStream()) {
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to read " + path, e);
    }
  }
}