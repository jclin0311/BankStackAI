package com.bankstack.rag.security;

import com.bankstack.rag.retrieve.HybridSearchResult;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PermissionFilterService {
	
	
	// Filters search results based on sensitivity and allowed scopes defined in metadata.
	// Ensures only authorized results are returned according to the user's access context.
	// Supports PUBLIC, INTERNAL, and ADMIN access levels with scope-based checks.
	// Unrecognized sensitivity values are denied: a typo in ingestion metadata must not
	// widen access.

    public List<HybridSearchResult> filter(List<HybridSearchResult> input, AccessContext accessContext) {
        return input.stream()
                .filter(result -> isAllowed(result, accessContext))
                .toList();
    }

    boolean isAllowed(HybridSearchResult result, AccessContext accessContext) {
    	
    	
    	if (accessContext.admin()) {
    	    return true; // admin can access all chunks
    	}
    	
        Map<String, Object> metadata = result.metadata();
        if (metadata == null || metadata.isEmpty()) {
            return true;
        }

        String sensitivity = readString(metadata, "sensitivity");
        Set<String> allowedScopes = readScopes(metadata, "allowedScopes");

        if (sensitivity == null || sensitivity.isBlank()) {
            return true;
        }

        
        boolean allowed =
         switch (sensitivity.toUpperCase()) {
            case "PUBLIC" -> true;
            case "INTERNAL" ->  hasAtLeastOneAllowedScope(accessContext.grantedScopes(), allowedScopes);
            // Only the admin short-circuit above reaches ADMIN chunks.
            case "ADMIN" -> false;
            default -> false;
        };

        return allowed;
    }

    private boolean hasAtLeastOneAllowedScope(Set<String> grantedScopes, Set<String> allowedScopes) {
        // An INTERNAL chunk that names no scopes grants access to nobody but admins,
        // who already returned above. Treating "no scopes listed" as "everyone" would
        // expose internal content to any authenticated caller, customers included,
        // since allowedScopes is optional at ingest.
        if (allowedScopes == null || allowedScopes.isEmpty()) {
            return false;
        }
        if (grantedScopes == null || grantedScopes.isEmpty()) {
            return false;
        }
        return grantedScopes.stream().anyMatch(allowedScopes::contains);
    }

    private String readString(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        return value == null ? null : value.toString();
    }

    private Set<String> readScopes(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value == null) {
            return Collections.emptySet();
        }
        String raw = value.toString().trim();
        if (raw.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
    }
}
