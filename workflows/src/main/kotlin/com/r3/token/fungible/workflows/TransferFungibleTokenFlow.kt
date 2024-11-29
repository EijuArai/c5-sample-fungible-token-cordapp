package com.r3.token.fungible.workflows

import com.r3.corda.ledger.utxo.fungible.NumericDecimal
import com.r3.corda.ledger.utxo.ownable.query.OwnableStateQueries
import com.r3.sum
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
import net.corda.v5.ledger.utxo.StateAndRef
import net.corda.v5.ledger.utxo.UtxoLedgerService
import net.corda.v5.ledger.utxo.token.selection.TokenClaimCriteria
import net.corda.v5.ledger.utxo.token.selection.TokenSelection
import org.slf4j.LoggerFactory
import java.lang.IllegalArgumentException
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit


@InitiatingFlow(protocol = "transfer-fungible-token")
@Suppress("unused")
class TransferFungibleTokenFlow : ClientStartableFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    data class TransferFungibleTokenRequest(
        val issuer: String,
        val owner: String,
        val newOwner: String,
        val quantity: BigDecimal,
        val symbol: String,
        val tag: String
    )

    @CordaInject
    private lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    private lateinit var utxoLedgerService: UtxoLedgerService

    @CordaInject
    private lateinit var memberLookup: MemberLookup

    @CordaInject
    private lateinit var flowMessaging: FlowMessaging

    @CordaInject
    private lateinit var flowEngine: FlowEngine

    @CordaInject
    private lateinit var digestService: DigestService

    @CordaInject
    private lateinit var notaryLookup: NotaryLookup

    @CordaInject
    lateinit var tokenSelection: TokenSelection

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        log.info("TransferFungibleTokenFlow.call() called")
        val request = requestBody.getRequestBodyAs(jsonMarshallingService, TransferFungibleTokenRequest::class.java)

        val issuerX500Name = MemberX500Name.parse(request.issuer)
        val ownerX500Name = MemberX500Name.parse(request.owner)
        val newOwnerX500Name = MemberX500Name.parse(request.newOwner)
        val myInfo = memberLookup.myInfo()

        val issuer = requireNotNull(memberLookup.lookup(issuerX500Name)) {
            "Failed to obtain member information for the specified name: ${request.issuer}."
        }
        val newOwner = requireNotNull(memberLookup.lookup(newOwnerX500Name)) {
            "Failed to obtain member information for the specified name: ${request.newOwner}."
        }
        if (myInfo.name != ownerX500Name) {
            throw IllegalArgumentException("Owner should be Initiator.")
        }

        val notaryX500Name = requireNotNull(notaryLookup.notaryServices.single().name) {
            "Notary not found."
        }

        val issuerKeys = issuer.ledgerKeys
        val myKeys = myInfo.ledgerKeys
        val newOwnerKeys = newOwner.ledgerKeys
        val issuerHash = digestService.hash(issuerKeys.first().encoded, DigestAlgorithmName.SHA2_256)
        val ownerHash = digestService.hash(myKeys.first().encoded, DigestAlgorithmName.SHA2_256)
        val newOwnerHash = digestService.hash(newOwnerKeys.first().encoded, DigestAlgorithmName.SHA2_256)

        log.info("Start Token Selection")

        val criteria = TokenClaimCriteria(
            Token.tokenType,
            issuerHash,
            notaryX500Name,
            request.symbol,
            request.quantity
        ).apply { this.ownerHash = ownerHash }
        val claim =
            tokenSelection.tryClaim("transfer", criteria) ?: return jsonMarshallingService.format("Insufficient Token Amount")
        val spentTokenRefs = claim.claimedTokens.map { it.stateRef }
        val spentTokenAmount = claim.claimedTokens.sumOf { it.amount }

        val quantity = NumericDecimal(request.quantity, 2)

        val outputs = mutableListOf<Token>()
        outputs.add(
            Token(
                issuerKeys.first(),
                newOwnerKeys.first(),
                quantity,
                issuerHash,
                newOwnerHash,
                request.symbol,
                request.tag
            )
        )

        val changeAmount = NumericDecimal(spentTokenAmount - request.quantity)
        if (changeAmount > NumericDecimal(BigDecimal(0))) {
            outputs.add(
                Token(
                    issuerKeys.first(),
                    myKeys.first(),
                    changeAmount,
                    issuerHash,
                    ownerHash,
                    request.symbol,
                    request.tag
                )
            )
        }

        val newOwnerSession = flowMessaging.initiateFlow(newOwnerX500Name)

        val transaction = utxoLedgerService.createTransactionBuilder()
            .addInputStates(spentTokenRefs)
            .addOutputStates(outputs)
            .addCommand(TokenContract.Transfer())
            .addSignatories(issuerKeys.first())
            .addSignatories(myKeys.first())
            .setNotary(notaryX500Name)
            .setTimeWindowUntil(Instant.now().plus(1, ChronoUnit.DAYS))
            .toSignedTransaction()

        return try {
            utxoLedgerService.finalize(transaction, listOf(newOwnerSession))
            log.info("Finalization has been finished")
            "Successfully Transferred Fungible Token(amount:${request.quantity}) From ${request.owner} To ${request.newOwner}"
        } catch (e: Exception) {
            "Flow failed, message: ${e.message}"
        }
    }
}

@InitiatedBy(protocol = "transfer-fungible-token")
class TransferFungibleTokenResponderFlow : ResponderFlow {

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
  "clientRequestId": "transfer-1",
  "flowClassName": "com.r3.token.fungible.workflows.TransferFungibleTokenFlow",
  "requestBody": {
    "issuer": "CN=Alice, OU=Test Dept, O=R3, L=London, C=GB",
    "owner": "CN=Bob, OU=Test Dept, O=R3, L=London, C=GB",
    "newOwner": "CN=Alice, OU=Test Dept, O=R3, L=London, C=GB",
    "quantity": 50,
    "symbol": "USD",
    "tag": ""
  }
}
*/
