package com.example.state

import net.corda.core.contracts.Amount
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.math.BigDecimal
import java.util.*

data class LoanPaymentState(val payment: Amount<Currency>, //La cantidad que pago
                            val interestRate: BigDecimal,
                            val remain: Amount<Currency>, //Lo que falta por pagar y asi verifico si es el ultimo pago
                            val lender: Party,
                            val borrower: Party,
                            override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {

    override val participants: List<AbstractParty> get() = listOf(lender, borrower)

    val calculation = payment.quantity + (payment.quantity /100 * interestRate.toLong())
    val repaymentAmount: Amount<Currency> get() = Amount<Currency>(calculation, payment.token)
}

