package com.codemong.be.chathistory.service;

import com.codemong.be.branch.entity.Branch;
import com.codemong.be.chathistory.entity.ChatHistory;
import com.codemong.be.chathistory.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class ChatHistoryService {
    private final ChatHistoryRepository chatHistoryRepository;

    public String getLatestChatHistory(Long branchId) {
        return chatHistoryRepository.findFirstByBranch_IdOrderByCreatedAtDesc(branchId)
                .map(ChatHistory::getContent)
                .orElse("N/A");
    }

    public void save(Branch curBranch, String updatedSummary) {
        log.debug("updatedSummary : \n{}", updatedSummary);
        ChatHistory chatHistory = new ChatHistory(curBranch, updatedSummary);

        chatHistoryRepository.save(chatHistory);
    }

}
