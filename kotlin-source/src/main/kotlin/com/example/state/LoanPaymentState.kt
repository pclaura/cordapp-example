package com.example.state

import net.corda.core.contracts.Amount
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.util.*

data class LoanPaymentState(val payment: Amount<Currency>,
                            val loanId: UniqueIdentifier,
                            val lender: Party,
                            val borrower: Party,
                            override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {

    override val participants: List<AbstractParty> get() = listOf(lender, borrower)

}

