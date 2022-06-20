package com.example.application.bybit.auth;

import com.example.application.member.Member;
import com.example.application.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final MemberRepository memberRepository;
    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public void register(MemberRegisterDTO memberRegisterDTO){
        memberRepository.save(Member.builder()
                .username(memberRegisterDTO.getUsername())
                .password(encoder.encode(memberRegisterDTO.getPassword()))
                .name(memberRegisterDTO.getName())
                .traceYn("N")
                .role("MEMBER")
                .build());
    }
}
