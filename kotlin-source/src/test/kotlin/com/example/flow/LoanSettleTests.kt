package com.example.flow

import com.example.state.LoanPaymentState
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

class LoanSettleTests {

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
        listOf(a, b).forEach { it.registerInitiatedFlow(LoanSettle.Acceptor::class.java) }
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `flow rejects invalid LOANs`() {
        val flow = LoanSettle.InitiatorFlow(value, ir, b.info.singleIdentity())
        val future = a.startFlow(flow)
        network.runNetwork()

        // The IOUContract specifies that IOUs cannot have negative values.
        assertFailsWith<TransactionVerificationException> { future.getOrThrow() }
    }


    @Test
    fun `recorded transaction has a single input and a single output, the input LoanPayment and the output Loan`() {
        val loanValue = value
        val flow = LoanSettle.InitiatorFlow(value, ir, b.info.singleIdentity())
        val future = a.startFlow(flow)
        network.runNetwork()
        val signedTx = future.getOrThrow()

        // We check the recorded transaction in both vaults.
        for (node in listOf(a, b)) { //'node' es el iterador del for
            val recordedTx = node.services.validatedTransactions.getTransaction(signedTx.id) //Tx firmada (puede ser nula)
            val txOutputs = recordedTx!!.tx.outputs // (!!) == Null check
            assert(txOutputs.size == 1) //Checks it has only one output

            val txInputs = recordedTx!!.tx.inputs
            assert(txInputs.size == 1) //Checks it has only one input


            val recordedState_out = txOutputs[0].data as LoanState
            assertEquals(recordedState_out.borrowedAmount, loanValue) //Checks the borrowed amount
            assertEquals(recordedState_out.interestRate, ir) //Checks the interest rate
            assertEquals(recordedState_out.lender, a.info.singleIdentity()) //Checks the lender identity
            assertEquals(recordedState_out.borrower, b.info.singleIdentity()) //Checks the borrower identity

            /*//TODO: verify the input (LoanPaymentState)
            val recordedState_in = txInputs[0] as LoanPaymentState
            assertEquals(recordedState_in.repaymentAmount, loanValue) //Checks the borrowed amount
            assertEquals(recordedState_out.interestRate, ir) //Checks the interest rate
            assertEquals(recordedState_out.lender, a.info.singleIdentity()) //Checks the lender identity
            assertEquals(recordedState_out.borrower, b.info.singleIdentity()) //Checks the borrower identity
*/
        }
    }
}