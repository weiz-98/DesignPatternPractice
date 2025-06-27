package com.example.demo.rule;

public enum PreCheckType {
    LOT_TYPE_EMPTY,          // skipIfLotTypeEmpty
    LOT_TYPE_MISMATCH,       // skipIfLotTypeMismatch
    SETTINGS_NULL            // skipIfSettingsNull
}
