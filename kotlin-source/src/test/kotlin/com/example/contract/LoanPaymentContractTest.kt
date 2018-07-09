package com.example.contract

import com.example.contract.LoanPaymentContract.Companion.LOANPAYMENTCONTRACT_CONTRACT_ID
import com.example.state.LoanState
import com.example.state.LoanPaymentState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.math.BigDecimal
import java.util.*

class LoanPaymentContractTest {

    private val ledgerServices = MockServices()
    private val megaCorp = TestIdentity(CordaX500Name("MegaCorp", "London", "GB"))
    private val miniCorp = TestIdentity(CordaX500Name("MiniCorp", "New York", "US"))
    private val otherCorp = TestIdentity(CordaX500Name("OtherCorp", "New York", "US"))

    private val amountLoan: Amount<Currency> get() = Amount(100, Currency.getInstance("GBP"))
    private val amountLoan2: Amount<Currency> get() = Amount(50, Currency.getInstance("GBP"))
    private val amountLoan3: Amount<Currency> get() = Amount(20, Currency.getInstance("GBP"))

    private val payoff: Amount<Currency> get() = Amount(0, Currency.getInstance("GBP"))
    private val payoff2: Amount<Currency> get() = Amount(50, Currency.getInstance("GBP"))

    private val pay: Amount<Currency> get() = Amount(50, Currency.getInstance("GBP"))
    private val pay2: Amount<Currency> get() = Amount(50, Currency.getInstance("RUB"))
    private val pay3: Amount<Currency> get() = Amount(50, Currency.getInstance("EUR"))

    private val iR: BigDecimal = BigDecimal(1.25)
    private val iR2: BigDecimal = BigDecimal(2.25)

