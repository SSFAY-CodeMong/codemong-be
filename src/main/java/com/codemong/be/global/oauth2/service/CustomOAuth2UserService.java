package com.codemong.be.global.oauth2.service;

import com.codemong.be.admin.service.AdminTaskLogService;
import com.codemong.be.global.exception.CustomException;
import com.codemong.be.global.exception.ErrorCode;
import com.codemong.be.global.kms.KmsService;
import com.codemong.be.global.oauth2.user.CustomOAuth2User;
import com.codemong.be.role.entity.Role;
import com.codemong.be.role.repository.RoleRepository;
import com.codemong.be.user.entity.User;
import com.codemong.be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private static final String USER_BAN_KEY_PREFIX = "BAN:";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final KmsService kmsService;
    private final RedisTemplate<String, String> redisTemplate;
    private final AdminTaskLogService adminTaskLogService;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        Object idValue = attributes.get("id");
        Long snsId = ((Number) idValue).longValue();

        String name = (String) attributes.get("name");
        String email = (String) attributes.get("email");
        String htmlUrl = (String) attributes.get("html_url");
        String profilePath = (String) attributes.get("avatar_url");

        User user = userRepository.findBySnsId(snsId)
                .orElse(null);

        String accessToken = userRequest.getAccessToken().getTokenValue();

        String encryptToken;

        try {
            encryptToken = kmsService.encrypt(accessToken);
        } catch (CustomException e) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(ErrorCode.OAUTH_LOGIN_FAILED.getErrorCode(), ErrorCode.OAUTH_LOGIN_FAILED.getMessage(), null)
            );
        }

        if(user == null){
            Role userRole = roleRepository.findById(1L)
                    .orElseThrow(()-> new RuntimeException("해당 Role이 없습니다."));

            user = userRepository.save(
                        User.builder()
                                .snsId(snsId)
                                .name(name)
                                .email(email)
                                .role(userRole)
                                .htmlUrl(htmlUrl)
                                .githubToken(encryptToken)
                                .profilePath(profilePath)
                                .build()
                        );

        }else{
            user.updateGithubToken(encryptToken);
        }

        if (Boolean.TRUE.equals(redisTemplate.hasKey(USER_BAN_KEY_PREFIX + user.getId()))) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("USER_BANNED", "밴 처리된 사용자입니다.", null)
            );
        }

        String taskId = adminTaskLogService.start(
                "USER_LOGIN",
                "userId " + user.getId() + "번이 GitHub OAuth 로그인.",
                user.getId(),
                null,
                null,
                null
        );
        adminTaskLogService.complete(taskId, true, "snsId=" + snsId);

        return new CustomOAuth2User(user.getId(), user.getRole().getName(), attributes);
    }
}
