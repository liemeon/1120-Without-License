package com.deepid.lgc.data.common

sealed class BaseResult <out T : Any, out U : Any> {
    data class Success <T: Any>(val data : T) : BaseResult<T, Nothing>()
    data class Error <U : Any>(val err: U) : BaseResult<Nothing, U>()
}

data class Failure(
    val code: Int,
    val message: String
)
