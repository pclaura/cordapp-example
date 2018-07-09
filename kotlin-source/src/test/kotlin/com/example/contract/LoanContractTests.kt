package com.example.contract

import com.example.contract.LoanContract.Companion.LOANCONTRACT_CONTRACT_ID
import com.example.state.LoanPaymentState
import com.example.state.LoanState
import net.corda.core.contracts.Amount
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.math.BigDecimal
import java.util.*


class LoanContractTests {

    private val ledgerServices = MockServices()
    private val megaCorp = TestIdentity(CordaX500Name("MegaCorp", "London", "GB"))
    private val miniCorp = TestIdentity(CordaX500Name("MiniCorp", "New York", "US"))

    private val value: Amount<Currency> get() = Amount(1, Currency.getInstance("GBP"))
    private val value2: Amount<Currency> get() = Amount(1, Currency.getInstance("RUB"))
    private val value3: Amount<Currency> get() = Amount(10, Currency.getInstance("GBP"))
    private val value4: Amount<Currency> get() = Amount(100, Currency.getInstance("GBP"))

    private val iR: BigDecimal = BigDecimal(1.25)

    private val payedOff: Amount<Currency> get() = Amount(10, Currency.getInstance("GBP"))
    private val payedOff2: Amount<Currency> get() = Amount(20, Currency.getInstance("RUB"))
    private val payedOff3: Amount<Currency> get() = Amount(1, Currency.getInstance("EUR"))
    private val payedOff4: Amount<Currency> get() = Amount(125, Currency.getInstance("GBP"))



    @Test
    fun `transaction must include Issue command`() {

        ledgerServices.ledger {
            transaction {
                output(LOANCONTRACT_CONTRACT_ID, LoanState(value, iR, payedOff, miniCorp.party, megaCorp.party))
                fails()
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), LoanContract.Commands.Issue())
                verifies()
            }
        }
    }


    @Test
    fun `transaction must include Settle command`() {

        ledgerServices.ledger {
            transaction {
                input(LOANCONTRACT_CONTRACT_ID, LoanState(value4, iR, payedOff4,miniCorp.party, megaCorp.party))
                fails()
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), LoanContract.Commands.Settle())
                verifies()
            }
        }
    }


    /*@Test TODO: add a LoanPayment output
    fun `transaction must have no inputs`() {

        ledgerServices.ledger {
            transaction {
                input(LOANCONTRACT_CONTRACT_ID, LoanState(value, iR, payedOff, miniCorp.party, megaCorp.party))
                output(LOANCONTRACT_CONTRACT_ID, LoanState(value, iR, payedOff, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), LoanContract.Commands.Issue())
                `fails with`("No inputs should be consumed when issuing a LOAN.")
            }
        }
    }*/

    @Test
    fun `transaction must have no two outputs`(){

        ledgerServices.ledger {
            transaction{
                output(LOANCONTRACT_CONTRACT_ID, LoanState(value, iR, payedOff, miniCorp.party, megaCorp.party))
                output(LOANCONTRACT_CONTRACT_ID, LoanState(value, iR, payedOff, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), LoanContract.Commands.Issue())
                `fails with`("Only one output state should be created.")

            }
        }
    }

    @Test
    fun `Lender and borrower cannot be the same`(){
        ledgerServices.ledger {
            transaction{
                output(LOANCONTRACT_CONTRACT_ID, LoanState(value, iR, payedOff, miniCorp.party, miniCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), LoanContract.Commands.Issue())
                `fails with`("The lender and the borrower cannot be the same entity.")
            }
        }
    }


    @Test
    fun `All parties must be signers`(){
        ledgerServices.ledger{
            transaction {
                output(LOANCONTRACT_CONTRACT_ID, LoanState(value, iR, payedOff, miniCorp.party, megaCorp.party))
                command(megaCorp.publicKey, LoanContract.Commands.Issue()) //Only one signature here (megaCorp)
                `fails with`("All of the participants must be signers.")
            }
        }
    }


    @Test
    fun `Currencies allowed are GBP, USD and EUR (borrowed amount)`(){
        ledgerServices.ledger {
            transaction {
                output(LOANCONTRACT_CONTRACT_ID, LoanState(value2, iR, payedOff, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey,miniCorp.publicKey),LoanContract.Commands.Issue())
                `fails with`("The LOAN's borrowed amount must be GBP, USD or EUR.")
            }
        }
    }

    @Test
    fun `Currencies allowed are GBP, USD and EUR (payoff amount)`(){
        ledgerServices.ledger {
            transaction {
                output(LOANCONTRACT_CONTRACT_ID, LoanState(value, iR, payedOff2, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey,miniCorp.publicKey),LoanContract.Commands.Issue())
                `fails with`("The LOAN's payoff amount must be GBP, USD or EUR.")
            }
        }
    }

    @Test
    fun `Borrowed and payoff amount are in the same currency`(){
        ledgerServices.ledger {
            transaction {
                output(LOANCONTRACT_CONTRACT_ID, LoanState(value, iR, payedOff3, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey,miniCorp.publicKey),LoanContract.Commands.Issue())
                `fails with`("The LOAN's borrowed and payoff amount must be in the same currency.")
            }
        }
    }

    @Test
    fun `Payoff amount equals to repayment amount`(){
        ledgerServices.ledger {
            transaction {
                input(LOANCONTRACT_CONTRACT_ID, LoanState(value4, iR, payedOff, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey,miniCorp.publicKey),LoanContract.Commands.Settle())
                `fails with`("The repayment amount and the payoff must be the same.")
            }
        }
    }

}