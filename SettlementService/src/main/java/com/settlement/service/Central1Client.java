package com.settlement.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Stub client that represents uploading the CPA005 file to Central1.
 * Replace with REST/SFTP implementation as needed.
 */
@Component
@Slf4j
public class Central1Client {

    public String upload(String fileName) {
        // In real world: upload the file and return Central1 reference number
        log.info("Uploading Pain001 file={} to Central1...", fileName);
        // Dummy reference
        return "CENTRAL1-" + System.currentTimeMillis();
    }
}
