package gosbry.services

import com.password4j.Argon2Function
import com.password4j.Password
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PasswordHashingTest {
    @Test
    fun `hashes use Argon2id with the configured costs and a fresh salt`() {
        val password = " test password "
        val first = hashPassword(password)
        val second = hashPassword(password)

        assertNotEquals(first, second)
        for (hash in listOf(first, second)) {
            assertTrue(hash.startsWith("\$argon2id\$v=19\$m=19456,t=2,p=1\$"))
            val algorithm = Argon2Function.getInstanceFromHash(hash)
            assertTrue(Password.check(password, hash).with(algorithm))
            assertFalse(Password.check(password.trim(), hash).with(algorithm))
        }
    }
}
