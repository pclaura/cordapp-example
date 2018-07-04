package com.example.contract

import com.example.state.LoanState
import com.example.state.LoanPaymentState
import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.using
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.contracts.requireThat

open class LoanContract : Contract {

    companion object {
        @JvmStatic
        val LOANCONTRACT_CONTRACT_ID = "com.example.contract.LoanContract" //Se usa en el test
    }


    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    override fun verify(tx: LedgerTransaction) {

        val command = tx.commands.requireSingleCommand<Commands>()
        //val commandIssue = tx.commands.requireSingleCommand<Commands.Issue>()
        //val commandSettle = tx.commands.requireSingleCommand<Commands.Settle>()
        when (command.value) {

            is Commands.Issue -> {

                requireThat {
                    //Aqui se insertan todas las reglas necesarias para considerar valida la transaccion
                    // Generic constraints around the IOU transaction.
                    "No inputs should be consumed when issuing a LOAN." using (tx.inputs.isEmpty())
                    "Only one output state should be created." using (tx.outputs.size == 1)

                    val outLoan = tx.outputsOfType<LoanState>().single()
                    "The lender and the borrower cannot be the same entity." using (outLoan.lender != outLoan.borrower)
                    "All of the participants must be signers." using (command.signers.containsAll(outLoan.participants.map { it.owningKey }))
                    //"All of the participants must be signers." using (commandSettle.signers.containsAll(outLoan.participants.map { it.owningKey }))

                    // IOU-specific constraints.
                    "The interest must be non-negative." using (outLoan.interestRate.toLong() >= 0)
                    // Constraint to only allow issuance of IOUs in GBP, USD or EUR.
                    "The IOU's issuance must be GBP, USD or EUR." using (listOf("GBP", "USD", "EUR").contains(outLoan.borrowedAmount.token.toString()))

                }

            }

            is Commands.Settle -> { //TODO: NOT SURE AT ALL, Habria que poner otra vez las comprobaciones de moneda y todo eso?
                "Only one output state should be created." using (tx.outputs.size == 1) //Creates a LOAN State
                "Only one input state should be consumed." using (tx.inputs.size == 1) //Must have a LOAN PAYMENT as an input

                val intLoanP = tx.inputsOfType<LoanPaymentState>().single()
                val outLoan = tx.outputsOfType<LoanState>().single()
                "The value must be equal to the remain loan." using (intLoanP.payment.quantity == intLoanP.remain.quantity ) //El pago tiene que ser igual a la cantidad que falta
                "The value of the output loan must be 0." using (outLoan.borrowedAmount.quantity.toInt() == 0) //La cantidad a deber del prestamo tiene que quedar a 0


            }
        }


    }


    /**
     * This contract only implements two commands for the loan,
     * 1. Issue loan (issuance of a loan from a lender to a borrower)
     * 2. Settle loan (occurs when all loan payments have been completed)
     */
    interface Commands : CommandData {
        class Issue : TypeOnlyCommandData(), Commands
        class Settle : TypeOnlyCommandData(), Commands
    }
}