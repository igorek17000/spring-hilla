package com.example.application.bybit.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public void register(@RequestBody MemberRegisterDTO memberRegisterDTO){
        authService.register(memberRegisterDTO);
    }
}
