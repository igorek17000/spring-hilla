package com.example.application.member;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Integer> {

    List<Member> findByIdxNotInAndTraceYn(Collection<Integer> idx, String traceYn);
    List<Member> findByTraceYn(String traceYn);


}
