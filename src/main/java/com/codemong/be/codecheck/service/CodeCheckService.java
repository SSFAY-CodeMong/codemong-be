package com.codemong.be.codecheck.service;

import com.codemong.be.branch.entity.Branch;
import com.codemong.be.branch.repository.BranchRepository;
import com.codemong.be.codecheck.dto.CodeCheckCallbackRequest;
import com.codemong.be.codecheck.dto.CodeCheckResult;
import com.codemong.be.global.kms.KmsService;
import com.codemong.be.repository.entity.GithubRepository;
import com.codemong.be.repository.repository.GithubRepositoryRepository;
import com.codemong.be.user.entity.User;
import com.codemong.be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CodeCheckService {

    private final UserRepository userRepository;
    private final GithubRepositoryRepository githubRepositoryRepository;
    private final BranchRepository branchRepository;
    private final KmsService kmsService;

    public CodeCheckResult runGithubActionsCheck(Long repositoryId, Long step, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("TODO: 사용자 커스텀 예외로 변경"));
        GithubRepository repository = githubRepositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new IllegalStateException("TODO: 레포지토리 커스텀 예외로 변경"));

        // TODO 1: repository 소유자가 user인지 검증한다.
        if(!repository.getUser().getId().equals(user.getId()))
            throw new IllegalStateException("Todo : 유저 불일치 커스텀 예외로 변경");

        // TODO 2: 사용자가 진행 중인 프로젝트의 최신 Branch 엔티티를 조회 => 최신 스텝기준으로 이름 조합으로 가져오면 될려나?
        // branches 테이블에서 where repository_id = :repositoryId 하고, created_at desc 하고, limit 1 하면 될듯
        Branch currentBranch = branchRepository.findTopByRepository_IdOrderByCreatedAtDesc(repositoryId)
                .orElseThrow(() -> new IllegalStateException("TODO : Brnach custom error change"));

        // TODO 3: user.githubToken을 복호화해서 GitHub Actions workflow_dispatch를 호출한다.
        // TODO 3-1: 복호화된 토큰은 매우 중요한 요소이므로, 처리에 주의하기
        String decryptToken = kmsService.decrypt(user.getGithubToken());

        // TODO 4: workflow_dispatch 입력값으로 repositoryId, userId, step, branchName, callbackUrl을 전달한다.
        // TODO 5: 콜백이 비동기로 들어오는 구조면 pending 상태를 저장하고, 여기서는 요청 접수 결과를 반환한다.
        // TODO 6: 동기 대기가 필요하면 workflow run 상태를 polling해서 최종 true/false를 반환한다.

        return new CodeCheckResult(false);
    }

    public void receiveGithubActionsCallback(CodeCheckCallbackRequest request) {
        // TODO 1: GitHub Actions에서 온 콜백인지 서명/토큰으로 검증한다.
        // TODO 2: request.repositoryId, request.userId, request.step 기준으로 검사 요청을 찾는다.
        // TODO 3: request.passed 값을 저장한다.
        // TODO 4: AIService.codeReview가 대기/polling하는 방식이면 결과 조회 가능 상태로 변경한다.
    }
}
