package com.codemong.be.feedback.service;

import com.codemong.be.branch.entity.Branch;
import com.codemong.be.feedback.entity.Feedback;
import com.codemong.be.feedback.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;

    public void save(Branch curBranch, String content) {
        log.debug("feedback : \n{}", content);
        Feedback feedback = new Feedback(curBranch, content);

        feedbackRepository.save(feedback);
    }

}
