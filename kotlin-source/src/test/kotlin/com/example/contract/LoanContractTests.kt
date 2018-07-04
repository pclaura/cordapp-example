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
    private val iR: BigDecimal = BigDecimal(1)
    private val iR2: BigDecimal = BigDecimal(-1)
    private val remain: Amount<Currency> get() = Amount(10, Currency.getInstance("GBP"))
    private val remain2: Amount<Currency> get() = Amount(20, Currency.getInstance("GBP"))
    private val value3: Amount<Currency> get() = Amount(10, Currency.getInstance("GBP"))
    private val value4: Amount<Currency> get() = Amount(0, Currency.getInstance("GBP"))


    @Test
    fun `transaction must include Issue command`() {

        ledgerServices.ledger {
            transaction {
                output(LOANCONTRACT_CONTRACT_ID, LoanState(value, iR, miniCorp.party, megaCorp.party))
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
                input(LOANCONTRACT_CONTRACT_ID, LoanPaymentState(value3, iR, remain, miniCorp.party, megaCorp.party))
                output(LOANCONTRACT_CONTRACT_ID, LoanState(value4, iR, miniCorp.party, megaCorp.party))
                fails()
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), LoanContract.Commands.Settle())
                verifies()
            }
        }
    }

    @Test
    fun `transaction must have no inputs`() {

        ledgerServices.ledger {
            transaction {
                input(LOANCONTRACT_CONTRACT_ID, LoanState(value, iR, miniCorp.party, megaCorp.party))
                output(LOANCONTRACT_CONTRACT_ID, LoanState(value, iR, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), LoanContract.Commands.Issue())
                `fails with`("No inputs should be consumed when issuing a LOAN.")
            }
        }
    }

    @Test
    fun `transaction must have no two outputs`(){

        ledgerServices.ledger {
            transaction{
                output(LOANCONTRACT_CONTRACT_ID, LoanState(value, iR, miniCorp.party, megaCorp.party))
                output(LOANCONTRACT_CONTRACT_ID, LoanState(value, iR, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), LoanContract.Commands.Issue())
                `fails with`("Only one output state should be created.")

            }
        }
    }

    @Test
    fun `Lender and borrower cannot be the same`(){
        ledgerServices.ledger {
            transaction{
                output(LOANCONTRACT_CONTRACT_ID, LoanState(value, iR, miniCorp.party, miniCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), LoanContract.Commands.Issue())
                `fails with`("The lender and the borrower cannot be the same entity.")
            }
        }
    }


    @Test
    fun `All parties must be signers`(){
        ledgerServices.ledger{
            transaction {
                output(LOANCONTRACT_CONTRACT_ID, LoanState(value, iR, miniCorp.party, megaCorp.party))
                command(megaCorp.publicKey, LoanContract.Commands.Issue())
                `fails with`("All of the participants must be signers.")
            }
        }
    }

    @Test
    fun `Interest Rate must not be negative`(){
        ledgerServices.ledger {
            transaction {
                output(LOANCONTRACT_CONTRACT_ID, LoanState(value, iR2, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey,miniCorp.publicKey),LoanContract.Commands.Issue())
                `fails with`("The interest must be non-negative.")
            }
        }
    }

    @Test
    fun `Currencies allowed are GBP, USD and EUR`(){
        ledgerServices.ledger {
            transaction {
                output(LOANCONTRACT_CONTRACT_ID, LoanState(value2, iR, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey,miniCorp.publicKey),LoanContract.Commands.Issue())
                `fails with`("The IOU's issuance must be GBP, USD or EUR.")
            }
        }
    }


    @Test
    fun `Payment value must be equal to the remain quantity`(){
        ledgerServices.ledger {
            transaction {
                input(LOANCONTRACT_CONTRACT_ID, LoanPaymentState(value3, iR, remain2, miniCorp.party, megaCorp.party))
                output(LOANCONTRACT_CONTRACT_ID, LoanState(value4, iR, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey,miniCorp.publicKey),LoanContract.Commands.Settle())
                `fails with`("The value must be equal to the remain loan.")
            }
        }
    }

    @Test
    fun `The value of the output loan must be 0`(){
        ledgerServices.ledger {
            transaction {
                input(LOANCONTRACT_CONTRACT_ID, LoanPaymentState(value3, iR, remain, miniCorp.party, megaCorp.party))
                output(LOANCONTRACT_CONTRACT_ID, LoanState(value, iR, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey,miniCorp.publicKey),LoanContract.Commands.Settle())
                `fails with`("The value of the output loan must be 0.")
            }
        }
    }


}