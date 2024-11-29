package com.r3.token.fungible.workflows

import com.r3.corda.ledger.utxo.fungible.NumericDecimal
import com.r3.token.fungible.contracts.TokenContract
import com.r3.token.fungible.states.Token
import net.corda.v5.application.crypto.DigestService
import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.crypto.DigestAlgorithmName
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.LoggerFactory
import java.lang.IllegalArgumentException
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit


@InitiatingFlow(protocol = "issue-fungible-token")
@Suppress("unused")
class IssueFungibleTokenFlow : ClientStartableFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    data class IssueFungibleTokenRequest(
        val issuer: String,
        val owner: String,
        val quantity: BigDecimal,
        val symbol: String,
        val tag: String
    )

    @CordaInject
    private lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    private lateinit var memberLookup: MemberLookup

    @CordaInject
    private lateinit var notaryLookup: NotaryLookup

    @CordaInject
    private lateinit var flowMessaging: FlowMessaging

    @CordaInject
    private lateinit var flowEngine: FlowEngine

    @CordaInject
    private lateinit var utxoLedgerService: UtxoLedgerService

    @CordaInject
    private lateinit var digestService: DigestService

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        log.info("IssueFungibleTokenFlow.call() called")

        val request = requestBody.getRequestBodyAs(jsonMarshallingService, IssueFungibleTokenRequest::class.java)

        val issuerX500Name = MemberX500Name.parse(request.issuer)
        val ownerX500Name = MemberX500Name.parse(request.owner)
        val myInfo = memberLookup.myInfo()

        val owner = requireNotNull(memberLookup.lookup(ownerX500Name)) {
            "Failed to obtain member information for the specified name: ${request.owner}."
        }

        if (myInfo.name != issuerX500Name) {
            throw IllegalArgumentException("Issuer should be Initiator.")
        }

        val notaryX500Name = requireNotNull(notaryLookup.notaryServices.single().name) {
            "Notary not found."
        }

        val myKey = myInfo.ledgerKeys.first()

        val outputToken = Token(
            issuer = myKey,
            owner = owner.ledgerKeys.first(),
            quantity = NumericDecimal(request.quantity, 2),
            issuerHash = digestService.hash(myKey.encoded, DigestAlgorithmName.SHA2_256),
            ownerHash = digestService.hash(owner.ledgerKeys.first().encoded, DigestAlgorithmName.SHA2_256),
            symbol = request.symbol,
            tag = request.tag
        )

        val sessions =
            if (ownerX500Name == myInfo.name) emptyList() else listOf(flowMessaging.initiateFlow(ownerX500Name))

        val transaction = utxoLedgerService.createTransactionBuilder()
            .setNotary(notaryX500Name)
            .addOutputState(outputToken)
            .addCommand(TokenContract.Issue())
            .setTimeWindowUntil(Instant.now().plus(1, ChronoUnit.DAYS))
            .addSignatories(listOf(myKey, owner.ledgerKeys.first()))
            .toSignedTransaction()

        return try {
            utxoLedgerService.finalize(transaction, sessions)
            log.info("Finalization has been finished")
            "Successfully Issued New Fungible Token(amount:${request.quantity}) To ${request.owner}"
        } catch (e: Exception) {
            "Flow failed, message: ${e.message}"
        }
    }
}

@InitiatedBy(protocol = "issue-fungible-token")
class IssueFungibleTokenResponderFlow : ResponderFlow {

    @CordaInject
    lateinit var utxoLedgerService: UtxoLedgerService

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @Suspendable
    override fun call(session: FlowSession) {
        try {
            val finalizedSignedTransaction = utxoLedgerService.receiveFinality(session) { ledgerTransaction ->
                val states = ledgerTransaction.outputContractStates
                // Check something for SampleToken if you need
            }
            log.info("Finished responder flow - $finalizedSignedTransaction")
        } catch (e: Exception) {
            log.warn("Exceptionally finished responder flow", e)
        }
    }
}

/*
{
  "clientRequestId": "issue-1",
  "flowClassName": "com.r3.token.fungible.workflows.IssueFungibleTokenFlow",
  "requestBody": {
    "issuer": "CN=Alice, OU=Test Dept, O=R3, L=London, C=GB",
    "owner": "CN=Bob, OU=Test Dept, O=R3, L=London, C=GB",
    "quantity": 100,
    "symbol": "USD",
    "tag": ""
  }
}
*/
