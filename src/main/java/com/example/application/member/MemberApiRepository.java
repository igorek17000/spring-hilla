package com.example.application.member;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberApiRepository extends JpaRepository<MemberApi, Integer> {
    Optional<MemberApi> findByMinuteBongAndMemberIdx(Integer minuteBong, Integer member_idx);
}
