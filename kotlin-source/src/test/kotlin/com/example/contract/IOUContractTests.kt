package com.example.contract

import com.example.contract.IOUContract.Companion.IOU_CONTRACT_ID
import com.example.state.IOUState
import net.corda.core.contracts.Amount
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.util.*

class IOUContractTests {
    private val ledgerServices = MockServices()
    private val megaCorp = TestIdentity(CordaX500Name("MegaCorp", "London", "GB"))
    private val miniCorp = TestIdentity(CordaX500Name("MiniCorp", "New York", "US"))
    private val value: Amount<Currency> get() = Amount(1, Currency.getInstance("GBP"))
    private val value2: Amount<Currency> get() = Amount(1, Currency.getInstance("EUR"))
    private val value3: Amount<Currency> get() = Amount(1, Currency.getInstance("RUB"))


    @Test
    fun `transaction must include Create command`() {
        val iou = value
        ledgerServices.ledger {
            transaction {
                output(IOU_CONTRACT_ID, IOUState(iou, miniCorp.party, megaCorp.party))
                fails()
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), IOUContract.Commands.Create())
                verifies()
            }
        }
    }

    @Test
    fun `transaction must have no inputs`() {
        val iou = value
        ledgerServices.ledger {
            transaction {
                input(IOU_CONTRACT_ID, IOUState(iou, miniCorp.party, megaCorp.party))
                output(IOU_CONTRACT_ID, IOUState(iou, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), IOUContract.Commands.Create())
                `fails with`("No inputs should be consumed when issuing an IOU.")
            }
        }
    }

    @Test
    fun `transaction must have one output`() {
        val iou = value
        ledgerServices.ledger {
            transaction {
                output(IOU_CONTRACT_ID, IOUState(iou, miniCorp.party, megaCorp.party))
                output(IOU_CONTRACT_ID, IOUState(iou, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), IOUContract.Commands.Create())
                `fails with`("Only one output state should be created.")
            }
        }
    }

    @Test
    fun `lender must sign transaction`() {
        val iou = value
        ledgerServices.ledger {
            transaction {
                output(IOU_CONTRACT_ID, IOUState(iou, miniCorp.party, megaCorp.party))
                command(miniCorp.publicKey, IOUContract.Commands.Create())
                `fails with`("All of the participants must be signers.")
            }
        }
    }

    @Test
    fun `borrower must sign transaction`() {
        val iou = value
        ledgerServices.ledger {
            transaction {
                output(IOU_CONTRACT_ID, IOUState(iou, miniCorp.party, megaCorp.party))
                command(megaCorp.publicKey, IOUContract.Commands.Create())
                `fails with`("All of the participants must be signers.")
            }
        }
    }

    @Test
    fun `lender is not borrower`() {
        val iou = value
        ledgerServices.ledger {
            transaction {
                output(IOU_CONTRACT_ID, IOUState(iou, megaCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), IOUContract.Commands.Create())
                `fails with`("The lender and the borrower cannot be the same entity.")
            }
        }
    }


    @Test
    fun `Currency must be GBP, USD or EUR`() {
        val iou = value3
        ledgerServices.ledger {
            transaction {
                output(IOU_CONTRACT_ID, IOUState(iou, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), IOUContract.Commands.Create())
                `fails with`("The IOU's issuance must be GBP, USD or EUR.")
            }
        }
    }
}