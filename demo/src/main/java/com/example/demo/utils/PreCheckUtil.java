package com.example.demo.utils;

import com.example.demo.rule.PreCheckType;
import com.example.demo.vo.RecipeToolPair;
import com.example.demo.vo.ResultInfo;
import com.example.demo.vo.Rule;
import com.example.demo.vo.RuncardRawInfo;

import java.util.EnumSet;
import java.util.Objects;

public final class PreCheckUtil {

    private PreCheckUtil() {
    }

    public static ResultInfo run(EnumSet<PreCheckType> types,
                                 String cond,
                                 RuncardRawInfo rc,
                                 Rule rule,
                                 RecipeToolPair pair) {

        Objects.requireNonNull(types, "types");

        for (PreCheckType type : types) {
            ResultInfo r = switch (type) {
                case LOT_TYPE_EMPTY -> RuleUtil.skipIfLotTypeEmpty(cond, rc, rule, pair);
                case LOT_TYPE_MISMATCH -> RuleUtil.skipIfLotTypeMismatch(cond, rc, rule, pair);
                case SETTINGS_NULL -> RuleUtil.skipIfSettingsNull(cond, rc, rule, pair);
            };
            if (r != null) {
                return r;
            }
        }
        return null;
    }
}

