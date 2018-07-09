package com.example.contract

import com.example.state.LoanState
import com.example.state.LoanPaymentState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.contracts.requireThat

open class LoanPaymentContract : Contract{


    companion object {
        @JvmStatic
        val LOANPAYMENTCONTRACT_CONTRACT_ID = "com.example.contract.LoanPaymentContract"
    }


    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    override fun verify(tx: LedgerTransaction) {

        val commandIssue = tx.commands.requireSingleCommand<Commands.Issue>()

        requireThat {
            // Generic constraints around the IOU transaction.
            "Only one input state should be consumed." using (tx.inputs.size == 1)
            "Two output states should be created." using (tx.outputs.size == 2)

            val inLoan = tx.inputsOfType<LoanState>().single()
            val outLoanPayment = tx.outputsOfType<LoanPaymentState>().single()
            val outLoan = tx.outputsOfType<LoanState>().single()


            "The lender and the borrower cannot be the same entity." using (outLoanPayment.lender != outLoanPayment.borrower)

            // Check all participants are signers
            "All of the participants must be signers." using (commandIssue.signers.containsAll(outLoanPayment.participants.map { it.owningKey }))

            //Check the output Loan Payment values
            "The borrower must not change." using (inLoan.borrower == outLoanPayment.borrower)
            "The lender must not change." using (inLoan.lender == outLoanPayment.lender)

            // Loan Payment specific constraints.
            "The loan payment must be GBP, USD or EUR." using (listOf("GBP","USD","EUR").contains(outLoanPayment.payment.token.toString()))
            "Loan payments must be made in the same currency as the loan." using (outLoanPayment.payment.token.toString() == inLoan.borrowedAmount.token.toString())
            "Loan payments cannot exceed the loan quantity." using (outLoanPayment.payment.quantity <= inLoan.repaymentAmount.quantity)


            // Check all participants are signers
            "All of the participants must be signers." using (commandIssue.signers.containsAll(outLoan.participants.map { it.owningKey }))

            //Check the output Loan values
            "The borrower must not change." using (inLoan.borrower == outLoan.borrower)
            "The lender must not change." using (inLoan.lender == outLoan.lender)
            "The interest rate must not change." using (inLoan.interestRate == outLoan.interestRate)
            "The loan id must not change." using (inLoan.linearId == outLoan.linearId)

        }
    }



    /**
     * This contract only implements one command for the loan repayment,
     * 1. Issue loan (occurs when a payment is made against the loan)
     * A loan payment can only be issued
     */
    interface Commands : CommandData {
        class Issue : TypeOnlyCommandData(), Commands
    }
}