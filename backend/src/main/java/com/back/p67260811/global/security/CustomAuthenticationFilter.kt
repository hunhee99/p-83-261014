package com.back.p67260811.global.security

import com.back.p67260811.domain.member.entity.Member
import com.back.p67260811.domain.member.service.MemberService
import com.back.p67260811.global.exception.ServiceException
import com.back.p67260811.global.rq.Rq
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import lombok.RequiredArgsConstructor
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException
import java.util.function.Supplier

@Component
@RequiredArgsConstructor
class CustomAuthenticationFilter(
    private val memberService: MemberService,
    private val rq: Rq
) : OncePerRequestFilter() {


    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        logger.debug("CustomAuthenticationFilter called")
        // 어쩔 수 없이 여기서 예외처리를 해야함 -> 가독성을 위해 로직을 함수화
        try {
            authenticate(request, response, filterChain)
        } catch (e: ServiceException) {
            val rsData = e.getRsData()
            response.setContentType("application/json; charset=UTF-8")
            response.setStatus(rsData.statusCode)
            response.getWriter().write(
                """
                    {
                        "resultCode": "${rsData.resultCode}",
                        "msg": "${rsData.msg}"
                    }
                    
                    """.trimIndent()
            )
        } catch (e: Exception) {
            throw e
        }
    }

    @Throws(ServletException::class, IOException::class)
    private fun authenticate(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        if (!request.getRequestURI().startsWith("/api/")) {
            filterChain.doFilter(request, response)
            return
        }

        if (mutableListOf<String?>("/api/v1/members/join", "/api/v1/members/login").contains(request.getRequestURI())) {
            filterChain.doFilter(request, response)
            return
        }


        val apiKey: String
        val accessToken: String

        val headerAuthorization = rq.getHeader("Authorization", "")

        if (!headerAuthorization.isBlank()) {
            if (!headerAuthorization.startsWith("Bearer ")) throw ServiceException(
                "401-2",
                "Authorization 헤더가 Bearer 형식이 아닙니다."
            )

            val headerAuthorizationBits = headerAuthorization.split(" ".toRegex(), limit = 3).toTypedArray()

            apiKey = headerAuthorizationBits[1]
            accessToken = if (headerAuthorizationBits.size == 3) headerAuthorizationBits[2] else ""
        } else {
            apiKey = rq.getCookieValue("apiKey", "")
            accessToken = rq.getCookieValue("accessToken", "")
        }

        var member: Member? = null

        val isApiKeyExists = !apiKey.isBlank()
        val isAccessTokenExists = !accessToken.isBlank()
        var isAccessTokenValid = false

        // 공개 API의 경우 여기서 인증 처리하지 않고 시큐리티에게 위임
        if (!isApiKeyExists && !isAccessTokenExists) {
            filterChain.doFilter(request, response)
            return
        }

        if (!isApiKeyExists) throw ServiceException("401-1", "로그인 후 이용해주세요.")


        if (isAccessTokenExists) {
            val payload = memberService.payloadOrNull(accessToken)

            if (payload != null) {
                val id = payload.get("id") as Int
                val username = payload.get("username") as String?
                val nickname = payload.get("nickname") as String?

                member = Member(id, username, nickname)
                isAccessTokenValid = true
            }
        }

        if (member == null) {
            member = memberService
                .findByApiKey(apiKey)
                .orElseThrow<ServiceException?>(Supplier { ServiceException("401-3", "API 키가 유효하지 않습니다.") })
        }

        if (isAccessTokenExists && !isAccessTokenValid) {
            val newAccessToken = memberService.genAccessToken(member)
            rq.addCookie("accessToken", newAccessToken)
            rq.setHeader("accessToken", newAccessToken)
        }

        val user: UserDetails = SecurityUser(
            id = member.id,
            username = member.username,
            nickname = member.nickname,
            authorities = member.authorities,
            password = ""
        )


        val authentication: Authentication = UsernamePasswordAuthenticationToken(
            user,
            user.getPassword(),
            user.getAuthorities()
        )


        SecurityContextHolder
            .getContext()
            .setAuthentication(authentication)

        filterChain.doFilter(request, response)
    }
}
