package com.r3.token.fungible.workflows

import com.r3.corda.ledger.utxo.fungible.NumericDecimal
import com.r3.corda.ledger.utxo.ownable.query.OwnableStateQueries
import com.r3.sum
import com.r3.token.fungible.states.Token
import net.corda.v5.application.crypto.DigestService
import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.crypto.DigestAlgorithmName
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.StateAndRef
import net.corda.v5.ledger.utxo.UtxoLedgerService
import net.corda.v5.ledger.utxo.token.selection.TokenBalanceCriteria
import net.corda.v5.ledger.utxo.token.selection.TokenSelection
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.Instant

@InitiatingFlow(protocol = "get-token-balance")
@Suppress("unused")
class GetTokenBalanceFlow : ClientStartableFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    data class GetBalanceRequest(
        val issuer: String,
        val owner: String,
        val symbol: String
    )

    @CordaInject
    private lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    private lateinit var utxoLedgerService: UtxoLedgerService

    @CordaInject
    private lateinit var memberLookup: MemberLookup

    @CordaInject
    private lateinit var notaryLookup: NotaryLookup

    @CordaInject
    private lateinit var flowMessaging: FlowMessaging

    @CordaInject
    private lateinit var flowEngine: FlowEngine

    @CordaInject
    private lateinit var digestService: DigestService

    @CordaInject
    private lateinit var tokenSelection: TokenSelection

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        log.info("GetTokenBalanceFlow.call() called")

        val request = requestBody.getRequestBodyAs(jsonMarshallingService, GetBalanceRequest::class.java)

        val issuerX500Name = MemberX500Name.parse(request.issuer)
        val ownerX500Name = MemberX500Name.parse(request.owner)

        val issuer = requireNotNull(memberLookup.lookup(issuerX500Name)) {
            "Failed to obtain member information for the specified name: ${request.issuer}."
        }
        val owner = requireNotNull(memberLookup.lookup(ownerX500Name)) {
            "Failed to obtain member information for the specified name: ${request.owner}."
        }

        val issuerKey = issuer.ledgerKeys.first()
        val issuerHash = digestService.hash(issuerKey.encoded, DigestAlgorithmName.SHA2_256)

        val ownerKey = owner.ledgerKeys.first()
        val ownerHash = digestService.hash(ownerKey.encoded, DigestAlgorithmName.SHA2_256)

        val notaryX500Name = notaryLookup.notaryServices.first().name

        val criteria =
            TokenBalanceCriteria(
                Token::class.java.name.toString(),
                issuerHash,
                notaryX500Name,
                request.symbol,
            ).apply { this.ownerHash = ownerHash }

        val tokenBalance = tokenSelection.queryBalance(criteria) ?: throw CordaRuntimeException("Failed to query token balance")

        log.info("Querying Token has been finished")

        return ("Token balance of " + request.owner + " is " + tokenBalance.totalBalance)
    }
}
/*
{
    "clientRequestId": "get-1",
    "flowClassName": "com.r3.token.fungible.workflows.GetTokenBalanceFlow",
    "requestBody": {
        "issuer": "CN=Alice, OU=Test Dept, O=R3, L=London, C=GB",
        "owner": "CN=Bob, OU=Test Dept, O=R3, L=London, C=GB",
        "symbol": "USD"
    }
}
*/