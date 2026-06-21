package com.codemong.be.mail.service;

import com.codemong.be.mail.dto.MailContentResponse;
import com.codemong.be.mail.repository.MailContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MailContentService {

    private final MailContentRepository mailContentRepository;

    public List<MailContentResponse> list() {
        return mailContentRepository.findAllByOrderByTypeAscDisplayOrderAsc().stream()
                .map(content -> new MailContentResponse(
                        content.getTrack() + "/" + content.getSourceFile().replaceFirst("\\.md$", ""),
                        content.getType().name(),
                        content.getTrack(),
                        content.getSourceFile(),
                        content.getDisplayOrder(),
                        content.getTitle(),
                        ""
                ))
                .toList();
    }

    public Optional<MailContentResponse> randomContent() {
        return mailContentRepository.findRandom().map(MailContentResponse::from);
    }

    public Optional<MailContentResponse> find(String id) {
        String[] parts = id == null ? new String[0] : id.split("/", 2);
        if (parts.length != 2) {
            return Optional.empty();
        }
        String sourceFile = parts[1].endsWith(".md") ? parts[1] : parts[1] + ".md";
        return mailContentRepository.findByTrackAndSourceFile(parts[0], sourceFile)
                .map(MailContentResponse::from);
    }
}
