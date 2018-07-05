package com.example.flow

import com.example.state.LoanState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LoanIssuanceTests {

    lateinit var network: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode
    val value: Amount<Currency> = Amount(100, Currency.getInstance("EUR"))
    val ir: BigDecimal = BigDecimal(10)

    @Before
    fun setup() {
        network = MockNetwork(listOf("com.example.contract"))
        a = network.createPartyNode()
        b = network.createPartyNode()
        // For real nodes this happens automatically, but we have to manually register the flow for tests.
        listOf(a, b).forEach { it.registerInitiatedFlow(LoanIssuance.Acceptor::class.java) }
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `flow rejects invalid LOANs`() { //Expected exception but not occurred as the LOAN is
        val flow = LoanIssuance.InitiatorFlow(value, ir, b.info.singleIdentity())
        val future = a.startFlow(flow)
        network.runNetwork()

        // The LOANContract specifies that LOANs cannot have negative values.
        assertFailsWith<TransactionVerificationException> { future.getOrThrow() }
    }

    @Test
    fun `SignedTransaction returned by the flow is signed by the initiator`() {
        val flow = LoanIssuance.InitiatorFlow(value, ir, b.info.singleIdentity())
        val future = a.startFlow(flow) //TODO: Node a is he one that initiates
        network.runNetwork()
        val signedTx = future.getOrThrow()

        signedTx.verifySignaturesExcept(b.info.singleIdentity().owningKey)
    }

    @Test
    fun `flow records a transaction in both parties' transaction storages`() {
        val flow = LoanIssuance.InitiatorFlow(value, ir, b.info.singleIdentity())
        val future = a.startFlow(flow)
        network.runNetwork()
        val signedTx = future.getOrThrow()

        // We check the recorded transaction in both transaction storages.
        for (node in listOf(a, b)) {
            assertEquals(signedTx, node.services.validatedTransactions.getTransaction(signedTx.id))
        }
    }


    @Test
    fun `recorded transaction has no inputs and a single output, the input IOU`() {
        val loanValue = value
        val flow = LoanIssuance.InitiatorFlow(value, ir, b.info.singleIdentity())
        val future = a.startFlow(flow)
        network.runNetwork()
        val signedTx = future.getOrThrow()

        // We check the recorded transaction in both vaults.
        for (node in listOf(a, b)) { //'node' es el iterador del for
            val recordedTx = node.services.validatedTransactions.getTransaction(signedTx.id) //Tx firmada (puede ser nula)
            val txOutputs = recordedTx!!.tx.outputs // (!!) == Null check
            assert(txOutputs.size == 1) //Checks it has only one output

            val recordedState = txOutputs[0].data as LoanState
            assertEquals(recordedState.borrowedAmount, loanValue) //Checks the borrowed amount
            assertEquals(recordedState.interestRate, ir) //Checks the interest rate
            assertEquals(recordedState.lender, a.info.singleIdentity()) //Checks the lender identity
            assertEquals(recordedState.borrower, b.info.singleIdentity()) //Checks the borrower identity
        }
    }
}