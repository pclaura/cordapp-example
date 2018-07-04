package com.example.contract

import com.example.contract.LoanContract.Companion.LOANCONTRACT_CONTRACT_ID
import com.example.contract.LoanPaymentContract.Companion.LOANPAYMENTCONTRACT_CONTRACT_ID
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

class LoanPaymentContractTests {

    private val ledgerServices = MockServices()
    private val megaCorp = TestIdentity(CordaX500Name("MegaCorp", "London", "GB"))
    private val miniCorp = TestIdentity(CordaX500Name("MiniCorp", "New York", "US"))

    private val pay: Amount<Currency> get() = Amount(10, Currency.getInstance("GBP"))
    //private val pay2: Amount<Currency> get() = Amount(400, Currency.getInstance("GBP"))
    private val pay3: Amount<Currency> get() = Amount(20, Currency.getInstance("GBP"))

    private val remain: Amount<Currency> get() = Amount(50, Currency.getInstance("GBP"))
    private val remain2: Amount<Currency> get() = Amount(10, Currency.getInstance("GBP"))

    private val value: Amount<Currency> get() = Amount(100, Currency.getInstance("GBP"))
    //private val value2: Amount<Currency> get() = Amount(100, Currency.getInstance("USD"))
    private val value3: Amount<Currency> get() = Amount(40, Currency.getInstance("GBP"))
    private val value4: Amount<Currency> get() = Amount(40, Currency.getInstance("USD"))

    private val iR: BigDecimal = BigDecimal(50)

    @Test
    fun `transaction must include Issue command`() {

        ledgerServices.ledger {
            transaction { //TODO: AQUI HAY ALGUN TIPO DE CONFLICTO POR ESO PUSE EN EL OUTPUT EL ID DEL CONTRATO DEL LoanPaymentState
                input(LOANPAYMENTCONTRACT_CONTRACT_ID, LoanPaymentState(pay,iR,remain,megaCorp.party,miniCorp.party))
                output(LOANPAYMENTCONTRACT_CONTRACT_ID, LoanState(value3,iR,megaCorp.party,miniCorp.party))
                fails()
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), LoanPaymentContract.Commands.Issue())
                verifies()
            }
        }
    }

    @Test
    fun `Loan payments in same currency as loan`(){
        ledgerServices.ledger {
            transaction {
                input(LOANPAYMENTCONTRACT_CONTRACT_ID, LoanPaymentState(pay,iR,remain,megaCorp.party,miniCorp.party))
                output(LOANCONTRACT_CONTRACT_ID, LoanState(value4,iR,megaCorp.party,miniCorp.party))
                command(listOf(megaCorp.publicKey,miniCorp.publicKey),LoanPaymentContract.Commands.Issue())
                `fails with`("Loan payments must be made in the same currency as the loan.")
            }
        }
    }

    @Test
    fun `Loan payments less or equal to loan`(){
        ledgerServices.ledger {
            transaction {
                input(LOANPAYMENTCONTRACT_CONTRACT_ID, LoanPaymentState(pay3,iR,remain2,megaCorp.party,miniCorp.party))
                output(LOANCONTRACT_CONTRACT_ID, LoanState(value,iR,megaCorp.party,miniCorp.party))
                command(listOf(megaCorp.publicKey,miniCorp.publicKey),LoanPaymentContract.Commands.Issue())
                `fails with`("Loan payments cannot exceed the remain quantity.")
            }
        }
    }

}

