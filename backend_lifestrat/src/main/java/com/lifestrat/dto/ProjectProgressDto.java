package com.lifestrat.dto;

public record ProjectProgressDto(
        Long projectId,
        String title,
        int totalSteps,
        int completedSteps,
        double progressPercentage
) {
    public ProjectProgressDto {
        progressPercentage = totalSteps > 0 ? (double) completedSteps / totalSteps * 100 : 0.0;
    }
}