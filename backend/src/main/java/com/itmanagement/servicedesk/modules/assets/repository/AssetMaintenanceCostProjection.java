package com.itmanagement.servicedesk.modules.assets.repository;

import java.math.BigDecimal;

public interface AssetMaintenanceCostProjection {
    Long getAssetId();
    BigDecimal getTotalCost();
}
