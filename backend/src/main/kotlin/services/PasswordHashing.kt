package gosbry.services

import com.password4j.Argon2Function
import com.password4j.Password
import com.password4j.types.Argon2

private val argon2id = Argon2Function.getInstance(19 * 1024, 2, 1, 32, Argon2.ID)

internal fun hashPassword(password: String): String = Password.hash(password).addRandomSalt(16).with(argon2id).result
