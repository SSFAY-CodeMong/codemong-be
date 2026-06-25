package com.codemong.be.project.dto;

import java.util.List;

public record ProjectPreferenceCardResponse(
        String id,
        String title,
        String summary,
        String track,
        String level,
        String image,
        List<String> tags
) {
}
