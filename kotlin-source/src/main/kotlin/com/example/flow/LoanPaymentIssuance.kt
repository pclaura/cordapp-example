package com.example.flow

import co.paralleluniverse.fibers.Suspendable
import com.example.contract.LoanContract
import com.example.contract.LoanPaymentContract
import com.example.state.LoanPaymentState
import com.example.state.LoanState
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria.LinearStateQueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.util.*



object LoanPaymentIssuance {
    @InitiatingFlow
    @StartableByRPC
    class InitiatorFlow(val payment: Amount<Currency>,
                        val loanID: UniqueIdentifier,
                        val otherParty: Party
    ) : FlowLogic<SignedTransaction>() {
        companion object {
            object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction based on an input LOAN.")
            object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
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
        }

        override val progressTracker = tracker()

        @Suspendable
        override fun call(): SignedTransaction {
            // Obtain a reference to the notary we want to use.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            // Stage 1.
            progressTracker.currentStep = GENERATING_TRANSACTION
            // Generate an unsigned transaction.

            //Get the input loan State (with a specific id) from the vault
            val criteria = LinearStateQueryCriteria(linearId = listOf(loanID)) //Set the criteria
            val result = serviceHub.vaultService.queryBy<LoanState>(criteria) //get the Loan State with the id specified
            val states = result.states
            val inLoan = states.single().state.data //Get the data

            val outputLoanPayment = LoanPaymentState(payment,inLoan.linearId,serviceHub.myInfo.legalIdentities.first(),otherParty)

            val outLoan = inLoan.copy(payedOffAmount = inLoan.payedOffAmount.plus(payment))

            val txCommandLoanPaymentIssue = Command(LoanPaymentContract.Commands.Issue(),outputLoanPayment.participants.map{it.owningKey})
            val txCommandLoanIssue = Command(LoanContract.Commands.Issue(),outLoan.participants.map{it.owningKey})


            val txBuilder = TransactionBuilder(notary) //Tx builder

                    .addInputState(states.single())
                    .addOutputState(outputLoanPayment, LoanPaymentContract.LOANPAYMENTCONTRACT_CONTRACT_ID) //Output Loan Payment
                    .addOutputState(outLoan, LoanContract.LOANCONTRACT_CONTRACT_ID) //Output Loan
                    .addCommand(txCommandLoanPaymentIssue)
                    .addCommand(txCommandLoanIssue)


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

            val requiredSignatureFlowSessions = listOf( //The required signatures
                    outLoan.borrower,
                    outLoan.lender)
                    .filter { !serviceHub.myInfo.legalIdentities.contains(it) }
                    .distinct()
                    .map { initiateFlow(serviceHub.identityService.requireWellKnownPartyFromAnonymous(it)) }

            val fullySignedTx = subFlow(CollectSignaturesFlow( //Gathering them
                    partSignedTx,
                    requiredSignatureFlowSessions,
                    GATHERING_SIGS.childProgressTracker()))

            // Stage 5.
            progressTracker.currentStep = FINALISING_TRANSACTION
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(FinalityFlow(fullySignedTx, FINALISING_TRANSACTION.childProgressTracker()))
        }//Close call function

    }//Close InitiatorFlow

    @InitiatedBy(InitiatorFlow::class)
    class Acceptor(val otherPartyFlow: FlowSession) : FlowLogic<SignedTransaction>(){
        @Suspendable
    override fun call(): SignedTransaction{
            val signTransactionFlow = object : SignTransactionFlow(otherPartyFlow){
                override fun checkTransaction(stx: SignedTransaction) = requireThat{
                    val outLoan = stx.tx.outputsOfType<LoanState>().single()
                    "This must be a LOAN transaction." using (outLoan is LoanState)
                    val outLoanPayment = stx.tx.outputsOfType<LoanPaymentState>().single()
                    "This must be a LOAN PAYMENT transaction." using (outLoanPayment is LoanPaymentState)
                }
            }

            return subFlow(signTransactionFlow)
        }
    }
}