package com.example.application.member;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.example.application.member.MemberApi;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberApiRepository memberApiRepository;

    public Optional<MemberApi> getApi(Integer memberIdx, Integer minuteBong) {
        return memberApiRepository.findByMinuteBongAndMemberIdx(minuteBong, memberIdx);
    }

}
