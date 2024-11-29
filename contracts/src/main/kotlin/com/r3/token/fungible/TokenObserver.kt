package com.r3.token.fungible

import com.r3.token.fungible.states.Token
import net.corda.v5.application.crypto.DigestService
import net.corda.v5.ledger.utxo.observer.UtxoLedgerTokenStateObserver
import net.corda.v5.ledger.utxo.observer.UtxoToken
import net.corda.v5.ledger.utxo.observer.UtxoTokenFilterFields
import net.corda.v5.ledger.utxo.observer.UtxoTokenPoolKey

class TokenObserver : UtxoLedgerTokenStateObserver<Token> {

    override fun onCommit(state: Token, digestService: DigestService): UtxoToken {
        return UtxoToken(
            UtxoTokenPoolKey(Token::class.java.name, state.issuerHash, state.symbol),
            state.quantity.value,
            UtxoTokenFilterFields(state.tag, state.ownerHash)
        )
    }

    override fun getStateType(): Class<Token> {
        return Token::class.java
    }
}