    @Test
    fun `transaction must include Issue command`() {

        ledgerServices.ledger {
            transaction {
                val inLoan = LoanState(amountLoan,iR,payoff,megaCorp.party,miniCorp.party)
                input(LOANPAYMENTCONTRACT_CONTRACT_ID, inLoan)
                output(LOANPAYMENTCONTRACT_CONTRACT_ID, LoanPaymentState(pay,inLoan.linearId,megaCorp.party,miniCorp.party))
                output(LOANPAYMENTCONTRACT_CONTRACT_ID, LoanState(amountLoan2,iR,payoff2,megaCorp.party,miniCorp.party,inLoan.linearId))

                fails()
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), LoanPaymentContract.Commands.Issue())
                verifies()
            }
        }
    }

    @Test
    fun `Only one input state should be consumed`(){
        ledgerServices.ledger {
            transaction {
                val inLoan = LoanState(amountLoan,iR,payoff,megaCorp.party,miniCorp.party)
                input(LOANPAYMENTCONTRACT_CONTRACT_ID, inLoan)
                input(LOANPAYMENTCONTRACT_CONTRACT_ID, inLoan)
                output(LOANPAYMENTCONTRACT_CONTRACT_ID, LoanPaymentState(pay,inLoan.linearId,megaCorp.party,miniCorp.party))
                output(LOANPAYMENTCONTRACT_CONTRACT_ID, LoanState(amountLoan2,iR,payoff2,megaCorp.party,miniCorp.party))

                command(listOf(megaCorp.publicKey, miniCorp.publicKey), LoanPaymentContract.Commands.Issue())
                `fails with`("Only one input state should be consumed.")
            }
        }
    }

    @Test
    fun `Two outputs must be created`(){
        ledgerServices.ledger {
            transaction {
                val inLoan = LoanState(amountLoan,iR,payoff,megaCorp.party,miniCorp.party)
                input(LOANPAYMENTCONTRACT_CONTRACT_ID, inLoan)
                output(LOANPAYMENTCONTRACT_CONTRACT_ID, LoanPaymentState(pay,inLoan.linearId,megaCorp.party,miniCorp.party))

                command(listOf(megaCorp.publicKey, miniCorp.publicKey), LoanPaymentContract.Commands.Issue())
                `fails with`("Two output states should be created.")
            }
        }
    }

    @Test
    fun `The lender and borrower not the same`(){
        ledgerServices.ledger {
            transaction {
                val inLoan = LoanState(amountLoan,iR,payoff,megaCorp.party,miniCorp.party)
                input(LOANPAYMENTCONTRACT_CONTRACT_ID, inLoan)
                output(LOANPAYMENTCONTRACT_CONTRACT_ID, LoanPaymentState(pay,inLoan.linearId,miniCorp.party,miniCorp.party))
                output(LOANPAYMENTCONTRACT_CONTRACT_ID, LoanState(amountLoan2,iR,payoff2,megaCorp.party,miniCorp.party))

                command(listOf(megaCorp.publicKey, miniCorp.publicKey), LoanPaymentContract.Commands.Issue())
                `fails with`("The lender and the borrower cannot be the same entity.")
            }
        }
    }

    @Test
    fun `All participants must be signers`(){
        ledgerServices.ledger {
            transaction {
                val inLoan = LoanState(amountLoan,iR,payoff,megaCorp.party,miniCorp.party)
                input(LOANPAYMENTCONTRACT_CONTRACT_ID, inLoan)
                output(LOANPAYMENTCONTRACT_CONTRACT_ID, LoanPaymentState(pay,inLoan.linearId,megaCorp.party,miniCorp.party))
                output(LOANPAYMENTCONTRACT_CONTRACT_ID, LoanState(amountLoan2,iR,payoff2,megaCorp.party,miniCorp.party))

                command(miniCorp.publicKey, LoanPaymentContract.Commands.Issue())
                `fails with`("All of the participants must be signers.")
            }
        }
    }

    @Test
    fun `Borrower must not change in the loan payment`(){
        ledgerServices.ledger {
            transaction {
                val inLoan = LoanState(amountLoan,iR,payoff,megaCorp.party,otherCorp.party)
                input(LOANPAYMENTCONTRACT_CONTRACT_ID, inLoan)
                output(LOANPAYMENTCONTRACT_CONTRACT_ID, LoanPaymentState(pay,inLoan.linearId,megaCorp.party,miniCorp.party))
                output(LOANPAYMENTCONTRACT_CONTRACT_ID, LoanState(amountLoan2,iR,payoff2,megaCorp.party,miniCorp.party))

                command(listOf(megaCorp.publicKey, miniCorp.publicKey), LoanPaymentContract.Commands.Issue())
                `fails with`("The borrower must not change.")
            }
        }
    }

    @Test
    fun `Lender must not change in the loan payment`(){
        ledgerServices.ledger {
            transaction {
                val inLoan = LoanState(amountLoan,iR,payoff,megaCorp.party,miniCorp.party)
                input(LOANPAYMENTCONTRACT_CONTRACT_ID, inLoan)
                output(LOANPAYMENTCONTRACT_CONTRACT_ID, LoanPaymentState(pay,inLoan.linearId,otherCorp.party,miniCorp.party))
                output(LOANPAYMENTCONTRACT_CONTRACT_ID, LoanState(amountLoan2,iR,payoff2,megaCorp.party,miniCorp.party))

                command(listOf(otherCorp.publicKey, miniCorp.publicKey), LoanPaymentContract.Commands.Issue())
                `fails with`("The lender must not change.")
            }
        }
    }


    @Test
    fun `Currencies accepted are GBP, USD or EUR`(){
        ledgerServices.ledger {
            transaction {
                val inLoan = LoanState(amountLoan,iR,payoff,megaCorp.party,miniCorp.party)
                input(LOANPAYMENTCONTRACT_CONTRACT_ID, inLoan)
                output(LOANPAYMENTCONTRACT_CONTRACT_ID, LoanPaymentState(pay2,inLoan.linearId,megaCorp.party,miniCorp.party))
                output(LOANPAYMENTCONTRACT_CONTRACT_ID, LoanState(amountLoan2,iR,payoff2,megaCorp.party,miniCorp.party))

                command(listOf(megaCorp.publicKey, miniCorp.publicKey), LoanPaymentContract.Commands.Issue())
                `fails with`("The loan payment must be GBP, USD or EUR.")
            }
        }
    }

    @Test
    fun `Payments must be in the same currency`(){
        ledgerServices.ledger {
            transaction {
                val inLoan = LoanState(amountLoan,iR,payoff,megaCorp.party,miniCorp.party)
                input(LOANPAYMENTCONTRACT_CONTRACT_ID, inLoan)
                output(LOANPAYMENTCONTRACT_CONTRACT_ID, LoanPaymentState(pay3,inLoan.linearId,megaCorp.party,miniCorp.party))
                output(LOANPAYMENTCONTRACT_CONTRACT_ID, LoanState(amountLoan2,iR,payoff2,megaCorp.party,miniCorp.party))

                command(listOf(megaCorp.publicKey, miniCorp.publicKey), LoanPaymentContract.Commands.Issue())
                `fails with`("Loan payments must be made in the same currency as the loan.")
            }
        }
    }

    @Test
    fun `Payments cannot exceed the loan amount`(){
        ledgerServices.ledger {
            transaction {
                val inLoan = LoanState(amountLoan3,iR,payoff,megaCorp.party,miniCorp.party)
                input(LOANPAYMENTCONTRACT_CONTRACT_ID, inLoan)
                output(LOANPAYMENTCONTRACT_CONTRACT_ID, LoanPaymentState(pay,inLoan.linearId,megaCorp.party,miniCorp.party))
                output(LOANPAYMENTCONTRACT_CONTRACT_ID, LoanState(amountLoan2,iR,payoff2,megaCorp.party,miniCorp.party))

                command(listOf(megaCorp.publicKey, miniCorp.publicKey), LoanPaymentContract.Commands.Issue())
                `fails with`("Loan payments cannot exceed the loan quantity.")
            }
        }
    }

    @Test
    fun `All participants must be signers (out loan)`(){
        ledgerServices.ledger {
            transaction {
                val inLoan = LoanState(amountLoan3,iR,payoff,megaCorp.party,miniCorp.party)
                input(LOANPAYMENTCONTRACT_CONTRACT_ID, inLoan)
                output(LOANPAYMENTCONTRACT_CONTRACT_ID, LoanPaymentState(pay,inLoan.linearId,megaCorp.party,miniCorp.party))
                output(LOANPAYMENTCONTRACT_CONTRACT_ID, LoanState(amountLoan2,iR,payoff2,megaCorp.party,miniCorp.party))

                command(miniCorp.publicKey, LoanPaymentContract.Commands.Issue())
                `fails with`("All of the participants must be signers.")
            }
        }
    }

    @Test
    fun `Borrower must not change in the out loan`(){
        ledgerServices.ledger {
            transaction {
                val inLoan = LoanState(amountLoan,iR,payoff,megaCorp.party,miniCorp.party)
                input(LOANPAYMENTCONTRACT_CONTRACT_ID, inLoan)
                output(LOANPAYMENTCONTRACT_CONTRACT_ID, LoanPaymentState(pay,inLoan.linearId,megaCorp.party,miniCorp.party))
                output(LOANPAYMENTCONTRACT_CONTRACT_ID, LoanState(amountLoan2,iR,payoff2,megaCorp.party,otherCorp.party))

                command(listOf(megaCorp.publicKey, miniCorp.publicKey, otherCorp.publicKey), LoanPaymentContract.Commands.Issue())
                `fails with`("The borrower must not change.")
            }
        }
    }

    @Test
    fun `Lender must not change in the out loan`(){
        ledgerServices.ledger {
            transaction {
                val inLoan = LoanState(amountLoan,iR,payoff,megaCorp.party,miniCorp.party)
                input(LOANPAYMENTCONTRACT_CONTRACT_ID, inLoan)
                output(LOANPAYMENTCONTRACT_CONTRACT_ID, LoanPaymentState(pay,inLoan.linearId,megaCorp.party,miniCorp.party))
                output(LOANPAYMENTCONTRACT_CONTRACT_ID, LoanState(amountLoan2,iR,payoff2,otherCorp.party,miniCorp.party))

                command(listOf(megaCorp.publicKey, miniCorp.publicKey, otherCorp.publicKey), LoanPaymentContract.Commands.Issue())
                `fails with`("The lender must not change.")
            }
        }
    }

    @Test
    fun `Interest rate must not change`(){
        ledgerServices.ledger {
            transaction {
                val inLoan = LoanState(amountLoan,iR,payoff,megaCorp.party,miniCorp.party)
                input(LOANPAYMENTCONTRACT_CONTRACT_ID, inLoan)
                output(LOANPAYMENTCONTRACT_CONTRACT_ID, LoanPaymentState(pay,inLoan.linearId,megaCorp.party,miniCorp.party))
                output(LOANPAYMENTCONTRACT_CONTRACT_ID, LoanState(amountLoan2,iR2,payoff2,megaCorp.party,miniCorp.party))

                command(listOf(megaCorp.publicKey, miniCorp.publicKey), LoanPaymentContract.Commands.Issue())
                `fails with`("The interest rate must not change.")
            }
        }
    }

    @Test
    fun `Loan id must not change`(){
        ledgerServices.ledger {
            transaction {
                val inLoan = LoanState(amountLoan,iR,payoff,megaCorp.party,miniCorp.party)
                input(LOANPAYMENTCONTRACT_CONTRACT_ID, inLoan)
                output(LOANPAYMENTCONTRACT_CONTRACT_ID, LoanPaymentState(pay,inLoan.linearId,megaCorp.party,miniCorp.party))
                val linearId2 = UniqueIdentifier()
                output(LOANPAYMENTCONTRACT_CONTRACT_ID, LoanState(amountLoan2,iR,payoff2,megaCorp.party,miniCorp.party,linearId2))

                command(listOf(megaCorp.publicKey, miniCorp.publicKey), LoanPaymentContract.Commands.Issue())
                `fails with`("The loan id must not change.")
            }
        }
    }

}