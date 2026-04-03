package com.example.blablacar.controller.api;

import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Base interface for API v1 controllers to centralize the versioned mapping.
 */
@RequestMapping(headers = "X-API-Version=1")
public interface V1Api {
}
