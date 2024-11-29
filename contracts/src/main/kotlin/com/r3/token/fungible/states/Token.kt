package com.r3.token.fungible.states

import com.r3.corda.ledger.utxo.fungible.FungibleState
import com.r3.corda.ledger.utxo.fungible.NumericDecimal
import com.r3.corda.ledger.utxo.issuable.IssuableState
import com.r3.corda.ledger.utxo.ownable.OwnableState
import com.r3.token.fungible.contracts.TokenContract
import net.corda.v5.crypto.SecureHash
import net.corda.v5.ledger.utxo.BelongsToContract
import java.security.PublicKey

@BelongsToContract(TokenContract::class)
data class Token(
    private val issuer: PublicKey,
    private val owner: PublicKey,
    private val quantity: NumericDecimal,
    val issuerHash: SecureHash,
    val ownerHash: SecureHash,
    val symbol: String,
    val tag: String,
    private val participants: List<PublicKey> = listOf(owner)
) : FungibleState<NumericDecimal>, IssuableState, OwnableState {

    companion object {
        val tokenType = Token::class.java.name.toString()
    }

    override fun getIssuer(): PublicKey {
        return issuer
    }

    override fun getOwner(): PublicKey {
        return owner
    }

    override fun getParticipants(): List<PublicKey> {
        return participants
    }

    override fun getQuantity(): NumericDecimal {
        return quantity
    }

    override fun isFungibleWith(other: FungibleState<NumericDecimal>): Boolean {
        return other is Token && other.issuer == issuer && other.symbol == symbol
    }
}