package org.darkan.lobby.web.model

import io.ktor.utils.io.*
import org.darkan.core.EnvVars
import org.darkan.core.Logger.logError
import org.darkan.core.Logger.logWarn
import org.darkan.core.PasswordProblem
import org.darkan.core.isValidEmail
import org.darkan.core.mongo.collections.Accounts
import org.darkan.core.net.Session
import org.darkan.core.net.prot.dto.CheckEmailReplyOpcode
import org.darkan.core.net.prot.dto.CreateAccountReplyOpcode
import org.darkan.core.net.prot.packet.AccountCreationStage
import org.darkan.core.net.prot.packet.CheckEmailValidity
import org.darkan.core.net.prot.packet.CreateAccountReply
import org.darkan.core.net.prot.packet.CreateCheckEmailReply
import org.darkan.core.net.prot.packet.Ping
import org.darkan.core.net.prot.packet.Pong
import org.darkan.core.net.prot.packet.SendSignUpForm
import org.darkan.core.passwordProblem
import world.gregs.voidps.buffer.readRSString
import world.gregs.voidps.cache.secure.Xtea

class AccountCreationSession(val session: Session) {
    suspend fun handleDecodedPackets() {
        for (i in 0 until EnvVars.packetQueueCapacity) {
            val packet = session.readChannel.tryReceive().getOrNull() ?: break
            when (packet) {
                is Ping -> session.send(Pong())
                is AccountCreationStage -> {}
                is CheckEmailValidity -> session.send(CreateCheckEmailReply(checkEmail(packet)))
                is SendSignUpForm -> session.send(CreateAccountReply(signUp(packet)))
                else -> logWarn("Unhandled account creation packet: $packet")
            }
        }
    }

    private fun decrypt(data: ByteArray): ByteReadChannel {
        Xtea.decipher(data, session.isaacIn.seed)
        return ByteReadChannel(data)
    }

    private suspend fun checkEmail(packet: CheckEmailValidity): CheckEmailReplyOpcode = try {
        val email = decrypt(packet.encryptedData).readRSString()
        when {
            !email.isValidEmail() -> CheckEmailReplyOpcode.INVALID_EMAIL
            Accounts.exists(email) -> CheckEmailReplyOpcode.EMAIL_ALREADY_IN_USE
            else -> CheckEmailReplyOpcode.VALID
        }
    } catch (e: Exception) {
        logError("Email check failed", e)
        CheckEmailReplyOpcode.SERVER_ERROR
    }

    private suspend fun signUp(packet: SendSignUpForm): CreateAccountReplyOpcode = try {
        val form = decrypt(packet.encryptedData)
        val email = form.readRSString()
        val password = form.readRSString()
        when {
            !email.isValidEmail() -> CreateAccountReplyOpcode.INVALID_EMAIL
            else -> passwordReply(password) ?: create(email, password)
        }
    } catch (e: Exception) {
        logError("Sign-up failed", e)
        CreateAccountReplyOpcode.SERVER_ERROR
    }

    private fun passwordReply(password: String): CreateAccountReplyOpcode? = when (password.passwordProblem()) {
        PasswordProblem.TOO_SHORT ->
            if (password.length < CLIENT_PASSWORD_MIN_LENGTH) CreateAccountReplyOpcode.PASSWORD_LENGTH
            else CreateAccountReplyOpcode.PASSWORD_TOO_EASY
        PasswordProblem.NOT_ALPHANUMERIC -> CreateAccountReplyOpcode.PASSWORD_NOT_ALPHANUMERIC
        PasswordProblem.TOO_EASY -> CreateAccountReplyOpcode.PASSWORD_TOO_EASY
        null -> null
    }

    private suspend fun create(email: String, password: String): CreateAccountReplyOpcode =
        if (Accounts.createLobby(email, password) == null) CreateAccountReplyOpcode.EMAIL_ALREADY_IN_USE
        else CreateAccountReplyOpcode.SUCCESS

    private companion object {
        /** Below this length the client's own script explains the minimum; above it, only that the password is weak. */
        const val CLIENT_PASSWORD_MIN_LENGTH = 5
    }
}
