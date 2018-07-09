package com.example.contract

import com.example.state.LoanState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.contracts.requireThat

open class LoanContract : Contract {

    companion object {
        @JvmStatic
        val LOANCONTRACT_CONTRACT_ID = "com.example.contract.LoanContract"
    }


    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    override fun verify(tx: LedgerTransaction) {

        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value) {

            is Commands.Issue -> { //Issue verification logic

                //Inputs and Outputs:
                val inputLoanStates = tx.inputsOfType<LoanState>()

                requireThat {

                    if (inputLoanStates.isNotEmpty()) { //If have an input, it means a loan payment was made

                        // Generic constraints around the LOAN transaction.
                        "Only one input should be consumed." using (tx.inputs.size == 1)
                        "Two outputs should be created: loan and loan payment" using (tx.outputs.size == 2)

                    } else { //Issuance with no inputs consumed

                        // Generic constraints around the LOAN transaction.
                        "No inputs should be consumed when issuing a LOAN." using (tx.inputs.isEmpty())
                        "Only one output state should be created." using (tx.outputs.size == 1)
                    }


                    val outLoan = tx.outputsOfType<LoanState>().single()

                    "The lender and the borrower cannot be the same entity." using (outLoan.lender != outLoan.borrower)
                    "All of the participants must be signers." using (command.signers.containsAll(outLoan.participants.map { it.owningKey }))

                    // LOAN-specific constraints.
                    "The LOAN's borrowed amount must be GBP, USD or EUR." using (listOf("GBP", "USD", "EUR").contains(outLoan.borrowedAmount.token.toString()))
                    "The LOAN's payoff amount must be GBP, USD or EUR." using (listOf("GBP", "USD", "EUR").contains(outLoan.payedOffAmount.token.toString()))
                    "The LOAN's borrowed and payoff amount must be in the same currency." using (outLoan.borrowedAmount.token.toString() == outLoan.payedOffAmount.token.toString())


                }

            }

            is Commands.Settle -> { //Settle verification logic

                requireThat {
                    // Check there is an input loan and no outputs.
                    "Only one input state should be consumed." using (tx.inputs.size == 1) //todo: test
                    "No outputs." using (tx.outputs.isEmpty()) //todo: test

                    val inputLoan = tx.inputsOfType<LoanState>().single()
                    //Check settle condition
                    "The repayment amount and the payoff must be the same." using (inputLoan.repaymentAmount == inputLoan.payedOffAmount)

                    println("SETTLE DONE!")
                }


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