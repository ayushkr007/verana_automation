package com.verana.utils;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * DIDGenerator
 *
 * Generates DID identifiers used by the form automation.
 */
public class DIDGenerator {

    private static final int SIMPLE_SUFFIX_LENGTH = 8;
    private static final Pattern DID_REGEX = Pattern.compile("^did:[a-zA-Z0-9]+:[a-zA-Z0-9._:/-]+$");

    /**
     * Generates a unique DID string using current timestamp + UUID suffix.
     * Reads the prefix from config.properties (default: "did:vna:testnet").
     *
     * @return e.g. "did:vna:testnet:20260303-114955-a3f2"
     */
    public static String generateDID() {
        String prefix = DriverManager.getConfig().getProperty("did.prefix", "did:vna:testnet");
        String uuidSuffix = UUID.randomUUID().toString().substring(0, 4);
        String did = prefix + ":" + uuidSuffix;
        System.out.println("[DIDGenerator] Generated unique DID: " + did);
        return did;
    }

    /**
     * Generates format requested by user:
     * did:verana:<short-value>
     * where <short-value> is 8 alphanumeric chars (<10 chars) and
     * matches Verana add_did.go regex.
     *
     * @return e.g. "did:verana:a1b2c3d4"
     */
    public static String generateSimpleDID() {
        String raw = UUID.randomUUID().toString().replace("-", "");
        String suffix = raw.substring(0, SIMPLE_SUFFIX_LENGTH).toLowerCase();
        String did = "did:verana:" + suffix;
        if (!matchesAddDidRule(did)) {
            throw new IllegalStateException("Generated DID does not match add_did.go regex: " + did);
        }
        System.out.println("[DIDGenerator] Generated simple DID identifier: " + did);
        return did;
    }

    public static boolean matchesAddDidRule(String did) {
        return did != null && DID_REGEX.matcher(did).matches();
    }
}
