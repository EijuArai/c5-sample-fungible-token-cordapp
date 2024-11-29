package com.r3.token.fungible

import com.r3.token.fungible.states.Token
import net.corda.v5.ledger.utxo.StateAndRef
import net.corda.v5.ledger.utxo.query.VaultNamedQueryFactory
import net.corda.v5.ledger.utxo.query.VaultNamedQueryStateAndRefTransformer
import net.corda.v5.ledger.utxo.query.registration.VaultNamedQueryBuilderFactory

class TokenQueryFactory : VaultNamedQueryFactory {
    override fun create(vaultNamedQueryBuilderFactory: VaultNamedQueryBuilderFactory) {
        vaultNamedQueryBuilderFactory.create("TOKEN_QUERY")
            .whereJson(
                "WHERE visible_states.custom_representation -> 'com.r3.token.fungible.states.Token' ->> 'symbol' = :symbol " +
                        "AND visible_states.custom_representation -> 'com.r3.token.fungible.states.Token' ->> 'ownerHash' = :ownerHash " +
                        "AND visible_states.custom_representation -> 'com.r3.token.fungible.states.Token' ->> 'issuerHash' = :issuerHash"
            )
            .map(TokenQueryTransformer())
            .register()
    }
}

class TokenQueryTransformer : VaultNamedQueryStateAndRefTransformer<Token, Token> {
    override fun transform(data: StateAndRef<Token>, parameters: MutableMap<String, Any>): Token {
        return data.state.contractState
    }
}