package com.example.application.bybit.auth;

import com.example.application.member.Member;
import com.example.application.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<Member> member = memberRepository.findByUsername(username);

        if(member.isPresent()){
            log.info("로그인 성공 : "+member.get().getUsername());
            return new UserDetailsImpl(member.get());
        }else{
            return null;
        }
    }

    private static GrantedAuthority getAuthorities(Member member) {
        return new SimpleGrantedAuthority("ROLE_" + member.getRole());
    }
}
