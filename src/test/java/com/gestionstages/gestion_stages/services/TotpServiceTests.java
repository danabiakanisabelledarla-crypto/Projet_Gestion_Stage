package com.gestionstages.gestion_stages.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TotpServiceTests {

    private final TotpService service = new TotpService();

    @Test
    void generatesRfc6238CompatibleSixDigitCode() {
        String rfcSecret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
        assertEquals(287082, service.generateCode(rfcSecret, 1));
    }

    @Test
    void rejectsMalformedCodes() {
        assertFalse(service.verify("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", "123"));
        assertFalse(service.verify(null, "123456"));
    }
}
