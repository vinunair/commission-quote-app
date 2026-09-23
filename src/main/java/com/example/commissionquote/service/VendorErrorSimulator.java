package com.example.commissionquote.service;

import com.example.commissionquote.config.VendorProperties;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class VendorErrorSimulator {

    private final VendorProperties vendorProperties;

    public VendorErrorSimulator(VendorProperties vendorProperties) {
        this.vendorProperties = vendorProperties;
    }

    public boolean shouldFail() {
        return ThreadLocalRandom.current().nextInt(100) < vendorProperties.errorRatePercent();
    }
}
