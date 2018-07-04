package com.example.contract

import com.example.state.LoanState
import com.example.state.LoanPaymentState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.contracts.requireThat

open class LoanPaymentContract : Contract{


    companion object {
        @JvmStatic
        val LOANPAYMENTCONTRACT_CONTRACT_ID = "com.example.contract.LoanPaymentContract" //Se usa en el test
    }


    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    override fun verify(tx: LedgerTransaction) {

        val commandIssue = tx.commands.requireSingleCommand<Commands.Issue>()

        requireThat { //Aqui se insertan todas las reglas necesarias para considerar valida la transaccion
            // Generic constraints around the IOU transaction.
            "Only one input state should be consumed." using (tx.inputs.size == 1)
            "Only one output state should be created." using (tx.outputs.size == 1)

            val inLoan = tx.inputsOfType<LoanPaymentState>().single()
            val outLoan = tx.outputsOfType<LoanState>().single()

            "The lender and the borrower cannot be the same entity." using (inLoan.lender != inLoan.borrower)
            "All of the participants must be signers." using (commandIssue.signers.containsAll(inLoan.participants.map { it.owningKey }))

            // IOU-specific constraints.
            "The interest must be non-negative." using (inLoan.interestRate.toLong() >= 0)

            "The loan payment must be GBP, USD or EUR." using (listOf("GBP","USD","EUR").contains(inLoan.payment.token.toString()))


            "Loan payments must be made in the same currency as the loan." using (inLoan.payment.token.toString() == outLoan.borrowedAmount.token.toString())

            "Loan payments cannot exceed the remain quantity." using (inLoan.repaymentAmount.quantity <= inLoan.remain.quantity)
        }
    }



    /**
     * This contract only implements one command for the loan repayment,
     * 1. Issue loan (occurs when a payment is made against the loan)
     */
    interface Commands : CommandData {
        class Issue : TypeOnlyCommandData(), Commands
    }
}