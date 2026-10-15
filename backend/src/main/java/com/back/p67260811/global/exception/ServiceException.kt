package com.back.p67260811.global.exception

import com.back.p67260811.global.dto.RsData

class ServiceException(
    resultCode: String,
    msg: String
) : RuntimeException(msg) {
    val rsData: RsData<*>


    init {
        this.rsData = RsData<Any?>(
            resultCode,
            msg
        )
    }


    val resultCode: String
        get() = rsData.resultCode

    val msg: String
        get() = rsData.msg
}
