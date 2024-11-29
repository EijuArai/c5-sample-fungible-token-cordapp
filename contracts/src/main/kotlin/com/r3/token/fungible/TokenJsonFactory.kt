package com.r3.token.fungible

import com.r3.token.fungible.states.Token
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.ledger.utxo.query.json.ContractStateVaultJsonFactory

class TokenJsonFactory : ContractStateVaultJsonFactory<Token> {
    override fun getStateType(): Class<Token> = Token::class.java

    override fun create(state: Token, jsonMarshallingService: JsonMarshallingService): String {
        return jsonMarshallingService.format(
            TokenJson(
                state.symbol,
                state.ownerHash.toString(),
                state.issuerHash.toString()
            )
        )
    }

    data class TokenJson(
        val symbol: String,
        val ownerHash: String,
        val issuerHash: String
    )
}