package com.example.flow

import com.example.state.LoanState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.util.*

class LoanSettleTest {
    lateinit var network: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode
    val payment1: Amount<Currency> = Amount(25, Currency.getInstance("EUR"))
    val payment2: Amount<Currency> = Amount(100, Currency.getInstance("EUR"))


    @Before
    fun setup() {
        network = MockNetwork(listOf("com.example.contract"))
        a = network.createPartyNode()
        b = network.createPartyNode()
        // For real nodes this happens automatically, but we have to manually register the flow for tests.
        listOf(a, b).forEach {
            it.registerInitiatedFlow(LoanIssuance.Acceptor::class.java)
            it.registerInitiatedFlow(LoanPaymentIssuance.Acceptor::class.java)
            it.registerInitiatedFlow(LoanSettle.Acceptor::class.java)
        }
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }


    @Test
    fun `Loan Payment completed`() {

        val flow = LoanIssuance.InitiatorFlow(Amount(100, Currency.getInstance("EUR")), BigDecimal(1.25), b.info.singleIdentity())
        val future = a.startFlow(flow)
        network.runNetwork()
        val signedTx = future.getOrThrow()

        val inLoan = signedTx.tx.outputsOfType<LoanState>().single()
        val loanID: UniqueIdentifier = inLoan.linearId


        val flow2 = LoanPaymentIssuance.InitiatorFlow(payment1, loanID, b.info.singleIdentity())
        val future2 = a.startFlow(flow2)
        network.runNetwork()
        val signedTx2 = future2.getOrThrow()
        val outLoan = signedTx2.tx.outputsOfType<LoanState>().single()

        val flow3 = LoanPaymentIssuance.InitiatorFlow(payment2, loanID, b.info.singleIdentity())
        val future3 = a.startFlow(flow3)
        network.runNetwork()
        val signedTx3 = future3.getOrThrow()
        val outLoan2 = signedTx3.tx.outputsOfType<LoanState>().single()

        val flow4 = LoanSettle.InitiatorFlow(loanID, b.info.singleIdentity())
        val future4 = a.startFlow(flow4)
        network.runNetwork()
        val signedTx4 = future4.getOrThrow()

        //PRINT's CHECK

        println("*** LOAN ***")
        println("LinearID: " + inLoan.linearId)
        println("*******")
        println("Borrowed Amount: "+ inLoan.borrowedAmount.quantity)
        println("Interest Rate: "+ inLoan.interestRate)
        println("REPAYMENT Amount: "+ inLoan.repaymentAmount.quantity)
        println("*******")
        println("Lender: " + inLoan.lender)
        println("Borrower: " + inLoan.borrower)
        println("*******")
        println("Payed amount: " + inLoan.payedOffAmount.quantity)
        println("*******")

        println("New Loan Payment of: " + payment1.quantity + payment1.token)
        println("Payed amount: " + outLoan.payedOffAmount.quantity)
        println("*******")

        println("New Loan Payment of: " + payment2.quantity + payment2.token)
        println("Payed amount: " + outLoan2.payedOffAmount.quantity)
        println("*******")

        println("SETTLE!")

    }

}