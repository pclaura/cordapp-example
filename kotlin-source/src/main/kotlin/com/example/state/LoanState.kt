package com.example.state

import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.LinearState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.math.BigDecimal
import java.util.*

/**
 *  LOAN STATE
 *
 *  borrowedAmount: what rests to be paid
 *  interestRate: interest rate
 *  payedOffAmount: total amount payed of the initial loan
 *  lender
 *  borrower
 *  linearId : State Identifier
 */
data class LoanState(val borrowedAmount: Amount<Currency>,
                     val interestRate: BigDecimal,
                     val payedOffAmount: Amount<Currency>,
                     val lender: Party,
                     val borrower: Party,
                     override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {

    override val participants: List<AbstractParty> get() = listOf(lender, borrower)

    val repaymentAmount: Amount<Currency> = Amount.fromDecimal(borrowedAmount.toDecimal() * interestRate, borrowedAmount.token)
}