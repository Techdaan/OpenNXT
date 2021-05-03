package com.opennxt.login

import com.opennxt.net.GenericResponse

enum class LoginResult(val code: GenericResponse) {
    SUCCESS(GenericResponse.SUCCESSFUL),
    FAILED_LOADING(GenericResponse.FAILED_LOADING_PROFILE),
    DATABASE_TIMEOUT(GenericResponse.LOGINSERVER_OFFLINE),
    DATABASE_ERROR(GenericResponse.INVALID_LOGIN_SERVER_RESPONSE),
    INVALID_USERNAME_PASS(GenericResponse.INVALID_USERNAME_OR_PASSWORD),
    WORLD_FULL(GenericResponse.WORLD_FULL),
    LOCKED(GenericResponse.ACCOUNT_LOCKED),
    BANNED(GenericResponse.TEMPORARILY_BANNED),
    LOGGED_IN(GenericResponse.LOGGED_IN)
}