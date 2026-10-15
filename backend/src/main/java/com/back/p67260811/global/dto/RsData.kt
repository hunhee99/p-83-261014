package com.back.p67260811.global.dto

import com.fasterxml.jackson.annotation.JsonIgnore

class RsData<T> @JvmOverloads constructor(
    val resultCode: String,
    val msg: String,
    val data: T? = null
) {
    @get:JsonIgnore
    val statusCode: Int
        get() = resultCode.split("-")[0].toInt()
}