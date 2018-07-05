package com.example.flow

import co.paralleluniverse.fibers.Suspendable
import com.example.contract.LoanContract
import com.example.state.LoanState
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.math.BigDecimal
import java.util.*
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat

object LoanIssuance {
    @InitiatingFlow
    @StartableByRPC
    class InitiatorFlow(val borrowedAmount: Amount<Currency>,
                        val ir: BigDecimal,
                        val otherParty: Party
                        ): FlowLogic<SignedTransaction>() {

        companion object {
            object GENERATING_TRANSACTION:ProgressTracker.Step("Generating transaction based on a new LOAN.")
            object VERIFYING_TRANSACTION:ProgressTracker.Step("Verifying contract constraints.")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
            object GATHERING_SIGS : ProgressTracker.Step("Gathering the counterparty's signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }
            object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(
                    GENERATING_TRANSACTION,
                    VERIFYING_TRANSACTION,
                    SIGNING_TRANSACTION,
                    GATHERING_SIGS,
                    FINALISING_TRANSACTION
            )
        }//Close companion object


        override val progressTracker = tracker()

        /** Flow logic
         *
         */
        @Suspendable
        override fun call(): SignedTransaction {
            // Obtain a reference to the notary we want to use.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            // Stage 1.
            progressTracker.currentStep = GENERATING_TRANSACTION
            // Generate an unsigned transaction.
            val loanState = LoanState(borrowedAmount,ir,serviceHub.myInfo.legalIdentities.first(), otherParty) //The State
            val txCommand = Command(LoanContract.Commands.Issue(), loanState.participants.map { it.owningKey }) //Command: Issue
            val txBuilder = TransactionBuilder(notary) //Tx builder
                    .addOutputState(loanState, LoanContract.LOANCONTRACT_CONTRACT_ID)
                    .addCommand(txCommand)

            // Stage 2.
            progressTracker.currentStep = VERIFYING_TRANSACTION
            // Verify that the transaction is valid.
            txBuilder.verify(serviceHub)

            // Stage 3.
            progressTracker.currentStep = SIGNING_TRANSACTION
            // Sign the transaction.
            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

            // Stage 4.
            progressTracker.currentStep = GATHERING_SIGS
            // Send the state to the counterparty, and receive it back with their signature.

            val requiredSignatureFlowSessions = listOf(
                    loanState.borrower,
                    loanState.lender)
                    .filter { !serviceHub.myInfo.legalIdentities.contains(it) }
                    .distinct()
                    .map { initiateFlow(serviceHub.identityService.requireWellKnownPartyFromAnonymous(it)) }

            val fullySignedTx = subFlow(CollectSignaturesFlow(
                    partSignedTx,
                    requiredSignatureFlowSessions,
                    GATHERING_SIGS.childProgressTracker()))

            // Stage 5.
            progressTracker.currentStep = FINALISING_TRANSACTION
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(FinalityFlow(fullySignedTx, LoanIssuance.InitiatorFlow.Companion.FINALISING_TRANSACTION.childProgressTracker()))
        }//Close call function

    }//Close InitiatorFlow


    @InitiatedBy(InitiatorFlow::class)
    class Acceptor(val otherPartyFlow: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartyFlow) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be a LOAN transaction." using (output is LoanState)
                }
            }

            return subFlow(signTransactionFlow)
        }
    }
}