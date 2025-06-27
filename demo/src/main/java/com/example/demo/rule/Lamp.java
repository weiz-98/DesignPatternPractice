package com.example.demo.rule;

import java.util.Arrays;

public enum Lamp {
    SKIP(0),
    GREEN(1),
    YELLOW(2),
    RED(3);

    private final int code;

    Lamp(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static Lamp of(Integer code) {
        if (code == null) return RED;
        return Arrays.stream(values())
                .filter(l -> l.code == code)
                .findFirst()
                .orElse(RED);
    }
}
