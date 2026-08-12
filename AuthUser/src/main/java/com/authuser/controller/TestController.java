package com.authuser.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/test")
public class TestController {
	
	


  // Public: should work WITHOUT any token
  @GetMapping("/public")
  public String pub() { return "public-ok"; }

 
  // Protected: needs SCOPE_fdx:accounts.read
  @PreAuthorize("hasAuthority('SCOPE_fdx:accounts.read')")
  @GetMapping("/secure")
  public String secure() { return "secure-ok"; }
}
