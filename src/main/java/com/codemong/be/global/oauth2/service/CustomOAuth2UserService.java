package com.codemong.be.global.oauth2.service;

import com.codemong.be.global.oauth2.user.CustomOAuth2User;
import com.codemong.be.role.entity.Role;
import com.codemong.be.role.repository.RoleRepository;
import com.codemong.be.user.entity.User;
import com.codemong.be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

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

        User user = userRepository.findBySnsId(snsId)
                .orElse(null);

        if(user == null){
            String accessToken = userRequest.getAccessToken().getTokenValue();
            // TODO: KMS 설정이 준비되면 다시 암호화해서 저장해야 합니다.
            /*
            String encryptToken;
            try {
                encryptToken = kmsService.encrypt(accessToken);
            } catch (CustomException e) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error(ErrorCode.OAUTH_LOGIN_FAILED.getErrorCode(), ErrorCode.OAUTH_LOGIN_FAILED.getMessage(), null)
                );
            }
             */
            Role userRole = roleRepository.findById(1L)
                    .orElseThrow(()-> new RuntimeException("해당 Role이 없습니다."));

            user = userRepository.save(
                        User.builder()
                                .snsId(snsId)
                                .name(name)
                                .email(email)
                                .role(userRole)
                                .htmlUrl(htmlUrl)
                                .githubToken(accessToken) // 평문 하드 코딩 부분. KMS 수정
                                .build()
                        );

        }

        return new CustomOAuth2User(user.getId(), user.getRole().getName(), attributes);
    }
}
