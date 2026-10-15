package com.back.p67260811.global.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User

class SecurityUser(
    username: String,
    val nickname: String,
    authorities: Collection<GrantedAuthority>,
    val id: Int,
    password: String,
) : User(username, password, authorities) {
}