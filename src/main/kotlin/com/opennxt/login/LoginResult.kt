package com.opennxt.login

import com.opennxt.net.GenericResponse
import mu.KotlinLogging
import java.util.*

enum class LoginResult(val code: GenericResponse) {
    SUCCESS(GenericResponse.SUCCESSFUL),
    FAILED_LOADING(GenericResponse.FAILED_LOADING_PROFILE),
    DATABASE_TIMEOUT(GenericResponse.LOGINSERVER_OFFLINE),
    DATABASE_ERROR(GenericResponse.INVALID_LOGIN_SERVER_RESPONSE),
    INVALID_USERNAME_PASS(GenericResponse.INVALID_USERNAME_OR_PASSWORD),
    WORLD_FULL(GenericResponse.WORLD_FULL),
    LOCKED(GenericResponse.ACCOUNT_LOCKED),
    BANNED(GenericResponse.TEMPORARILY_BANNED),
    LOGGED_IN(GenericResponse.LOGGED_IN),
    OUT_OF_DATE(GenericResponse.OUT_OF_DATE),
    ;

    companion object {
        private val logger = KotlinLogging.logger {  }

        private val REVERSE_LOOKUP = EnumMap<GenericResponse, LoginResult>(GenericResponse::class.java)

        init {
            values().forEach { res -> REVERSE_LOOKUP[res.code] = res }
        }

        fun reverse(response: GenericResponse): LoginResult {
            val reversed = REVERSE_LOOKUP[response]
            if (reversed != null)
                return reversed
            logger.warn { "Couldn't find GenericResponse->LoginResult mapping for $response, returning LOCKED" }
            return LOCKED
        }
    }
}