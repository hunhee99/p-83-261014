package com.back.p67260811.global.rq

import com.back.p67260811.domain.member.entity.Member
import com.back.p67260811.global.exception.ServiceException
import com.back.p67260811.global.security.SecurityUser
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class Rq(
    private val request: HttpServletRequest,
    private val response: HttpServletResponse,
) {
    val actor: Member
        get() = actorOrNull ?: throw ServiceException("401-1", "로그인 후 이용해주세요.")

    val actorOrNull: Member?
        get() = (SecurityContextHolder.getContext().authentication?.principal as? SecurityUser)
            ?.let { Member(it.id, it.username, it.nickname) }

    fun setHeader(name: String, value: String) {
        response.setHeader(name, value)
    }

    fun getHeader(name: String, defaultValue: String): String =
        request.getHeader(name)
            ?.takeIf { it.isNotBlank() }
            ?: defaultValue

    fun getCookieValue(name: String, defaultValue: String): String =
        request.cookies
            ?.firstOrNull { it.name == name && it.value.isNotBlank() }
            ?.value
            ?: defaultValue

    fun addCookie(name: String, value: String) {
        val cookie = Cookie(name, value).apply {
            domain = "localhost"
            path = "/"
            isHttpOnly = true
        }
        response.addCookie(cookie)
    }

    fun deleteCookie(name: String) {
        val cookie = Cookie(name, "").apply {
            domain = "localhost"
            path = "/"
            isHttpOnly = true
            maxAge = 0
        }
        response.addCookie(cookie)
    }
}