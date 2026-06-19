package com.codemong.be.feedback.service;

import com.codemong.be.branch.entity.Branch;
import com.codemong.be.feedback.entity.Feedback;
import com.codemong.be.feedback.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;

    public String getLatestFeedback(Long branchId) {
        return feedbackRepository.findFirstByBranch_IdOrderByCreatedAtDesc(branchId)
                .map(Feedback::getContent)
                .orElse("N/A");
    }

    public void save(Branch curBranch, String updatedSummary) {
        log.debug("updatedSummary : \n{}", updatedSummary);
        Feedback feedback = new Feedback(curBranch, updatedSummary);

        feedbackRepository.save(feedback);
    }

}
