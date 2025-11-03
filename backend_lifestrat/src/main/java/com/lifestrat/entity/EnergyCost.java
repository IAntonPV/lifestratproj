package com.lifestrat.entity;

import lombok.Getter;

@Getter
public enum EnergyCost {
    LOW(1),
    MEDIUM(2),
    HIGH(3);

    private final int value;

    EnergyCost(int value) {
        this.value = value;
    }

}