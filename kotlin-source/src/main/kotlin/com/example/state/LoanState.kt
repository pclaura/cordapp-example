package com.example.state

import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.LinearState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.math.BigDecimal
import java.util.*


data class LoanState(val borrowedAmount: Amount<Currency>,
                     val interestRate: BigDecimal,
                     val lender: Party,
                     val borrower: Party,
                     override val linearId: UniqueIdentifier = UniqueIdentifier()): LinearState {

    override val participants: List<AbstractParty> get() = listOf(lender, borrower)

    val calculation = borrowedAmount.quantity + (borrowedAmount.quantity /100 * interestRate.toLong())
    val repaymentAmount: Amount<Currency> get() = Amount<Currency>(calculation, borrowedAmount.token)


}